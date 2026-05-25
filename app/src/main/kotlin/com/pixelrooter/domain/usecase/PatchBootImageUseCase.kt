package com.pixelrooter.domain.usecase

import android.content.Context
import com.pixelrooter.data.model.RootingResult
import com.pixelrooter.util.PartitionUtils
import com.pixelrooter.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PatchBootImageUseCase(private val context: Context) {

    suspend fun execute(onProgress: (String) -> Unit): RootingResult =
        withContext(Dispatchers.IO) {
            val workDir = File(context.cacheDir, "rooting_${System.currentTimeMillis()}")
            workDir.mkdirs()

            try {
                patch(workDir, onProgress)
            } finally {
                workDir.deleteRecursively()
            }
        }

    private suspend fun patch(workDir: File, onProgress: (String) -> Unit): RootingResult {
        val magiskboot = File(context.filesDir, "magiskboot")
        val magiskDir = File(context.filesDir, "magisk")
        val bootPartition = PartitionUtils.getBootPartitionPath()
        val bootImg = File(workDir, "boot.img")
        val patchedImg = File(workDir, "patched_boot.img")

        onProgress("Reading boot partition: $bootPartition")
        val readResult = ShellUtils.execAsRoot(
            "dd", "if=$bootPartition", "of=${bootImg.absolutePath}", "bs=4096"
        )
        if (readResult.exitCode != 0) {
            return RootingResult.Failure(
                RootingResult.Stage.PARTITION_READ,
                "dd read failed (${readResult.exitCode}): ${readResult.stderr}"
            )
        }

        onProgress("Unpacking boot image...")
        val unpackResult = ShellUtils.exec(
            arrayOf(magiskboot.absolutePath, "unpack", bootImg.absolutePath),
            workDir = workDir
        )
        if (unpackResult.exitCode != 0) {
            return RootingResult.Failure(
                RootingResult.Stage.PATCHING,
                "magiskboot unpack failed: ${unpackResult.stderr}"
            )
        }

        onProgress("Copying Magisk blobs into working directory...")
        for (blob in listOf("magisk64.xz", "magisk32.xz", "magiskinit", "stub.apk")) {
            val src = File(magiskDir, blob)
            if (src.exists()) src.copyTo(File(workDir, blob), overwrite = true)
        }

        onProgress("Patching ramdisk with Magisk...")
        val patchEnv = mapOf(
            "KEEPVERITY" to "false",
            "KEEPFORCEENCRYPT" to "false",
            "RECOVERYMODE" to "false",
            "LEGACYSAR" to "false"
        )
        val cpioResult = ShellUtils.exec(
            arrayOf(magiskboot.absolutePath, "cpio", "ramdisk.cpio", "patch"),
            workDir = workDir,
            env = patchEnv
        )
        if (cpioResult.exitCode != 0) {
            return RootingResult.Failure(
                RootingResult.Stage.PATCHING,
                "ramdisk patch failed: ${cpioResult.stderr}"
            )
        }

        onProgress("Repacking boot image...")
        val repackResult = ShellUtils.exec(
            arrayOf(
                magiskboot.absolutePath, "repack",
                bootImg.absolutePath, patchedImg.absolutePath
            ),
            workDir = workDir
        )
        if (repackResult.exitCode != 0) {
            return RootingResult.Failure(
                RootingResult.Stage.PATCHING,
                "magiskboot repack failed: ${repackResult.stderr}"
            )
        }

        onProgress("Writing patched image to boot partition...")
        val writeResult = ShellUtils.execAsRoot(
            "dd", "if=${patchedImg.absolutePath}", "of=$bootPartition", "bs=4096"
        )
        if (writeResult.exitCode != 0) {
            return RootingResult.Failure(
                RootingResult.Stage.PARTITION_WRITE,
                "dd write failed (${writeResult.exitCode}): ${writeResult.stderr}"
            )
        }

        onProgress("Syncing filesystem...")
        ShellUtils.execAsRoot("sync")

        return RootingResult.Success
    }
}

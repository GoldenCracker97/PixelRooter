package com.pixelrooter.domain.usecase

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExtractAssetsUseCase(private val context: Context) {

    suspend fun execute(): Boolean = withContext(Dispatchers.IO) {
        try {
            extractMagiskboot()
            extractMagiskBlobs()
            extractExploitPayloads()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractMagiskboot() {
        val abi = Build.SUPPORTED_ABIS.firstOrNull {
            it in listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        } ?: return

        val assetName = "magiskboot/magiskboot_$abi"
        val destFile = File(context.filesDir, "magiskboot")

        copyAssetIfChanged(assetName, destFile)
        destFile.setExecutable(true, false)
    }

    private fun extractMagiskBlobs() {
        val blobDir = File(context.filesDir, "magisk").also { it.mkdirs() }
        val blobs = listOf("magisk64.xz", "magisk32.xz", "magiskinit", "stub.apk")
        for (blob in blobs) {
            val assetPath = "magisk/$blob"
            val destFile = File(blobDir, blob)
            copyAssetIfChanged(assetPath, destFile)
        }
        File(blobDir, "magiskinit").setExecutable(true, false)
    }

    private fun extractExploitPayloads() {
        val exploitDir = File(context.filesDir, "exploits").also { it.mkdirs() }
        val assets = try {
            context.assets.list("exploits") ?: emptyArray()
        } catch (_: Exception) { emptyArray() }

        for (assetName in assets) {
            val destFile = File(exploitDir, assetName)
            copyAssetIfChanged("exploits/$assetName", destFile)
            destFile.setExecutable(true, false)
        }
    }

    private fun copyAssetIfChanged(assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                val assetSize = input.available().toLong()
                if (destFile.exists() && destFile.length() == assetSize) return
                destFile.parentFile?.mkdirs()
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (_: Exception) {
            // Asset not present yet (populated by prepare_assets.py before build)
        }
    }
}

package com.pixelrooter.domain.usecase

import android.content.Context
import com.pixelrooter.data.model.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CheckRootStatusUseCase(private val context: Context) {

    suspend fun execute(): RootStatus = withContext(Dispatchers.IO) {
        if (hasMagiskApp()) {
            val version = getMagiskVersion()
            return@withContext RootStatus.RootedWithMagisk(version)
        }
        if (hasSuBinary()) {
            if (canExecuteSu()) return@withContext RootStatus.RootedOther
        }
        RootStatus.NotRooted
    }

    private fun hasMagiskApp(): Boolean {
        val magiskPackages = listOf(
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk"
        )
        return magiskPackages.any { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) { false }
        }
    }

    private fun getMagiskVersion(): String {
        return try {
            val result = Runtime.getRuntime().exec(arrayOf("magisk", "-v"))
            result.inputStream.bufferedReader().readLine()?.trim() ?: "unknown"
        } catch (_: Exception) { "unknown" }
    }

    private fun hasSuBinary(): Boolean {
        val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su")
        return suPaths.any { File(it).exists() }
    }

    private fun canExecuteSu(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readLine() ?: ""
            process.waitFor()
            output.contains("uid=0")
        } catch (_: Exception) { false }
    }
}

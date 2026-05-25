package com.pixelrooter.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

object ShellUtils {

    suspend fun exec(
        command: Array<String>,
        workDir: File? = null,
        env: Map<String, String> = emptyMap(),
        timeoutMs: Long = 120_000L
    ): ShellResult = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder(*command).apply {
            workDir?.let { directory(it) }
            if (env.isNotEmpty()) environment().putAll(env)
            redirectErrorStream(false)
        }

        val result = withTimeoutOrNull(timeoutMs) {
            val process = builder.start()

            val stdoutDeferred = async {
                process.inputStream.bufferedReader().readText()
            }
            val stderrDeferred = async {
                process.errorStream.bufferedReader().readText()
            }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            val exitCode = process.waitFor()

            ShellResult(exitCode, stdout, stderr)
        }

        result ?: ShellResult(-1, "", "Command timed out after ${timeoutMs}ms")
    }

    suspend fun execAsRoot(
        vararg command: String,
        workDir: File? = null,
        env: Map<String, String> = emptyMap()
    ): ShellResult = exec(arrayOf(*command), workDir, env)
}

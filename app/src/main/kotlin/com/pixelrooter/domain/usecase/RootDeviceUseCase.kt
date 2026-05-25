package com.pixelrooter.domain.usecase

import android.content.Context
import com.pixelrooter.data.model.RootingResult
import com.pixelrooter.domain.exploit.ExploitEngine
import com.pixelrooter.domain.exploit.ExploitEngineResult
import com.pixelrooter.util.DeviceUtils
import com.pixelrooter.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootDeviceUseCase(private val context: Context) {

    private val exploitEngine = ExploitEngine(context)
    private val patchBootImage = PatchBootImageUseCase(context)

    suspend fun execute(onProgress: (String) -> Unit): RootingResult =
        withContext(Dispatchers.IO) {
            val device = DeviceUtils.getDeviceInfo()

            onProgress("Device: ${device.model} (${device.codename})")
            onProgress("Android ${device.androidVersion} | Kernel ${device.kernelVersion}")
            onProgress("Security patch: ${device.securityPatchLevel}")
            onProgress("")

            onProgress("Running privilege escalation...")
            val exploitResult = exploitEngine.run(device, onProgress)

            when (exploitResult) {
                is ExploitEngineResult.NoCandidates -> {
                    return@withContext RootingResult.Failure(
                        RootingResult.Stage.EXPLOIT,
                        "No compatible exploit for kernel ${device.kernelVersion} " +
                                "with security patch ${device.securityPatchLevel}"
                    )
                }
                is ExploitEngineResult.AllFailed -> {
                    val summary = exploitResult.attempts.joinToString("; ") { (cve, reason) ->
                        "$cve: $reason"
                    }
                    return@withContext RootingResult.Failure(
                        RootingResult.Stage.EXPLOIT,
                        "All exploits failed: $summary"
                    )
                }
                is ExploitEngineResult.Success -> {
                    onProgress("Root obtained via ${exploitResult.cve}")
                }
            }

            onProgress("")
            val patchResult = patchBootImage.execute(onProgress)

            if (patchResult is RootingResult.Failure) {
                return@withContext patchResult
            }

            onProgress("")
            onProgress("Rebooting in 3 seconds...")
            ShellUtils.execAsRoot("reboot")

            RootingResult.Success
        }
}

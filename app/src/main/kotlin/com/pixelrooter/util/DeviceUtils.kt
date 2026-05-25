package com.pixelrooter.util

import android.os.Build
import com.pixelrooter.data.model.DeviceInfo

private val PIXEL_CODENAMES = setOf(
    "sailfish", "walleye", "blueline", "crosshatch",
    "flame", "coral", "sunfish",
    "redfin", "bramble",
    "oriole", "raven",
    "panther", "cheetah",
    "lynx", "shiba", "husky",
    "akita", "caiman"
)

object DeviceUtils {

    fun getDeviceInfo(): DeviceInfo {
        val codename = Build.DEVICE ?: ""
        return DeviceInfo(
            model = Build.MODEL ?: "Unknown",
            codename = codename,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            kernelVersion = getKernelVersion(),
            securityPatchLevel = Build.VERSION.SECURITY_PATCH ?: "Unknown",
            buildId = Build.ID ?: "Unknown",
            isPixel = Build.MANUFACTURER?.equals("Google", ignoreCase = true) == true
                    && codename in PIXEL_CODENAMES,
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        )
    }

    private fun getKernelVersion(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("uname", "-r"))
            process.inputStream.bufferedReader().readLine()?.trim() ?: "Unknown"
        } catch (_: Exception) {
            System.getProperty("os.version") ?: "Unknown"
        }
    }
}

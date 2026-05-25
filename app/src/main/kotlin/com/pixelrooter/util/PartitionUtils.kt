package com.pixelrooter.util

import java.io.File

object PartitionUtils {

    fun getCurrentSlot(): String {
        return try {
            val cmdline = File("/proc/cmdline").readText()
            val match = Regex("""androidboot\.slot_suffix=_([ab])""").find(cmdline)
            match?.groupValues?.get(1) ?: "a"
        } catch (_: Exception) {
            "a"
        }
    }

    fun getBootPartitionPath(): String {
        val slot = getCurrentSlot()
        val candidates = listOf(
            "/dev/block/bootdevice/by-name/boot_$slot",
            "/dev/block/bootdevice/by-name/boot",
            "/dev/block/by-name/boot_$slot",
            "/dev/block/by-name/boot"
        )
        return candidates.firstOrNull { File(it).exists() }
            ?: "/dev/block/bootdevice/by-name/boot_$slot"
    }

    fun getBootPartitionSize(): Long {
        return try {
            val path = getBootPartitionPath()
            File(path).length().takeIf { it > 0 } ?: DEFAULT_BOOT_SIZE
        } catch (_: Exception) {
            DEFAULT_BOOT_SIZE
        }
    }

    private const val DEFAULT_BOOT_SIZE = 67_108_864L // 64 MiB fallback
}

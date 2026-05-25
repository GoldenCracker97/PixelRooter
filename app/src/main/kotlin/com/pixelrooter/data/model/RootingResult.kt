package com.pixelrooter.data.model

sealed class RootingResult {
    object Success : RootingResult()
    data class Failure(val stage: Stage, val reason: String) : RootingResult()
    object Cancelled : RootingResult()

    enum class Stage {
        EXPLOIT,
        PARTITION_READ,
        PATCHING,
        PARTITION_WRITE,
        REBOOT
    }
}

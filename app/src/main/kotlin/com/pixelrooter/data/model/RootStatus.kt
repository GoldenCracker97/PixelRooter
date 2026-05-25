package com.pixelrooter.data.model

sealed class RootStatus {
    object NotRooted : RootStatus()
    data class RootedWithMagisk(val magiskVersion: String) : RootStatus()
    object RootedOther : RootStatus()
    object Unknown : RootStatus()
}

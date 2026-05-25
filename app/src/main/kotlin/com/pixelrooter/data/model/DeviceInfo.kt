package com.pixelrooter.data.model

data class DeviceInfo(
    val model: String,
    val codename: String,
    val androidVersion: String,
    val kernelVersion: String,
    val securityPatchLevel: String,
    val buildId: String,
    val isPixel: Boolean,
    val supportedAbis: List<String>
)

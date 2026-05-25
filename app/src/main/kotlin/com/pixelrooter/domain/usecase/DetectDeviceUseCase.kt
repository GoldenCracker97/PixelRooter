package com.pixelrooter.domain.usecase

import com.pixelrooter.data.model.DeviceInfo
import com.pixelrooter.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetectDeviceUseCase {
    suspend fun execute(): DeviceInfo = withContext(Dispatchers.IO) {
        DeviceUtils.getDeviceInfo()
    }
}

package com.pixelrooter

import android.app.Application
import com.pixelrooter.domain.usecase.ExtractAssetsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PixelRooterApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            ExtractAssetsUseCase(applicationContext).execute()
        }
    }
}

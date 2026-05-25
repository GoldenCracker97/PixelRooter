package com.pixelrooter.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelrooter.data.model.DeviceInfo
import com.pixelrooter.data.model.RootStatus
import com.pixelrooter.domain.exploit.ExploitEngine
import com.pixelrooter.domain.usecase.CheckRootStatusUseCase
import com.pixelrooter.domain.usecase.DetectDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val deviceInfo: DeviceInfo? = null,
    val rootStatus: RootStatus = RootStatus.Unknown,
    val hasCompatibleExploit: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val detectDevice = DetectDeviceUseCase()
    private val checkRoot = CheckRootStatusUseCase(app)
    private val exploitEngine = ExploitEngine(app)

    init {
        loadDeviceInfo()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            val deviceInfo = detectDevice.execute()
            val rootStatus = checkRoot.execute()
            val hasExploit = exploitEngine.hasCompatibleExploit(deviceInfo)

            _uiState.value = HomeUiState(
                deviceInfo = deviceInfo,
                rootStatus = rootStatus,
                hasCompatibleExploit = hasExploit,
                isLoading = false
            )
        }
    }
}

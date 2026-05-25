package com.pixelrooter.ui.root

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelrooter.data.model.RootingResult
import com.pixelrooter.domain.usecase.RootDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RootUiState {
    object Idle : RootUiState()
    data class Running(val log: String) : RootUiState()
    object Success : RootUiState()
    data class Error(val log: String, val reason: String) : RootUiState()
}

class RootViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Idle)
    val uiState: StateFlow<RootUiState> = _uiState

    private val rootDevice = RootDeviceUseCase(app)
    private val logBuilder = StringBuilder()

    fun startRooting() {
        if (_uiState.value !is RootUiState.Idle) return

        logBuilder.clear()
        _uiState.value = RootUiState.Running("")

        viewModelScope.launch {
            val result = rootDevice.execute { line ->
                logBuilder.appendLine(line)
                _uiState.value = RootUiState.Running(logBuilder.toString())
            }

            _uiState.value = when (result) {
                RootingResult.Success -> RootUiState.Success
                is RootingResult.Failure -> RootUiState.Error(
                    logBuilder.toString(),
                    "[${result.stage.name}] ${result.reason}"
                )
                RootingResult.Cancelled -> RootUiState.Idle
            }
        }
    }
}

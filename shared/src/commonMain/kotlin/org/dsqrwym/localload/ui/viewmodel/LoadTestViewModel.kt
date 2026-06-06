package org.dsqrwym.localload.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dsqrwym.localload.engine.LoadTestController
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestResult

data class LoadTestUiState(
    val url: String = "",
    val isRunning: Boolean = false,
    val lastLatencyMs: Long? = null,
    val lastSuccess: Boolean? = null,
    val message: String? = null
)

class LoadTestViewModel(
    private val controller: LoadTestController
) : ViewModel() {

    private val _state = MutableStateFlow(LoadTestUiState())
    val state: StateFlow<LoadTestUiState> = _state.asStateFlow()

    private var runningJob: kotlinx.coroutines.Job? = null

    fun onUrlChange(url: String) {
        _state.value = _state.value.copy(url = url)
    }

    fun startTest() {
        val url = _state.value.url.trim()
        if (url.isBlank()) {
            _state.value = _state.value.copy(message = "URL不能为空")
            return
        }

        val config = LoadTestConfig(
            url = url,
            durationMs = 30_000, // MVP 固定 30s
            concurrency = 50
        )

        _state.value = _state.value.copy(
            isRunning = true,
            message = "running..."
        )

        val flow = controller.start(config)

        runningJob?.cancel()
        runningJob = viewModelScope.launch {
            flow.collect { result ->
                handleResult(result)
            }
        }
    }

    fun stopTest() {
        controller.stop()
        runningJob?.cancel()
        runningJob = null

        _state.value = _state.value.copy(
            isRunning = false,
            message = "stopped"
        )
    }

    private fun handleResult(result: RequestResult) {
        _state.value = _state.value.copy(
            lastLatencyMs = result.latencyMs,
            lastSuccess = result.success
        )
    }
}
package org.dsqrwym.localload.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dsqrwym.localload.engine.ControllerState
import org.dsqrwym.localload.engine.LoadTestController
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.metrics.TestReport
import kotlin.time.Duration.Companion.milliseconds

data class LoadTestUiState(
    val url: String = "",
    val isRunning: Boolean = false,

    val totalRequests: Long = 0,
    val currentRps: Double = 0.0,
    val avgLatencyMs: Double = 0.0,
    val errorRate: Double = 0.0,

    val lastLatencyMs: Long? = null,
    val lastSuccess: Boolean? = null,

    val message: String? = null
)

class LoadTestViewModel(
    private val controller: LoadTestController
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(LoadTestUiState())
    val uiState: StateFlow<LoadTestUiState> =
        _uiState.asStateFlow()
    private val _report =
        MutableStateFlow<TestReport?>(null)
    val report: StateFlow<TestReport?> =
        _report.asStateFlow()
    private var resultJob: Job? = null
    private var metricsJob: Job? = null

    init {
        observeControllerState()
    }

    private fun observeControllerState() {
        viewModelScope.launch {
            controller.state.collect { state ->
                _uiState.value =
                    _uiState.value.copy(
                        isRunning =
                            state == ControllerState.Starting ||
                                    state == ControllerState.Running
                    )
                when (state) {
                    ControllerState.Running -> {
                        startMetricsPolling()
                    }

                    ControllerState.Idle -> {
                        _report.value = controller.lastReport
                        metricsJob?.cancel()
                        metricsJob = null
                        resultJob?.cancel()
                        resultJob = null
                        _uiState.value =
                            _uiState.value.copy(
                                message = "completed"
                            )
                    }

                    else -> Unit
                }
            }
        }
    }

    fun onUrlChange(url: String) {
        _uiState.value =
            _uiState.value.copy(url = url)
    }

    fun startTest() {
        val url =
            _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    message = "URL不能为空"
                )
            return
        }

        clearReport()
        val config =
            LoadTestConfig(
                url = url,
                durationMs = 30_000,
                concurrency = 500
            )
        _uiState.value =
            _uiState.value.copy(
                message = "running..."
            )
        val flow = controller.start(config)

        resultJob?.cancel()

        resultJob =
            viewModelScope.launch {
                flow.collect { result ->
                    handleResult(result)
                }
            }
    }

    fun stopTest() {
        controller.stop()
    }

    fun clearReport() {
        _report.value = null
    }

    private fun handleResult(
        result: RequestResult
    ) {
        _uiState.value =
            _uiState.value.copy(
                lastLatencyMs = result.latencyMs,
                lastSuccess = result.success
            )
    }

    private fun startMetricsPolling() {
        if (metricsJob?.isActive == true) {
            return
        }
        metricsJob = viewModelScope.launch {
            while (controller.isRunning()) {
                controller.metricsCollector()?.snapshot()?.let { snapshot ->
                    _uiState.value =
                        _uiState.value.copy(
                            totalRequests = snapshot.totalRequests,
                            currentRps = snapshot.currentRps,
                            avgLatencyMs = snapshot.avgLatencyMs,
                            errorRate = snapshot.errorRate
                        )
                }
                delay(500.milliseconds)
            }
        }
    }

    override fun onCleared() {
        resultJob?.cancel()
        metricsJob?.cancel()
        controller.stop()
        super.onCleared()
    }
}
package org.dsqrwym.localload.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dsqrwym.localload.engine.LoadTestController
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.metrics.TestReport
import kotlin.time.Duration.Companion.seconds

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

    private val _state = MutableStateFlow(LoadTestUiState())
    val state: StateFlow<LoadTestUiState> = _state.asStateFlow()

    private val _report = MutableStateFlow<TestReport?>(null)
    val report: StateFlow<TestReport?> = _report.asStateFlow()

    private var runningJob: Job? = null
    private var metricsJob: Job? = null

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
            durationMs = 30_000,
            concurrency = 500,
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
            // Flow 结束后自动显示报告
            _report.value = controller.metricsCollector()?.buildReport()
            controller.stop()
            runningJob = null
            metricsJob?.cancel()
            metricsJob = null
            _state.value = _state.value.copy(
                isRunning = false,
                message = "completed"
            )
        }

        startMetricsPolling()
    }

    fun stopTest() {
        viewModelScope.launch {
            _report.value =
                controller.metricsCollector()
                    ?.buildReport()

            controller.stop()
            runningJob?.cancel()
            runningJob = null

            metricsJob?.cancel()
            metricsJob = null

            _state.value = _state.value.copy(
                isRunning = false,
                message = "stopped"
            )
        }
    }

    fun clearReport() {
        _report.value = null
    }

    private fun handleResult(result: RequestResult) {
        _state.value = _state.value.copy(
            lastLatencyMs = result.latencyMs,
            lastSuccess = result.success
        )
    }

    private fun startMetricsPolling() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            while (controller.isRunning()) {
                delay(1.seconds)
                val snapshot =
                    controller.metricsCollector()
                        ?.snapshot()
                        ?: continue

                _state.value = _state.value.copy(
                    totalRequests = snapshot.totalRequests,
                    currentRps = snapshot.currentRps,
                    avgLatencyMs = snapshot.avgLatencyMs,
                    errorRate = snapshot.errorRate
                )
            }
        }
    }
}
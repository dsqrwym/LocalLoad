package org.dsqrwym.localload.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.metrics.MetricsCollector
import org.dsqrwym.localload.engine.metrics.TestReport
import org.dsqrwym.localload.engine.scheduler.Scheduler

// Controller State Model
enum class ControllerState {
    Idle,
    Starting,
    Running,
    Stopping
}

class LoadTestController(
    private val scheduler: Scheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: LoadTestEngine? = null
    private var currentConfig: LoadTestConfig? = null
    var lastReport: TestReport? = null
        private set

    private val _state = MutableStateFlow(ControllerState.Idle)
    val state: StateFlow<ControllerState> = _state.asStateFlow()

    // START TEST
    fun start(config: LoadTestConfig): Flow<RequestResult> {
        if (engine != null) {
            stop()
        }

        _state.value = ControllerState.Starting

        val newEngine = LoadTestEngine(
            config = config,
            scheduler = scheduler,
        )

        engine = newEngine
        currentConfig = config
        lastReport = null

        newEngine.start()

        scope.launch {
            newEngine.isStopped.first { it }
            lastReport = newEngine.metricsCollector().buildReport()
            engine = null
            currentConfig = null
            _state.value = ControllerState.Idle
        }

        _state.value = ControllerState.Running
        return newEngine.results()
    }

    // STOP TEST
    fun stop() {
        val e = engine ?: return
        _state.value = ControllerState.Stopping
        e.stop()
    }

    // STATUS
    fun isRunning(): Boolean = engine != null

    fun getCurrentConfig(): LoadTestConfig? = currentConfig

    fun metricsCollector(): MetricsCollector? {
        return engine?.metricsCollector()
    }
}

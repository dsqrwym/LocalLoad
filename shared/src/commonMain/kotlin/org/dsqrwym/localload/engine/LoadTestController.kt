package org.dsqrwym.localload.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.metrics.MetricsCollector
import org.dsqrwym.localload.engine.scheduler.Scheduler

// Controller State Model
sealed class ControllerState {
    object Idle : ControllerState()
    object Starting : ControllerState()
    object Running : ControllerState()
    object Stopping : ControllerState()
}

class LoadTestController(
    private val scheduler: Scheduler,
    private val executor: KtorExecutor
) {

    private var engine: LoadTestEngine? = null
    private var currentConfig: LoadTestConfig? = null

    private val _state = MutableStateFlow<ControllerState>(ControllerState.Idle)
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
            executor = executor
        )

        engine = newEngine
        currentConfig = config

        newEngine.start()

        _state.value = ControllerState.Running

        return newEngine.results()
    }

    // STOP TEST
    fun stop() {
        val e = engine ?: return

        _state.value = ControllerState.Stopping

        e.stop()

        engine = null
        currentConfig = null

        _state.value = ControllerState.Idle
    }

    // STATUS
    fun isRunning(): Boolean = engine != null

    fun getCurrentConfig(): LoadTestConfig? = currentConfig

    fun metricsCollector(): MetricsCollector? {
        return engine?.metricsCollector()
    }
}

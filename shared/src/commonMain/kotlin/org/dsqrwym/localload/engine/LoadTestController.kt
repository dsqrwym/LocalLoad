package org.dsqrwym.localload.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

        // 监听 engine 的彻底关闭 - 绑定到类成员 scope
        scope.launch {
            e.isStopped.first { it } // 等待第一个为 true 的值
            _state.value = ControllerState.Idle
            engine = null
            currentConfig = null
        }
    }

    // STATUS
    fun isRunning(): Boolean = engine != null

    fun getCurrentConfig(): LoadTestConfig? = currentConfig

    fun metricsCollector(): MetricsCollector? {
        return engine?.metricsCollector()
    }
}

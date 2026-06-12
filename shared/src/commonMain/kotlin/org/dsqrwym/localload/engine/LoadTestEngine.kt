package org.dsqrwym.localload.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.config.WorkerPoolMode
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.execution.RequestTask
import org.dsqrwym.localload.engine.http.HttpProvider
import org.dsqrwym.localload.engine.metrics.MetricsCollector
import org.dsqrwym.localload.engine.runtime.QueueWorkerPool
import org.dsqrwym.localload.engine.runtime.QueueWorkerPoolImpl
import org.dsqrwym.localload.engine.runtime.ScenarioWorkerPool
import org.dsqrwym.localload.engine.runtime.WorkerPool
import org.dsqrwym.localload.engine.scheduler.Scheduler
import org.dsqrwym.localload.util.AppDispatchers
import org.dsqrwym.localload.util.TaskIdGenerator
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LoadTestEngine(
    private val config: LoadTestConfig,
    private val scheduler: Scheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
    private val metricsCollector = MetricsCollector()
    private val executor = KtorExecutor(
        client = HttpProvider(config.http).client,
        recorder = metricsCollector
    )
    private var metricsTickerJob: Job? = null

    private val _isStopped = MutableStateFlow(false)
    val isStopped: StateFlow<Boolean> = _isStopped.asStateFlow()

    // 根据配置模式工厂化创建
    private val workerPool: WorkerPool = when (config.workerPoolMode) {
        WorkerPoolMode.QUEUE -> {
            QueueWorkerPoolImpl(executor, config.concurrency, scope)
        }

        WorkerPoolMode.CLOSED_LOOP -> {
            ScenarioWorkerPool(executor, config.concurrency, scope) {
                RequestTask(
                    id = TaskIdGenerator.nextId(),
                    url = config.url,
                    method = config.method
                )
            }
        }
    }

    /**
     * Engine统一结果总线
     */
    private val resultFlow = MutableSharedFlow<RequestResult>(replay = 0, extraBufferCapacity = 1024)

    @Volatile
    private var running = false

    private var schedulerJob: Job? = null
    private var resultCollectorJob: Job? = null
    private var autoStopJob: Job? = null

    fun start() {
        if (running) return
        running = true

        workerPool.start()
        if (config.workerPoolMode == WorkerPoolMode.QUEUE) {
            startScheduler()
        }

        startMetricsTicker()
        startAutoStop()
    }

    fun stop() {
        if (!running) return

        running = false

        scheduler.stop()

        schedulerJob?.cancel()
        resultCollectorJob?.cancel()
        metricsTickerJob?.cancel()
        autoStopJob?.cancel()

        scope.launch {
            metricsCollector.flush()
            workerPool.shutdown()
            _isStopped.value = true // 通知外部：清理工作已完成
            scope.cancel()
        }
    }

    fun results(): Flow<RequestResult> {
        return resultFlow.asSharedFlow()
    }

    fun metricsCollector(): MetricsCollector = metricsCollector

    /**
     * Scheduler -> WorkerPool
     */
    private fun startScheduler() {
        schedulerJob = scope.launch {
            // 只有 QUEUE 模式才会走到这里
            val queuePool = workerPool as? QueueWorkerPool
                ?: throw IllegalStateException("Mode mismatch")
            scheduler
                .start(config)
                .collect { task ->
                    if (!running) return@collect
                    queuePool.submit(task)
                }
        }
    }


    private fun startAutoStop() {
        autoStopJob = scope.launch {
            delay(config.durationMs.milliseconds)
            stop()
        }
    }

    private fun startMetricsTicker() {
        metricsTickerJob =
            scope.launch {
                while (running) {
                    delay(1.seconds)
                    metricsCollector.tick()
                }
            }
    }
}
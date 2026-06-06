package org.dsqrwym.localload.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.execution.RequestTask
import org.dsqrwym.localload.engine.metrics.MetricsCollector
import org.dsqrwym.localload.engine.scheduler.Scheduler
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

class LoadTestEngine(
    private val config: LoadTestConfig,
    private val scheduler: Scheduler,
    private val executor: KtorExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val taskChannel = Channel<RequestTask>(Channel.BUFFERED)
    private val resultChannel = Channel<RequestResult>(Channel.BUFFERED)
    // 收集器
    private val metricsCollector = MetricsCollector()

    @Volatile
    private var running = false
    private var schedulerJob: Job? = null
    private val workerJobs = mutableListOf<Job>()

    fun start() {
        if (running) return
        running = true

        startScheduler()
        startWorkers()
        startAutoStop()
    }

    fun stop() {
        running = false
        schedulerJob?.cancel()
        workerJobs.forEach { it.cancel() }
        resultChannel.close()
        scope.cancel()
    }

    fun results(): Flow<RequestResult> = resultChannel.receiveAsFlow()
    fun metricsCollector(): MetricsCollector {
        return metricsCollector
    }

    // Scheduler → Channel
    private fun startScheduler() {
        schedulerJob = scope.launch {
            scheduler.start(config).collect { task ->
                if (!running) return@collect

                taskChannel.trySend(task)
            }

            taskChannel.close()
        }
    }

    // Workers
    private fun startWorkers() {
        repeat(config.concurrency) {
            val job = scope.launch {
                for (task in taskChannel) {
                    if (!running) break

                    val result = try {
                        executor.execute(task)
                    } catch (e: Exception) {
                        RequestResult(
                            requestId = task.id,
                            success = false,
                            httpStatus = null,
                            latencyMs = 0,
                            errorMessage = e.message
                        )
                    }

                    metricsCollector.record(result)
                    resultChannel.trySend(result)
                }
            }

            workerJobs.add(job)
        }
    }

    // Auto stop
    private fun startAutoStop() {
        scope.launch {
            delay(config.durationMs.milliseconds)
            stop()
        }
    }
}
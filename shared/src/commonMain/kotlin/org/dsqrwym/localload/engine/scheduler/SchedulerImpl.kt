package org.dsqrwym.localload.engine.scheduler

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestTask
import org.dsqrwym.localload.engine.rate.RateFunctionFactory
import org.dsqrwym.localload.util.AppDispatchers
import org.dsqrwym.localload.util.TaskIdGenerator
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * 调度器实现类，负责根据配置生成不同流量的请求任务
 */
class SchedulerImpl : Scheduler {
    private var job: Job? = null

    override fun start(config: LoadTestConfig): Flow<RequestTask> = channelFlow {
        job = launch(AppDispatchers.IO) {
            val start = TimeSource.Monotonic.markNow()
            // 创建流量函数
            val rateFunction = RateFunctionFactory.create(config.rate)

            var emitted = 0.0
            var lastTimeMs = 0L

            while (isActive) {
                val nowMs = start.elapsedNow().inWholeMilliseconds
                if (nowMs >= config.durationMs) break
                // 获取当前时间的 RPS
                val rps = rateFunction.rpsAt(nowMs)

                val dt = (nowMs - lastTimeMs).coerceAtLeast(1L)

                // 积分：rps × 时间片
                emitted += rps * (dt / 1000.0)
                // 消耗预算
                while (emitted >= 1.0) {
                    // 生成请求任务
                    send(
                        RequestTask(
                            id = TaskIdGenerator.nextId(),
                            url = config.url,
                            method = config.method
                        )
                    )

                    emitted -= 1.0
                }
                lastTimeMs = nowMs

                delay(1.milliseconds)
            }
        }

        awaitClose { job?.cancel() }
    }

    override fun stop() {
        job?.cancel()
    }
}
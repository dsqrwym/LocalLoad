package org.dsqrwym.localload.engine.config

import io.ktor.http.*
import org.dsqrwym.localload.engine.http.executor.HttpConfig
import org.dsqrwym.localload.engine.rate.RateFunctionSpec

enum class WorkerPoolMode {
    QUEUE,       // Scheduler -> WorkerPool
    CLOSED_LOOP  // autocannon/wrk
}

data class LoadTestConfig(
    /**
     * 目标 URL
     */
    val url: String,
    /**
     * HTTP 方法
     */
    val method: HttpMethod = HttpMethod.Get,
    /**
     * 压测持续时间
     * e.g. 30s / 60s / 5min
     */
    val durationMs: Long,
    /**
     * 并发连接数（WorkerPool semaphore 上限）
     */
    val concurrency: Int = 100,
    /**
     * WorkerPool 模式
     */
    val workerPoolMode: WorkerPoolMode = WorkerPoolMode.CLOSED_LOOP,
    /**
     * 流量配置
     */
    val rate: RateFunctionSpec = RateFunctionSpec.Constant(rps = 100.0),
    /**
     * HTTP 配置（HttpConfig）
     */
    val http: HttpConfig = HttpConfig(),
    /**
     * 是否启用 keep-alive（覆盖 http 层）
     */
    val keepAlive: Boolean = true,
    /**
     * 是否在错误率超过阈值时停止
     */
    val maxErrorRate: Double = 1.0,
    /**
     * 是否启用自动停止（类似 k6 thresholds）
     */
    val autoStopOnError: Boolean = false
)
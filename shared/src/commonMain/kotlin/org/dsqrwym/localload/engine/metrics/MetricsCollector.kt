package org.dsqrwym.localload.engine.metrics

import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dsqrwym.localload.engine.execution.ExecutionRecorder
import kotlin.math.sqrt

class MetricsCollector: ExecutionRecorder {
    private val mutex = Mutex()

    private var totalRequests = 0L
    private var successCount = 0L
    private var errorCount = 0L

    private val latencies = mutableListOf<Long>()

    private var mean = 0.0
    private var m2 = 0.0

    // RPS
    private val secondBuckets = mutableListOf<Long>()
    private var currentSecondCount = 0L

    override suspend fun record(
        latencyMs: Long,
        success: Boolean,
        statusCode: HttpStatusCode?,
        bytesRead: Long
    ) {
        mutex.withLock {
            totalRequests++

            if (success) {
                successCount++
            } else {
                errorCount++
            }

            latencies.add(latencyMs)

            updateVariance(latencyMs.toDouble())

            currentSecondCount++
        }
    }

    /**
     * Engine 每秒调用一次
     */
    suspend fun tick() {
        mutex.withLock {
            secondBuckets.add(currentSecondCount)
            currentSecondCount = 0
        }
    }

    /**
     * 测试结束时调用
     */
    suspend fun flush() {
        mutex.withLock {
            secondBuckets.add(currentSecondCount)
            currentSecondCount = 0
        }
    }

    private fun updateVariance(value: Double) {
        val count = totalRequests
        val delta = value - mean
        mean += delta / count
        m2 += delta * (value - mean)
    }

    suspend fun snapshot(): MetricsSnapshot =
        mutex.withLock {

            MetricsSnapshot(
                totalRequests = totalRequests,
                successCount = successCount,
                errorCount = errorCount,
                currentRps = currentSecondCount.toDouble(),
                errorRate =
                    if (totalRequests == 0L) {
                        0.0
                    } else {
                        errorCount.toDouble() / totalRequests
                    },
                avgLatencyMs = mean
            )
        }

    suspend fun buildReport(): TestReport =
        mutex.withLock {
            val sortedLatencies =
                latencies.sorted()
            val variance =
                if (totalRequests > 1) {
                    m2 / (totalRequests - 1)
                } else {
                    0.0
                }

            val stdev =
                sqrt(variance)

            TestReport(
                totalRequests = totalRequests,
                successCount = successCount,
                errorCount = errorCount,
                successRate =
                    if (totalRequests == 0L) {
                        0.0
                    } else {
                        successCount.toDouble() / totalRequests
                    },
                errorRate =
                    if (totalRequests == 0L) {
                        0.0
                    } else {
                        errorCount.toDouble() / totalRequests
                    },
                avgLatencyMs = mean,
                stdevLatencyMs = stdev,
                p50LatencyMs = percentile(sortedLatencies, 50.0),
                p95LatencyMs = percentile(sortedLatencies, 95.0),
                p99LatencyMs = percentile(sortedLatencies, 99.0),
                maxLatencyMs =  sortedLatencies.lastOrNull() ?: 0,
                avgRps =
                    if (secondBuckets.isEmpty()) {
                        0.0
                    } else {
                        secondBuckets.average()
                    },
                minRps = secondBuckets.minOrNull() ?: 0,
                maxRps = secondBuckets.maxOrNull() ?: 0
            )
        }

    private fun percentile(
        values: List<Long>,
        percentile: Double
    ): Long {
        if (values.isEmpty()) return 0
        val index = ((percentile / 100.0) * (values.size - 1)).toInt()
        return values[index]
    }
}
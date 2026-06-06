package org.dsqrwym.localload.engine.metrics

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dsqrwym.localload.engine.execution.RequestResult
import kotlin.math.sqrt
import kotlin.time.TimeSource

class MetricsCollector {
    private val mutex = Mutex()

    private var totalRequests = 0L
    private var successCount = 0L
    private var errorCount = 0L

    private val latencies = mutableListOf<Long>()

    private var mean = 0.0
    private var m2 = 0.0

    // RPS tracking
    private val secondBuckets = mutableListOf<Long>()

    private var currentSecondCount = 0L

    private val startMark = TimeSource.Monotonic.markNow()

    private var lastBucketSecond = 0L

    suspend fun record(result: RequestResult) {
        mutex.withLock {
            totalRequests++

            if (result.success) {
                successCount++
            } else {
                errorCount++
            }

            val latency = result.latencyMs
            latencies.add(latency)
            updateVariance(latency.toDouble())
            updateRpsBucket()
        }
    }

    private fun updateVariance(value: Double) {
        val count = totalRequests

        val delta = value - mean
        mean += delta / count
        m2 += delta * (value - mean)
    }

    private fun updateRpsBucket() {
        val second =
            startMark.elapsedNow().inWholeSeconds
        if (second != lastBucketSecond) {
            secondBuckets.add(currentSecondCount)
            currentSecondCount = 0
            lastBucketSecond = second
        }
        currentSecondCount++
    }

    suspend fun snapshot(): MetricsSnapshot =
        mutex.withLock {

            MetricsSnapshot(
                totalRequests = totalRequests,
                successCount = successCount,
                errorCount = errorCount,
                currentRps = currentSecondCount.toDouble(),
                errorRate =
                    if (totalRequests == 0L) 0.0
                    else errorCount.toDouble() / totalRequests,
                avgLatencyMs = mean
            )
        }

    suspend fun buildReport(): TestReport =
        mutex.withLock {

            val sortedLatencies =
                latencies.sorted()
            val p50 =
                percentile(sortedLatencies, 50.0)
            val p95 =
                percentile(sortedLatencies, 95.0)
            val p99 =
                percentile(sortedLatencies, 99.0)

            val variance =
                if (totalRequests > 1)
                    m2 / (totalRequests - 1)
                else
                    0.0

            val stdev =
                sqrt(variance)

            val buckets =
                buildList {
                    addAll(secondBuckets)
                    add(currentSecondCount)
                }

            val avgRps =
                if (buckets.isEmpty())
                    0.0
                else
                    buckets.average()

            val minRps =
                buckets.minOrNull() ?: 0

            val maxRps =
                buckets.maxOrNull() ?: 0

            TestReport(
                totalRequests = totalRequests,
                successCount = successCount,
                errorCount = errorCount,
                successRate =
                    if (totalRequests == 0L) 0.0
                    else successCount.toDouble() / totalRequests,
                errorRate =
                    if (totalRequests == 0L) 0.0
                    else errorCount.toDouble() / totalRequests,
                avgLatencyMs = mean,
                stdevLatencyMs = stdev,
                p50LatencyMs = p50,
                p95LatencyMs = p95,
                p99LatencyMs = p99,
                maxLatencyMs =
                    sortedLatencies.maxOrNull() ?: 0,
                avgRps = avgRps,
                minRps = minRps,
                maxRps = maxRps
            )
        }

    private fun percentile(
        values: List<Long>,
        percentile: Double
    ): Long {
        if (values.isEmpty()) return 0
        val index =
            ((percentile / 100.0) * (values.size - 1))
                .toInt()
        return values[index]
    }
}
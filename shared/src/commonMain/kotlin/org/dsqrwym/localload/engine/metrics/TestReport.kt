package org.dsqrwym.localload.engine.metrics

data class TestReport(
    val totalRequests: Long,
    val successCount: Long,
    val errorCount: Long,

    val successRate: Double,
    val errorRate: Double,

    val avgLatencyMs: Double,
    val stdevLatencyMs: Double,

    val p50LatencyMs: Long,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long,

    val maxLatencyMs: Long,

    val avgRps: Double,
    val minRps: Long,
    val maxRps: Long
)
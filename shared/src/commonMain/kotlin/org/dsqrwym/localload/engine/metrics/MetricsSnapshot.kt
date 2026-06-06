package org.dsqrwym.localload.engine.metrics

data class MetricsSnapshot(
    val totalRequests: Long,
    val successCount: Long,
    val errorCount: Long,

    val currentRps: Double,
    val errorRate: Double,

    val avgLatencyMs: Double
)
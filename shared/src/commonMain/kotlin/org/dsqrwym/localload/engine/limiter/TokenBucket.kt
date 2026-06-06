package org.dsqrwym.localload.engine.limiter

/**
 * 控制请求速率的令牌桶算法实现
 * @param capacity 令牌桶的容量
 * @param refillRatePerSec 每秒填充的令牌数
 */
class TokenBucket(
    private val capacity: Double,
    private val refillRatePerSec: Double
) {
    private var tokens = capacity
    private var lastMs = 0L

    fun tryConsume(nowMs: Long): Boolean {
        refill(nowMs)

        if (tokens < 1.0) return false

        tokens -= 1.0
        return true
    }

    private fun refill(nowMs: Long) {
        if (lastMs == 0L) {
            lastMs = nowMs
            return
        }

        val deltaSec = (nowMs - lastMs) / 1000.0
        tokens = minOf(capacity, tokens + deltaSec * refillRatePerSec)
        lastMs = nowMs
    }
}
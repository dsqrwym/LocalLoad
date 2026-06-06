package org.dsqrwym.localload.engine.rate

/**
 * 线性递增流量 f(t) = start + (target - start) * (t / rampUp)
 */
class RampRateFunction(
    private val startRps: Double,
    private val targetRps: Double,
    private val rampUpMs: Long
) : RateFunction {
    override fun rpsAt(timeMs: Long): Double {
        if (rampUpMs <= 0) return targetRps

        val t = timeMs.coerceAtMost(rampUpMs).toDouble()
        val progress = t / rampUpMs

        return startRps + (targetRps - startRps) * progress
    }
}
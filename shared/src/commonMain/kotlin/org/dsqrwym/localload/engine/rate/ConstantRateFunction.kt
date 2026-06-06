package org.dsqrwym.localload.engine.rate

/**
 * 恒定流量 f(t) = rps
 */
class ConstantRateFunction(
    private val rps: Double
) : RateFunction {
    override fun rpsAt(timeMs: Long): Double = rps
}
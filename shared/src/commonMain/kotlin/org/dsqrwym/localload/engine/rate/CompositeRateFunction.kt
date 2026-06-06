package org.dsqrwym.localload.engine.rate

/**
 * 分段流量
 * 0–10s → 100 RPS
 * 10–20s → 500 RPS
 * 20–30s → 0 RPS
 */
class CompositeRateFunction(
    private val segments: List<RateFunctionSpec.Segment>
) : RateFunction {

    override fun rpsAt(timeMs: Long): Double {
        return segments.firstOrNull {
            timeMs in it.fromMs..it.toMs
        }?.rps ?: 0.0
    }
}
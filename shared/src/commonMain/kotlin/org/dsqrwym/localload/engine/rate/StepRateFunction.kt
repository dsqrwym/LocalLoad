package org.dsqrwym.localload.engine.rate

/**
 * 分段递增流量
 * 0ms → 10 RPS
 * 10s → 100 RPS
 * 30s → 1000 RPS
 */
class StepRateFunction(
    private val steps: List<RateFunctionSpec.StepPoint>
) : RateFunction {

    override fun rpsAt(timeMs: Long): Double {
        var current = steps.first().rps

        for (step in steps) {
            if (timeMs >= step.atMs) {
                current = step.rps
            } else break
        }

        return current
    }
}
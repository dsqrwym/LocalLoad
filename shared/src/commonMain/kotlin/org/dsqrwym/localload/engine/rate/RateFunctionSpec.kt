package org.dsqrwym.localload.engine.rate

/**
 * 用户定义流量形状的配置
 */
sealed class RateFunctionSpec {
    /**
     * 恒定流量
     */
    data class Constant(val rps: Double) : RateFunctionSpec()

    /**
     * 线性递增流量
     */
    data class Ramp(
        val startRps: Double,
        val targetRps: Double,
        val rampUpMs: Long
    ) : RateFunctionSpec()

    /**
     * 分段递增流量
     */
    data class Step(
        val steps: List<StepPoint>
    ) : RateFunctionSpec()

    /**
     * 分段流量
     */
    data class Composite(
        val segments: List<Segment>
    ) : RateFunctionSpec()


    /**
     * 分段递增流量数据类
     */
    data class StepPoint(
        val atMs: Long,
        val rps: Double
    )

    /**
     * 分段流量数据类
     */
    data class Segment(
        val fromMs: Long,
        val toMs: Long,
        val rps: Double
    )
}

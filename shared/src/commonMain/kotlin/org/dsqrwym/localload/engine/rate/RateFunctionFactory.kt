package org.dsqrwym.localload.engine.rate

/**
 * 根据用户定义的流量形状配置创建相应的流量函数或者行为
 */
object RateFunctionFactory {
    fun create(spec: RateFunctionSpec): RateFunction {
        return when (spec) {
            is RateFunctionSpec.Constant ->
                ConstantRateFunction(spec.rps)

            is RateFunctionSpec.Ramp ->
                RampRateFunction(
                    spec.startRps,
                    spec.targetRps,
                    spec.rampUpMs
                )

            is RateFunctionSpec.Step ->
                StepRateFunction(spec.steps)

            is RateFunctionSpec.Composite ->
                CompositeRateFunction(spec.segments)
        }
    }
}
package org.dsqrwym.localload.engine.rate

/**
 * 表示一个流量函数或者说执行流量的行为，根据时间返回每秒请求数
 */

interface RateFunction {
    fun rpsAt(timeMs: Long): Double
}
package org.dsqrwym.localload.engine.scheduler

import kotlinx.coroutines.flow.Flow
import org.dsqrwym.localload.engine.config.LoadTestConfig
import org.dsqrwym.localload.engine.execution.RequestTask

/**
 * 调度器接口，定义了开始和停止调度的方法
 * 决定什么时候生成请求以及生成多少请求
 */
interface Scheduler {
    fun start(config: LoadTestConfig): Flow<RequestTask>
    fun stop()
}
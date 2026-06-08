package org.dsqrwym.localload.engine.runtime

import kotlinx.coroutines.flow.Flow
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.execution.RequestTask

// 通用生命周期接口
interface WorkerPool {
    fun start()
    suspend fun shutdown()
    fun results(): Flow<RequestResult>
}

// 扩展接口：仅适用于需要外部主动提交任务的模式
interface QueueWorkerPool : WorkerPool {
    suspend fun submit(task: RequestTask)
}
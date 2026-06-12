package org.dsqrwym.localload.engine.runtime

import org.dsqrwym.localload.engine.execution.RequestTask

// 通用生命周期接口
interface WorkerPool {
    fun start()
    suspend fun shutdown()
}

// 扩展接口：仅适用于需要外部主动提交任务的模式
interface QueueWorkerPool : WorkerPool {
    suspend fun submit(task: RequestTask)
}
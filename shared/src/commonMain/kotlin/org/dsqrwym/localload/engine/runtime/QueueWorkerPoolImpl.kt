package org.dsqrwym.localload.engine.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.dsqrwym.localload.engine.config.EngineConstants
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.execution.RequestTask
import org.dsqrwym.localload.util.AppDispatchers
import org.dsqrwym.localload.util.getCpuCount

/**
 * QueueWorkerPoolImpl 生产者-消费者模型 (Producer-Consumer), 用于管理协程池，负责消费任务并执行
 * @param executor 请求执行器
 * @param concurrency 并发数
 * @param scope 协程作用域
 */
class QueueWorkerPoolImpl(
    private val executor: KtorExecutor,
    private val concurrency: Int,
    private val scope: CoroutineScope,
    private val workerCount: Int = getCpuCount() * 2
) : WorkerPool, QueueWorkerPool {
    /**
     * 任务队列
     */
    private val taskChannel = Channel<RequestTask>(capacity = concurrency * 2)

    /**
     * 结果队列
     */
    private val resultChannel = Channel<RequestResult>(capacity = concurrency * 2)

    /**
     * 信号量或者说并发控制
     */
    private val semaphore = Semaphore(concurrency)


    private val workers = mutableListOf<Job>()

    // 启动 WorkerPool
    override fun start() {
        repeat(workerCount) {
            val job = scope.launch(AppDispatchers.IO) {
                // 循环外创建 属于每个 worker 的 buffer
                val buffer = ByteArray(EngineConstants.IO_BUFFER_SIZE_BYTES)
                workerLoop(buffer)
            }
            workers.add(job)
        }
    }

    // Worker 主循环
    private suspend fun workerLoop(buffer: ByteArray) {
        for (task in taskChannel) {
            // 获取许可去运行，没许可会直接挂起等待
            semaphore.withPermit {
                executor.execute(task, buffer)
            }
        }
    }

    // 提交任务
    override suspend fun submit(task: RequestTask) {
        taskChannel.send(task)
    }

    // 停止
    override suspend fun shutdown() {
        taskChannel.close()
        workers.forEach { it.cancelAndJoin() }
        resultChannel.close()
    }
}
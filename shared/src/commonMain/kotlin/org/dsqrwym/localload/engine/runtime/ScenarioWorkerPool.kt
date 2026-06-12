package org.dsqrwym.localload.engine.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.dsqrwym.localload.engine.config.EngineConstants
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.execution.RequestResult
import org.dsqrwym.localload.engine.execution.RequestTask
import org.dsqrwym.localload.util.AppDispatchers

/**
 * ScenarioWorkerPool 自驱动循环模型 (Active-Polling/Self-Driven)
 * @param executor 请求执行器
 * @param scope 协程作用域
 */
class ScenarioWorkerPool(
    private val executor: KtorExecutor,
    private val concurrency: Int,
    private val scope: CoroutineScope,
    private val taskFactory: () -> RequestTask
) : WorkerPool {
    private val workers = mutableListOf<Job>()
    private val resultChannel = Channel<RequestResult>(Channel.BUFFERED)

    override fun start() {
        repeat(concurrency) {
            val buffer = ByteArray(EngineConstants.IO_BUFFER_SIZE_BYTES)
            workers.add(scope.launch(AppDispatchers.IO) {
                // 自驱动模型：循环执行直到作用域取消
                while (isActive) {
                    val task = taskFactory()
                    executor.execute(task, buffer)
                }
            })
        }
    }

    override suspend fun shutdown() {
        workers.forEach { it.cancelAndJoin() }
        resultChannel.close()
    }
}
package org.dsqrwym.localload.engine.execution

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.time.TimeSource

class KtorExecutor(
    private val client: HttpClient,
    private val recorder: ExecutionRecorder
) {
    /**
     * 执行请求并记录结果
     * @param task 请求任务
     * @param buffer 用于读取响应的 buffer
     * */
    suspend fun execute(task: RequestTask, buffer: ByteArray) {
        val start = TimeSource.Monotonic.markNow()
        var bytesRead = 0L
        var success = false
        var statusCode: HttpStatusCode? = null

        try {
            client.prepareRequest(task.url) {
                method = task.method
                task.headers.forEach { (k, v) ->
                    headers.append(k, v)
                }
                task.body?.let {
                    setBody(it)
                }
            }.execute { response ->
                statusCode = response.status
                success = response.status.isSuccess()
                // 获取 byteReadChannel
                val chanel = response.bodyAsChannel()
                // 消费响应来达到默认测 TTLB (Time To Last Byte)
                while (!chanel.isClosedForRead) {
                    // 读取数据时使用外部传入的 buffer 避免循环内重复创建使内存使用暴涨
                    val read = chanel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    bytesRead += read
                }
            }
            recorder.record(
                latencyMs = start.elapsedNow().inWholeMilliseconds,
                success = success,
                statusCode = statusCode,
                bytesRead = bytesRead
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            recorder.record(
                latencyMs = start.elapsedNow().inWholeMilliseconds,
                success = false,
                statusCode = null,
                bytesRead = 0
            )
        }
    }
}
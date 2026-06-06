package org.dsqrwym.localload.engine.execution

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.time.TimeSource

class KtorExecutor(
    private val client: HttpClient
) {
    suspend fun execute(task: RequestTask): RequestResult {
        val start = TimeSource.Monotonic.markNow()

        return try {
            val response: HttpResponse = client.request(task.url) {
                method = task.method
                // headers
                task.headers.forEach { (k, v) ->
                    headers.append(k, v)
                }
                task.body?.let {
                    setBody(it)
                }
            }
            // 消费响应来达到默认测 TTLB (Time To Last Byte)
            response.bodyAsChannel().discard()
            RequestResult(
                requestId = task.id,
                success = response.status.isSuccess(),
                httpStatus = response.status,
                latencyMs = start.elapsedNow().inWholeMilliseconds
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RequestResult(
                requestId = task.id,
                success = false,
                httpStatus = null,
                latencyMs = start.elapsedNow().inWholeMilliseconds,
                errorMessage = e.message
            )
        }
    }
}
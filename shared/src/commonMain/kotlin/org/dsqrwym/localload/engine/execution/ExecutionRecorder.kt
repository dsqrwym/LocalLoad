package org.dsqrwym.localload.engine.execution

import io.ktor.http.*

interface ExecutionRecorder {
    suspend fun record(
        latencyMs: Long,
        success: Boolean,
        statusCode: HttpStatusCode?,
        bytesRead: Long
    )
}
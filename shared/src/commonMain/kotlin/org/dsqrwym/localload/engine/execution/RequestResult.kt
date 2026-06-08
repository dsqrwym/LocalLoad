package org.dsqrwym.localload.engine.execution

import io.ktor.http.*
import kotlin.time.Clock

data class RequestResult(
    val requestId: Long,

    val success: Boolean,
    val httpStatus: HttpStatusCode?,
    val latencyMs: Long,
    val errorMessage: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
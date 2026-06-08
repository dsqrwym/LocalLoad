package org.dsqrwym.localload.engine.execution

import io.ktor.http.*

data class RequestTask(
    val id: Long,
    val url: String,
    val method: HttpMethod,

    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)
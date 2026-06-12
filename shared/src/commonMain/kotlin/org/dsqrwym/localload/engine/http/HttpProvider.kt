package org.dsqrwym.localload.engine.http

import io.ktor.client.*
import io.ktor.client.plugins.*

expect class HttpProvider(httpConfig: HttpConfig) {
    val capabilities: HttpProviderCapabilities
    val client: HttpClient
}

internal fun HttpClientConfig<*>.installCommonConfig(httpConfig: HttpConfig) {
    followRedirects = httpConfig.followRedirects
    install(HttpTimeout) {
        connectTimeoutMillis = httpConfig.connectTimeoutMs
        requestTimeoutMillis = httpConfig.requestTimeoutMs
    }
}
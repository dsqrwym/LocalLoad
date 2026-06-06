package org.dsqrwym.localload.engine.http.executor

import io.ktor.client.*
import io.ktor.client.plugins.*
import org.dsqrwym.localload.engine.http.HttpProviderCapabilities

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
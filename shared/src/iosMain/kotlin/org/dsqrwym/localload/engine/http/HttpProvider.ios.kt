package org.dsqrwym.localload.engine.http

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

actual class HttpProvider actual constructor(val httpConfig: HttpConfig) {
    actual val capabilities: HttpProviderCapabilities =
        object : HttpProviderCapabilities {
            override val supportsKeepAlive = false
            override val supportsCompressionControl = false
            override val supportsHttpVersionSelection = false
        }

    actual val client: HttpClient = HttpClient(Darwin) {
        installCommonConfig(httpConfig)

        engine {
            configureSession {
                HTTPMaximumConnectionsPerHost = Long.MAX_VALUE
                requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData
                // iOS uses NSURLSession, which manages connection pooling and compression automatically.
            }
        }
    }
}
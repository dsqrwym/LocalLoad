package org.dsqrwym.localload.engine.http.executor

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.dsqrwym.localload.engine.http.HttpProviderCapabilities

actual class HttpProvider actual constructor(val httpConfig: HttpConfig) {
    actual val capabilities: HttpProviderCapabilities = object : HttpProviderCapabilities {
        override val supportsKeepAlive: Boolean = false
        override val supportsCompressionControl: Boolean = false
        override val supportsHttpVersionSelection: Boolean = false
    }

    actual val client: HttpClient = HttpClient(Js) {
        installCommonConfig(httpConfig)
        defaultRequest {
            // 强制浏览器不读取缓存，也不写入缓存
            header("Cache-Control", "no-store, no-cache, must-revalidate")
            header("Pragma", "no-cache")
        }
        /*
        https://htmlspecs.com/fetch/#forbidden-request-header
        根据文档内容禁止修改或者修改了也没用
        defaultRequest {
            // 处理 Keep-Alive
            if (!httpConfig.keepAlive) {
                header("Connection", "close")
            }
            // 处理 Compression
            if (!httpConfig.compression) {
                header("Accept-Encoding", "identity")
            }
        }
         */
    }

}
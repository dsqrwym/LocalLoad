package org.dsqrwym.localload.engine.http.executor

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import okhttp3.Dispatcher
import okhttp3.Protocol
import org.dsqrwym.localload.engine.http.HttpProviderCapabilities

actual class HttpProvider actual constructor(val httpConfig: HttpConfig) {
    actual val capabilities: HttpProviderCapabilities = object : HttpProviderCapabilities {
        override val supportsKeepAlive: Boolean = true
        override val supportsCompressionControl: Boolean = true
        override val supportsHttpVersionSelection: Boolean = true
    }

    actual val client: HttpClient = HttpClient(OkHttp) {
       installCommonConfig(httpConfig)

        engine {
            config {
                // 显示禁用 OkHttp 的缓存，虽然默认开启
                cache(null)

                dispatcher(
                    Dispatcher().apply {
                        maxRequestsPerHost = Int.MAX_VALUE
                        maxRequests = Int.MAX_VALUE
                    }
                )
                protocols(
                    when (httpConfig.version) {
                        HttpVersion.AUTO ->
                            listOf(
                                Protocol.HTTP_2,
                                Protocol.HTTP_1_1
                            )

                        HttpVersion.HTTP1_1 ->
                            listOf(Protocol.HTTP_1_1)

                        HttpVersion.HTTP2 ->
                            listOf(Protocol.HTTP_2)
                    }
                )
                // OkHttp 默认 keepAlive 为 true
                if (!httpConfig.keepAlive) {
                    addInterceptor { chain ->
                        val request =
                            chain.request()
                                .newBuilder()
                                .header("Connection", "close")
                                .build()

                        chain.proceed(request)
                    }
                }
                // OkHttp 默认 Accept-Encoding: gzip 允许压缩响应
                if (!httpConfig.compression) {
                    addInterceptor { chain ->
                        val request =
                            chain.request()
                                .newBuilder()
                                .header("Accept-Encoding", "identity")
                                .build()

                        chain.proceed(request)
                    }
                }
            }
        }
    }
}
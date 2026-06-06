package org.dsqrwym.localload.engine.http.executor

data class HttpConfig(
    val version: HttpVersion = HttpVersion.AUTO,
    /**
     * 连接超时时间（毫秒）
     */
    val connectTimeoutMs: Long = 10_000,
    /**
     * 请求超时时间（毫秒）
     */
    val requestTimeoutMs: Long = 10_000,
    /**
     * 是否跟随重定向
     */
    val followRedirects: Boolean = false,
    /**
     * 复用 TCP 连接
     */
    val keepAlive: Boolean = true,
    /**
     * 是否允许服务器返回 gzip/deflate/br 压缩响应
     */
    val compression: Boolean = true,
)
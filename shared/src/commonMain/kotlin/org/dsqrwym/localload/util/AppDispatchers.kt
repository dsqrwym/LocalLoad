package org.dsqrwym.localload.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 用来解决 commonMain 因为 Web 平台不支持从而无法设置 Dispatchers.IO 的问题
 */
expect object AppDispatchers {
    val IO: CoroutineDispatcher
}
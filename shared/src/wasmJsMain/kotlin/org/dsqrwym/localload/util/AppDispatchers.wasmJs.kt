package org.dsqrwym.localload.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object AppDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.Default
}
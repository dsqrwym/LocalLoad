package org.dsqrwym.localload.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual object AppDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}
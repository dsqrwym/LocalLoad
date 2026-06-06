package org.dsqrwym.localload.util

import kotlinx.browser.window
import kotlin.math.max

actual fun getCpuCount(): Int {
    val concurrency = window.navigator.hardwareConcurrency.toInt()
    return max(4, concurrency)
}
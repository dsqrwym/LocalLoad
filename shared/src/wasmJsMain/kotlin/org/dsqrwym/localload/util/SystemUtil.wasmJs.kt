package org.dsqrwym.localload.util

import kotlinx.browser.window

@OptIn(ExperimentalWasmJsInterop::class)
actual fun getCpuCount(): Int {
    val concurrency = window.navigator.hardwareConcurrency.toInt()
    return if (concurrency > 0) concurrency else 1
}
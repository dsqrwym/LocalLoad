package org.dsqrwym.localload.util

actual fun getCpuCount(): Int = Runtime.getRuntime().availableProcessors()
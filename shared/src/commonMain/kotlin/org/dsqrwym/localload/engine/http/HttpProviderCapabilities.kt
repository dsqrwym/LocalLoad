package org.dsqrwym.localload.engine.http

interface HttpProviderCapabilities {
    val supportsKeepAlive: Boolean
    val supportsCompressionControl: Boolean
    val supportsHttpVersionSelection: Boolean
}
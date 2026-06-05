package org.dsqrwym.localload

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
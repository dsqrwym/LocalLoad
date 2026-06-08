package org.dsqrwym.localload.util

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

object TaskIdGenerator {
    @OptIn(ExperimentalAtomicApi::class)
    private val counter = AtomicLong(0)
    @OptIn(ExperimentalAtomicApi::class)
    fun nextId(): Long = counter.fetchAndIncrement()
}
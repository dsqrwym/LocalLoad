package org.dsqrwym.localload.util

import kotlin.math.round

private val POW10 = doubleArrayOf(
    1.0,
    10.0,
    100.0,
    1000.0,
    10000.0,
    100000.0,
    1000000.0,
    10000000.0,
    100000000.0,
    1000000000.0,
)

fun Double.round(decimals: Int = 2): Double {
    if (decimals !in 0..9) error("Invalid decimals")
    val factor = POW10[decimals]
    return round(this * factor) / factor
}

fun Double.roundToDisplay(decimals: Int = 2): String {
    return this.round(decimals).toString()
}
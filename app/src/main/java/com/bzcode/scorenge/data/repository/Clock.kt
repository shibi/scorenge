package com.bzcode.scorenge.data.repository

/** Wall-clock time in epoch milliseconds; swapped for a fixed clock in tests. */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

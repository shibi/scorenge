package com.bzcode.scorenge.domain.scoring

/**
 * Round elapsed time that pauses while the round page is closed.
 *
 * Persist [accumulatedMillis] on the round. While the page is open, [openedAtMillis] holds the time it
 * was opened; nothing runs in the background. On close, call [close] and persist the result.
 */
data class RoundTimer(
    val accumulatedMillis: Long = 0L,
    val openedAtMillis: Long? = null,
) {
    val isRunning: Boolean get() = openedAtMillis != null

    fun open(nowMillis: Long): RoundTimer =
        if (isRunning) this else copy(openedAtMillis = nowMillis)

    fun close(nowMillis: Long): RoundTimer =
        if (!isRunning) this
        else RoundTimer(accumulatedMillis = elapsedMillis(nowMillis), openedAtMillis = null)

    /** Total elapsed time, counting only the time the round page was open. Never negative. */
    fun elapsedMillis(nowMillis: Long): Long {
        val sinceOpen = openedAtMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedMillis + sinceOpen
    }
}

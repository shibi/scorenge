package com.bzcode.scorenge.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RoundTimerTest {

    @Test
    fun `time while closed is not counted`() {
        var timer = RoundTimer().open(nowMillis = 1_000)
        timer = timer.close(nowMillis = 61_000) // open for 60s
        assertFalse(timer.isRunning)
        assertEquals(60_000, timer.elapsedMillis(nowMillis = 999_999)) // paused

        timer = timer.open(nowMillis = 500_000)
        assertEquals(90_000, timer.elapsedMillis(nowMillis = 530_000)) // 60s + 30s
    }

    @Test
    fun `opening twice keeps the first open time`() {
        val timer = RoundTimer().open(1_000).open(5_000)
        assertEquals(9_000, timer.elapsedMillis(10_000))
    }

    @Test
    fun `clock going backwards never yields negative time`() {
        val timer = RoundTimer(accumulatedMillis = 10_000).open(50_000)
        assertEquals(10_000, timer.elapsedMillis(40_000))
    }
}

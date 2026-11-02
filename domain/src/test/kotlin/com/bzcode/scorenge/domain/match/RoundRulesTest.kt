package com.bzcode.scorenge.domain.match

import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundRulesTest {
    @Test
    fun nextRoundStopsAfterTheLastFixedRound() {
        assertEquals(2, RoundRules.nextRoundNumber(1, RoundMode.FIXED, 3))
        assertNull(RoundRules.nextRoundNumber(3, RoundMode.FIXED, 3))
        assertEquals(42, RoundRules.nextRoundNumber(41, RoundMode.DYNAMIC, null))
    }

    @Test
    fun finishNeedsACompletedRoundAndNoScoresInProgress() {
        val done = RoundState("r1", RoundStatus.COMPLETED, 2)
        assertEquals(FinishBlocker.NO_COMPLETED_ROUND, RoundRules.finishBlocker(listOf(RoundState("r1", RoundStatus.PENDING, 0))))
        assertEquals(FinishBlocker.ROUND_IN_PROGRESS, RoundRules.finishBlocker(listOf(done, RoundState("r2", RoundStatus.ACTIVE, 1))))
        assertNull(RoundRules.finishBlocker(listOf(done, RoundState("r2", RoundStatus.ACTIVE, 0))))
        assertNull(RoundRules.finishBlocker(listOf(done, RoundState("r2", RoundStatus.PENDING, 0))))
    }

    @Test
    fun finishDiscardsUnplayedRoundsOnly() {
        val rounds = listOf(
            RoundState("r1", RoundStatus.COMPLETED, 2),
            RoundState("r2", RoundStatus.ACTIVE, 0),
            RoundState("r3", RoundStatus.PENDING, 0),
        )
        assertEquals(listOf("r2", "r3"), RoundRules.roundsDiscardedOnFinish(rounds).map { it.id })
    }
}

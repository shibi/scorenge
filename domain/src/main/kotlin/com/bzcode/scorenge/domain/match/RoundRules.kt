package com.bzcode.scorenge.domain.match

import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus

/** Rules for moving a match from round to round and finishing it. */
object RoundRules {

    /**
     * The round number to create as PENDING after round [completedNumber] completes, or null when the
     * match has played all its fixed rounds.
     */
    fun nextRoundNumber(completedNumber: Int, roundMode: RoundMode, totalRounds: Int?): Int? = when {
        roundMode == RoundMode.FIXED && totalRounds != null && completedNumber >= totalRounds -> null
        else -> completedNumber + 1
    }

    /** Why a match can't be finished yet, or null when it can. */
    fun finishBlocker(rounds: List<RoundState>): FinishBlocker? = when {
        rounds.any { it.status == RoundStatus.ACTIVE && it.enteredScores > 0 } -> FinishBlocker.ROUND_IN_PROGRESS
        rounds.none { it.status == RoundStatus.COMPLETED } -> FinishBlocker.NO_COMPLETED_ROUND
        else -> null
    }

    /**
     * Rounds that finishing removes: the unopened pending round, and an opened round with no scores
     * entered yet. Neither counts towards the result.
     */
    fun roundsDiscardedOnFinish(rounds: List<RoundState>): List<RoundState> = rounds.filter {
        it.status == RoundStatus.PENDING || (it.status == RoundStatus.ACTIVE && it.enteredScores == 0)
    }
}

/** What the round rules need to know about a round. */
data class RoundState(val id: String, val status: RoundStatus, val enteredScores: Int)

enum class FinishBlocker(val message: String) {
    ROUND_IN_PROGRESS("Complete the round in progress, or clear its scores, before finishing."),
    NO_COMPLETED_ROUND("Complete at least one round before finishing."),
}

package com.bzcode.scorenge.domain.scoring

import com.bzcode.scorenge.domain.model.RoundStatus

/** A player or team that receives scores in a match. */
data class SubjectRef(
    val id: String,
    val name: String,
)

/**
 * One round's scores, keyed by subject id. A missing key or a null value means "not entered yet",
 * which is different from a score of zero.
 */
data class RoundScores(
    val roundNumber: Int,
    val status: RoundStatus,
    val scores: Map<String, Long?>,
)

/**
 * One leaderboard row.
 *
 * @property officialTotal sum of completed rounds only; null when the subject has no completed score yet.
 * @property activeRoundScore score entered so far in the active round, if any.
 * @property projectedTotal officialTotal plus activeRoundScore; null when neither exists.
 * @property rank competition rank (1, 2, 2, 4) on the total this board was ranked by; null means unranked.
 */
data class LeaderboardEntry(
    val subjectId: String,
    val subjectName: String,
    val officialTotal: Long?,
    val activeRoundScore: Long?,
    val projectedTotal: Long?,
    val rank: Int?,
)

/** Which total a leaderboard is ranked by. */
enum class RankBasis {
    /** Completed rounds only. Used for the final scoreboard and official standings. */
    OFFICIAL,

    /** Completed rounds plus scores entered so far in the active round. Always shown as provisional. */
    PROVISIONAL,
}

data class Leaderboard(
    val basis: RankBasis,
    /** Ranked entries first (best first), then unranked entries in their original order. */
    val entries: List<LeaderboardEntry>,
) {
    /** All subjects sharing rank 1. Empty when nobody is ranked yet. */
    val leaders: List<LeaderboardEntry> get() = entries.filter { it.rank == 1 }

    val isProvisional: Boolean get() = basis == RankBasis.PROVISIONAL
}

/** Running total after each completed round, for the final scoreboard's line chart. */
data class CumulativeSeries(
    val subjectId: String,
    val subjectName: String,
    /** Index i holds the total after the i-th completed round (in round-number order). */
    val totals: List<Long>,
)

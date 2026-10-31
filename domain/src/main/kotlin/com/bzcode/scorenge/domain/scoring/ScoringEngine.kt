package com.bzcode.scorenge.domain.scoring

import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoringDirection

/**
 * Pure scoring rules: totals, competition ranking and chart data. No Android, Room or network code.
 *
 * Official totals use completed rounds only. The provisional view adds the active round's entered
 * scores and must be labelled as provisional in the UI. Pending rounds never count.
 */
object ScoringEngine {

    fun leaderboard(
        subjects: List<SubjectRef>,
        rounds: List<RoundScores>,
        direction: ScoringDirection,
        basis: RankBasis,
    ): Leaderboard {
        val completed = rounds.filter { it.status == RoundStatus.COMPLETED }
        val active = rounds.firstOrNull { it.status == RoundStatus.ACTIVE }

        val unrankedEntries = subjects.map { subject ->
            val completedScores = completed.mapNotNull { it.scores[subject.id] }
            val official = if (completedScores.isEmpty()) null else completedScores.sum()
            val activeScore = active?.scores?.get(subject.id)
            val projected = when {
                official == null && activeScore == null -> null
                else -> (official ?: 0L) + (activeScore ?: 0L)
            }
            LeaderboardEntry(
                subjectId = subject.id,
                subjectName = subject.name,
                officialTotal = official,
                activeRoundScore = activeScore,
                projectedTotal = projected,
                rank = null,
            )
        }

        val rankingTotal: (LeaderboardEntry) -> Long? = when (basis) {
            RankBasis.OFFICIAL -> { e -> e.officialTotal }
            RankBasis.PROVISIONAL -> { e -> e.projectedTotal }
        }

        val (withTotal, withoutTotal) = unrankedEntries.partition { rankingTotal(it) != null }
        val ranks = competitionRanks(withTotal.map { rankingTotal(it)!! }, direction)
        val ranked = withTotal.zip(ranks) { entry, rank -> entry.copy(rank = rank) }
            .sortedWith(compareBy<LeaderboardEntry> { it.rank }) // stable: ties keep input order

        return Leaderboard(basis = basis, entries = ranked + withoutTotal)
    }

    /**
     * Competition ("1224") ranking. Returns the rank for each input value, in input order.
     * Equal values share a rank and the next rank is skipped.
     */
    fun competitionRanks(values: List<Long>, direction: ScoringDirection): List<Int> {
        val better: (Long, Long) -> Boolean = when (direction) {
            ScoringDirection.HIGHER_WINS -> { a, b -> a > b }
            ScoringDirection.LOWER_WINS -> { a, b -> a < b }
        }
        return values.map { value -> 1 + values.count { other -> better(other, value) } }
    }

    /** Running totals per subject after each completed round, ordered by round number. */
    fun cumulativeSeries(subjects: List<SubjectRef>, rounds: List<RoundScores>): List<CumulativeSeries> {
        val completed = rounds
            .filter { it.status == RoundStatus.COMPLETED }
            .sortedBy { it.roundNumber }
        return subjects.map { subject ->
            var running = 0L
            CumulativeSeries(
                subjectId = subject.id,
                subjectName = subject.name,
                totals = completed.map { round ->
                    running += round.scores[subject.id] ?: 0L
                    running
                },
            )
        }
    }

    /** Subject ids still missing a score in [round]. A round can be completed only when this is empty. */
    fun missingScores(subjects: List<SubjectRef>, round: RoundScores): List<String> =
        subjects.map { it.id }.filter { round.scores[it] == null }
}

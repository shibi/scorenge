package com.bzcode.scorenge.data.repository

import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.local.dao.RecentMatchRow
import com.bzcode.scorenge.domain.stats.Finish
import com.bzcode.scorenge.domain.stats.StatsCalculator
import com.bzcode.scorenge.domain.stats.StatsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Finished matches: Home's recent list and the Stats tab. */
class StatsRepository @Inject constructor(private val db: ScorengeDatabase) {

    fun observeRecentMatches(userId: String, limit: Int = RECENT_LIMIT): Flow<List<RecentMatchRow>> =
        db.eventDao().observeRecentFinished(userId, limit)

    fun observeSummary(userId: String): Flow<StatsSummary> =
        db.eventDao().observeFinishes(userId).map { rows ->
            StatsCalculator.summarize(
                rows.map { Finish(it.playerId, it.playerName, it.eventId, it.gameId, it.gameTitle, it.gameIcon, it.finalRank) },
            )
        }

    companion object {
        const val RECENT_LIMIT = 10
    }
}

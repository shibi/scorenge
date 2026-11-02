package com.bzcode.scorenge.domain.stats

/**
 * One player's finish in one finished match. In team matches every team member gets the team's rank.
 * [rank] is null when the player or team had no score in any completed round.
 */
data class Finish(
    val playerId: String,
    val playerName: String,
    val eventId: String,
    val gameId: String,
    val gameTitle: String,
    val gameIcon: String?,
    val rank: Int?,
)

data class PlayerStats(
    val playerId: String,
    val name: String,
    val played: Int,
    /** Finished first, including shared first place. */
    val wins: Int,
    /** Finished in the top three. */
    val podiums: Int,
) {
    /** Whole percent, rounded down. */
    val winPercent: Int get() = if (played == 0) 0 else wins * 100 / played
}

data class GameStats(
    val gameId: String,
    val title: String,
    val icon: String?,
    val played: Int,
    /** Players with the most wins in this game, and that number; empty when nobody has won yet. */
    val topWinners: List<String>,
    val topWins: Int,
)

data class StatsSummary(
    val matchesPlayed: Int,
    val players: List<PlayerStats>,
    val games: List<GameStats>,
)

object StatsCalculator {
    const val PODIUM_PLACES = 3

    fun summarize(finishes: List<Finish>): StatsSummary {
        val players = finishes.groupBy { it.playerId }.map { (id, rows) ->
            val perMatch = rows.distinctBy { it.eventId }
            PlayerStats(
                playerId = id,
                name = rows.last().playerName,
                played = perMatch.size,
                wins = perMatch.count { it.rank == 1 },
                podiums = perMatch.count { it.rank != null && it.rank <= PODIUM_PLACES },
            )
        }.sortedWith(
            compareByDescending<PlayerStats> { it.wins }
                .thenByDescending { it.podiums }
                .thenByDescending { it.played }
                .thenBy { it.name.lowercase() },
        )

        val games = finishes.groupBy { it.gameId }.map { (id, rows) ->
            val winsByPlayer = rows.filter { it.rank == 1 }
                .distinctBy { it.eventId to it.playerId }
                .groupBy { it.playerId }
            val topWins = winsByPlayer.values.maxOfOrNull { it.size } ?: 0
            GameStats(
                gameId = id,
                title = rows.first().gameTitle,
                icon = rows.first().gameIcon,
                played = rows.map { it.eventId }.distinct().size,
                topWinners = winsByPlayer.values.filter { it.size == topWins && topWins > 0 }
                    .map { it.last().playerName }
                    .sortedBy { it.lowercase() },
                topWins = topWins,
            )
        }.sortedWith(compareByDescending<GameStats> { it.played }.thenBy { it.title.lowercase() })

        return StatsSummary(
            matchesPlayed = finishes.map { it.eventId }.distinct().size,
            players = players,
            games = games,
        )
    }
}

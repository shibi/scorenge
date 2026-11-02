package com.bzcode.scorenge.domain.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsCalculatorTest {
    private fun finish(player: String, event: String, rank: Int?, game: String = "golf") =
        Finish(player, player.replaceFirstChar { it.uppercase() }, event, game, game.replaceFirstChar { it.uppercase() }, null, rank)

    @Test
    fun countsWinsPodiumsAndMatchesPerPlayer() {
        val summary = StatsCalculator.summarize(
            listOf(
                finish("maya", "e1", 1), finish("leo", "e1", 2), finish("ana", "e1", 4),
                finish("maya", "e2", 1), finish("leo", "e2", 1), // shared first place
                finish("ana", "e3", 3, game = "chess"), finish("leo", "e3", null, game = "chess"),
            ),
        )

        assertEquals(3, summary.matchesPlayed)
        assertEquals(listOf("Maya", "Leo", "Ana"), summary.players.map { it.name })
        val maya = summary.players[0]
        assertEquals(Triple(2, 2, 2), Triple(maya.played, maya.wins, maya.podiums))
        assertEquals(100, maya.winPercent)
        val leo = summary.players[1]
        assertEquals(Triple(3, 1, 2), Triple(leo.played, leo.wins, leo.podiums))
        assertEquals(33, leo.winPercent)
        val ana = summary.players[2]
        assertEquals(Triple(2, 0, 1), Triple(ana.played, ana.wins, ana.podiums))
    }

    @Test
    fun gamesShowMatchesPlayedAndTopWinners() {
        val summary = StatsCalculator.summarize(
            listOf(
                finish("maya", "e1", 1), finish("leo", "e1", 2),
                finish("maya", "e2", 2), finish("leo", "e2", 1),
                finish("ana", "e3", 2, game = "chess"), finish("leo", "e3", 2, game = "chess"),
            ),
        )

        val golf = summary.games.first()
        assertEquals("Golf", golf.title)
        assertEquals(2, golf.played)
        assertEquals(listOf("Leo", "Maya"), golf.topWinners)
        assertEquals(1, golf.topWins)
        val chess = summary.games.last()
        assertEquals(emptyList(), chess.topWinners)
        assertEquals(0, chess.topWins)
    }
}

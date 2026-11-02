package com.bzcode.scorenge.domain.match

import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.ScoringMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchSetupRulesTest {
    private val base = MatchSetup(
        gameId = "g1",
        scoringMode = ScoringMode.INDIVIDUAL,
        roundMode = RoundMode.DYNAMIC,
        totalRounds = 5,
        playerIds = listOf("maya"),
        teams = emptyList(),
    )

    @Test
    fun `one player and a game is enough`() {
        assertTrue(MatchSetupRules.problems(base).isEmpty())
    }

    @Test
    fun `needs a game and a player`() {
        val problems = MatchSetupRules.problems(base.copy(gameId = null, playerIds = emptyList()))
        assertEquals(listOf(SetupProblem.NO_GAME, SetupProblem.NO_PLAYERS), problems)
    }

    @Test
    fun `fixed rounds must be 1 to 99, unlimited ignores the count`() {
        val fixed = base.copy(roundMode = RoundMode.FIXED)
        assertTrue(MatchSetupRules.problems(fixed.copy(totalRounds = 1)).isEmpty())
        assertTrue(MatchSetupRules.problems(fixed.copy(totalRounds = 99)).isEmpty())
        assertEquals(listOf(SetupProblem.ROUNDS_OUT_OF_RANGE), MatchSetupRules.problems(fixed.copy(totalRounds = 0)))
        assertEquals(listOf(SetupProblem.ROUNDS_OUT_OF_RANGE), MatchSetupRules.problems(fixed.copy(totalRounds = 100)))
        assertTrue(MatchSetupRules.problems(base.copy(totalRounds = 0)).isEmpty())
    }

    @Test
    fun `team mode needs named, non-empty, distinct teams with no shared players`() {
        val teamMode = base.copy(scoringMode = ScoringMode.TEAM, playerIds = emptyList())
        assertEquals(listOf(SetupProblem.NO_TEAMS), MatchSetupRules.problems(teamMode))

        val oneTeam = teamMode.copy(teams = listOf(TeamSetup("Red", listOf("maya"))))
        assertTrue(MatchSetupRules.problems(oneTeam).isEmpty())
        assertEquals(listOf("maya"), oneTeam.participantIds)

        val bad = teamMode.copy(
            teams = listOf(
                TeamSetup("Red", listOf("maya", "leo")),
                TeamSetup("red ", listOf("leo")),
                TeamSetup(" ", emptyList()),
            ),
        )
        assertEquals(
            listOf(
                SetupProblem.BLANK_TEAM_NAME,
                SetupProblem.DUPLICATE_TEAM_NAME,
                SetupProblem.EMPTY_TEAM,
                SetupProblem.PLAYER_ON_TWO_TEAMS,
            ),
            MatchSetupRules.problems(bad),
        )
        assertEquals(listOf("maya", "leo"), bad.participantIds)
    }
}

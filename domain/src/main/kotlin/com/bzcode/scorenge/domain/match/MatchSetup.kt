package com.bzcode.scorenge.domain.match

import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.ScoringMode

/** A team being set up: its name and the players on it, in order. */
data class TeamSetup(
    val name: String,
    val playerIds: List<String>,
)

/** Everything chosen on the New match screen, before anything is saved. */
data class MatchSetup(
    val gameId: String?,
    val scoringMode: ScoringMode,
    val roundMode: RoundMode,
    /** Used only for [RoundMode.FIXED]. */
    val totalRounds: Int,
    /** Individual mode: the players, in order. Ignored in team mode. */
    val playerIds: List<String>,
    /** Team mode: the teams, in order. Ignored in individual mode. */
    val teams: List<TeamSetup>,
) {
    /** Everyone taking part, in order: the players, or every team's members. */
    val participantIds: List<String>
        get() = when (scoringMode) {
            ScoringMode.INDIVIDUAL -> playerIds.distinct()
            ScoringMode.TEAM -> teams.flatMap { it.playerIds }.distinct()
        }
}

/** Why a match can't start yet. */
enum class SetupProblem(val message: String) {
    NO_GAME("Pick a game."),
    NO_PLAYERS("Add at least one player."),
    NO_TEAMS("Add at least one team."),
    EMPTY_TEAM("Every team needs at least one player."),
    BLANK_TEAM_NAME("Every team needs a name."),
    DUPLICATE_TEAM_NAME("Team names must be different."),
    PLAYER_ON_TWO_TEAMS("A player can only be on one team."),
    ROUNDS_OUT_OF_RANGE("Rounds must be between ${MatchSetupRules.MIN_ROUNDS} and ${MatchSetupRules.MAX_ROUNDS}."),
}

object MatchSetupRules {
    const val MIN_ROUNDS = 1
    const val MAX_ROUNDS = 99
    const val DEFAULT_FIXED_ROUNDS = 5

    /** All problems, in the order they appear on screen. Empty means the match can start. */
    fun problems(setup: MatchSetup): List<SetupProblem> = buildList {
        if (setup.gameId == null) add(SetupProblem.NO_GAME)
        when (setup.scoringMode) {
            ScoringMode.INDIVIDUAL -> if (setup.playerIds.isEmpty()) add(SetupProblem.NO_PLAYERS)
            ScoringMode.TEAM -> {
                val teams = setup.teams
                if (teams.isEmpty()) {
                    add(SetupProblem.NO_TEAMS)
                } else {
                    if (teams.any { it.name.isBlank() }) add(SetupProblem.BLANK_TEAM_NAME)
                    val names = teams.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }
                    if (names.size != names.distinct().size) add(SetupProblem.DUPLICATE_TEAM_NAME)
                    if (teams.any { it.playerIds.isEmpty() }) add(SetupProblem.EMPTY_TEAM)
                    val all = teams.flatMap { it.playerIds.distinct() }
                    if (all.size != all.distinct().size) add(SetupProblem.PLAYER_ON_TWO_TEAMS)
                }
            }
        }
        if (setup.roundMode == RoundMode.FIXED && setup.totalRounds !in MIN_ROUNDS..MAX_ROUNDS) {
            add(SetupProblem.ROUNDS_OUT_OF_RANGE)
        }
    }
}

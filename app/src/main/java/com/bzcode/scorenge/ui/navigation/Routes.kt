package com.bzcode.scorenge.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object GamesRoute
@Serializable data object PlayersRoute
@Serializable data object StatsRoute

/** Match setup. Full screen: the pill bar is hidden. */
@Serializable data object NewMatchRoute

/** Settings, opened from the avatar on Home. Full screen. */
@Serializable data object SettingsRoute

/** Create (gameId null) or edit a game. Full screen. */
@Serializable data class GameEditRoute(val gameId: String? = null)

/** A match: standings, rounds, finish. Full screen. */
@Serializable data class MatchRoute(val eventId: String)

/** One round's page: clock and score entry. Full screen. */
@Serializable data class RoundRoute(val eventId: String, val roundNumber: Int)

/** Final standings of a finished match. Full screen. */
@Serializable data class ResultsRoute(val eventId: String)

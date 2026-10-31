package com.bzcode.scorenge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

/**
 * The four icon destinations of the floating pill bar, in bar order. "+ New match" sits in the
 * middle of the bar but is an action, not a destination. The bar shows only on these destinations,
 * so match, round and final screens are full screen.
 */
enum class TopLevelDestination(
    val label: String,
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("Home", HomeRoute, HomeRoute::class, Icons.Rounded.Home, Icons.Outlined.Home),
    GAMES("All games", GamesRoute, GamesRoute::class, Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    PLAYERS("Players", PlayersRoute, PlayersRoute::class, Icons.Rounded.Groups, Icons.Outlined.Groups),
    STATS("Stats", StatsRoute, StatsRoute::class, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
}

package com.bzcode.scorenge.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bzcode.scorenge.data.auth.AuthUser
import com.bzcode.scorenge.data.repository.InProgressMatch
import com.bzcode.scorenge.ui.common.PlaceholderScreen
import com.bzcode.scorenge.ui.games.GameEditPage
import com.bzcode.scorenge.ui.games.GamesPage
import com.bzcode.scorenge.ui.home.HomeScreen
import com.bzcode.scorenge.ui.match.MatchPage
import com.bzcode.scorenge.ui.match.NewMatchPage
import com.bzcode.scorenge.ui.players.PlayersPage
import com.bzcode.scorenge.ui.settings.SettingsScreen

/** Signed-in app shell: the nav graph plus the floating pill bar, which shows only on top-level destinations. */
@Composable
fun ScorengeAppRoot(
    user: AuthUser,
    onSignOut: () -> Unit,
    inProgress: InProgressMatch? = null,
    navController: NavHostController = rememberNavController(),
    newMatchContent: @Composable (onBack: () -> Unit, onCreateGame: () -> Unit, onStarted: (String) -> Unit) -> Unit =
        { onBack, onCreateGame, onStarted -> NewMatchPage(onBack = onBack, onCreateGame = onCreateGame, onStarted = onStarted) },
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevel = backStackEntry?.destination?.topLevelDestination()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTopLevel != null) {
                PillNavBar(
                    selected = currentTopLevel,
                    onSelect = navController::navigateToTopLevel,
                    onNewMatch = { navController.navigate(NewMatchRoute) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    firstName = user.firstName,
                    inProgress = inProgress,
                    onNewMatch = { navController.navigate(NewMatchRoute) },
                    onContinueMatch = { navController.navigate(MatchRoute(eventId = it)) },
                    onSearchGames = { navController.navigateToTopLevel(TopLevelDestination.GAMES) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<GamesRoute> {
                GamesPage(
                    onAddGame = { navController.navigate(GameEditRoute()) },
                    onOpenGame = { navController.navigate(GameEditRoute(gameId = it)) },
                )
            }
            composable<GameEditRoute> {
                GameEditPage(onDone = { navController.popBackStack() })
            }
            composable<PlayersRoute> {
                PlayersPage()
            }
            composable<StatsRoute> {
                PlaceholderScreen(title = "Stats", message = "Wins and podiums across all games will show here.")
            }
            composable<NewMatchRoute> {
                newMatchContent(
                    { navController.popBackStack() },
                    { navController.navigate(GameEditRoute()) },
                    { eventId ->
                        navController.navigate(MatchRoute(eventId = eventId)) {
                            popUpTo<NewMatchRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<MatchRoute> {
                MatchPage(onBack = { navController.popBackStack() })
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    user = user,
                    onBack = { navController.popBackStack() },
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

internal fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { destination ->
        hierarchy.any { it.hasRoute(destination.routeClass) }
    }

/** Switches tabs without stacking them, keeping each tab's state. */
internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

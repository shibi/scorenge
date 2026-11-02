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
import com.bzcode.scorenge.ui.common.PlaceholderScreen
import com.bzcode.scorenge.ui.home.HomeScreen
import com.bzcode.scorenge.ui.settings.SettingsScreen

/** Signed-in app shell: the nav graph plus the floating pill bar, which shows only on top-level destinations. */
@Composable
fun ScorengeAppRoot(
    user: AuthUser,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
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
                    onNewMatch = { navController.navigate(NewMatchRoute) },
                    onSearchGames = { navController.navigateToTopLevel(TopLevelDestination.GAMES) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<GamesRoute> {
                PlaceholderScreen(title = "All games", message = "Your games will be listed here.")
            }
            composable<PlayersRoute> {
                PlaceholderScreen(title = "Players", message = "Add the people you play with here.")
            }
            composable<StatsRoute> {
                PlaceholderScreen(title = "Stats", message = "Wins and podiums across all games will show here.")
            }
            composable<NewMatchRoute> {
                PlaceholderScreen(
                    title = "New match",
                    message = "Match setup is coming next.",
                    onBack = { navController.popBackStack() },
                )
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

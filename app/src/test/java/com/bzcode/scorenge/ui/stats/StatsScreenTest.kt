package com.bzcode.scorenge.ui.stats

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.domain.stats.GameStats
import com.bzcode.scorenge.domain.stats.PlayerStats
import com.bzcode.scorenge.domain.stats.StatsSummary
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsPlayersAndGames() {
        compose.setContent {
            ScorengeTheme {
                StatsScreen(
                    StatsSummary(
                        matchesPlayed = 3,
                        players = listOf(PlayerStats("p1", "Maya", played = 3, wins = 2, podiums = 3)),
                        games = listOf(GameStats("g1", "Golf", "⛳", played = 3, topWinners = listOf("Maya"), topWins = 2)),
                    ),
                )
            }
        }
        compose.onNodeWithText("From 3 finished matches").assertExists()
        compose.onNodeWithText("Maya").assertExists()
        compose.onNodeWithText("66% wins").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("3 matches · most wins: Maya (2)"))
        compose.onNodeWithText("3 matches · most wins: Maya (2)").assertExists()
    }

    @Test
    fun explainsTheEmptyState() {
        compose.setContent { ScorengeTheme { StatsScreen(StatsSummary(0, emptyList(), emptyList())) } }
        compose.onNodeWithText("Finish a match to see wins and podiums here.").assertExists()
    }
}

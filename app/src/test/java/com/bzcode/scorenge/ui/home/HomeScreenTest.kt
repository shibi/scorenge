package com.bzcode.scorenge.ui.home

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.local.dao.RecentMatchRow
import com.bzcode.scorenge.data.repository.InProgressMatch
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheMatchInProgressAndContinuesIt() {
        var continued: String? = null
        val match = InProgressMatch("event-1", "Golf · 2 Nov", null, 3, listOf("Maya", "Leo"))
        compose.setContent {
            ScorengeTheme {
                HomeScreen(
                    firstName = "Sam",
                    inProgress = match,
                    onNewMatch = {},
                    onContinueMatch = { continued = it },
                    onSearchGames = {},
                    onOpenSettings = {},
                )
            }
        }

        compose.onNodeWithText("Golf · 2 Nov").assertExists()
        compose.onNodeWithText("Round 3 · Maya, Leo").assertExists()
        compose.onNodeWithText("No match in progress").assertDoesNotExist()
        compose.onNodeWithText("Continue match").performClick()
        assertEquals("event-1", continued)
    }

    @Test
    fun listsRecentMatchesAndOpensTheirResults() {
        var opened: String? = null
        compose.setContent {
            ScorengeTheme {
                HomeScreen(
                    firstName = "Sam",
                    inProgress = null,
                    onNewMatch = {},
                    onContinueMatch = {},
                    recentMatches = listOf(RecentMatchRow("event-9", "Golf night", null, "⛳", "Maya")),
                    onOpenResults = { opened = it },
                    onSearchGames = {},
                    onOpenSettings = {},
                )
            }
        }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Golf night"))
        compose.onNodeWithText("Finished matches will show up here.").assertDoesNotExist()
        compose.onNodeWithText("🏆 Maya").assertExists()
        compose.onNodeWithText("Golf night").performClick()
        assertEquals("event-9", opened)
    }
}

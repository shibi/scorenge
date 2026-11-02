package com.bzcode.scorenge.ui.navigation

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PillNavBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsSelectionAndReportsTaps() {
        var selected: TopLevelDestination? = null
        var newMatchTaps = 0
        compose.setContent {
            ScorengeTheme {
                PillNavBar(
                    selected = TopLevelDestination.HOME,
                    onSelect = { selected = it },
                    onNewMatch = { newMatchTaps++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Home").assertIsSelected()
        compose.onNodeWithContentDescription("Players").assertIsNotSelected()

        compose.onNodeWithContentDescription("Players").performClick()
        assertEquals(TopLevelDestination.PLAYERS, selected)

        compose.onNodeWithText("New match").performClick()
        assertEquals(1, newMatchTaps)
    }
}

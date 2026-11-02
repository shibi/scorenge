package com.bzcode.scorenge.ui.navigation

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScorengeAppRootTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun barSwitchesTabsAndHidesOnFullScreenPages() {
        compose.setContent { ScorengeTheme { ScorengeAppRoot() } }

        compose.onNodeWithText("Ready to roll").assertExists()
        compose.onNodeWithContentDescription("Home").assertIsSelected()

        compose.onNodeWithContentDescription("Stats").performClick()
        compose.onNodeWithContentDescription("Stats").assertIsSelected()

        compose.onNodeWithText("New match").performClick()
        compose.onNodeWithText("Match setup is coming next.").assertExists()
        compose.onNodeWithContentDescription("Stats").assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Stats").assertIsSelected()
    }
}

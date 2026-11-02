package com.bzcode.scorenge.ui.navigation

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.auth.AuthUser
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScorengeAppRootTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun barSwitchesTabsAndHidesOnFullScreenPages() {
        compose.setContent { ScorengeTheme { ScorengeAppRoot(user = USER, onSignOut = {}) } }

        compose.onNodeWithText("Ready to roll").assertExists()
        compose.onNodeWithText("Hi Sam").assertExists()
        compose.onNodeWithContentDescription("Home").assertIsSelected()

        compose.onNodeWithContentDescription("Stats").performClick()
        compose.onNodeWithContentDescription("Stats").assertIsSelected()

        compose.onNodeWithText("New match").performClick()
        compose.onNodeWithText("Match setup is coming next.").assertExists()
        compose.onNodeWithContentDescription("Stats").assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Stats").assertIsSelected()
    }

    @Test
    fun settingsShowsAccountAndConfirmsSignOut() {
        var signedOut = false
        compose.setContent { ScorengeTheme { ScorengeAppRoot(user = USER, onSignOut = { signedOut = true }) } }

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("sam@example.com").assertExists()
        compose.onNodeWithContentDescription("Home").assertDoesNotExist()

        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Sign out?").assertExists()
        assertFalse(signedOut)

        compose.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
        assertTrue(signedOut)
    }

    private companion object {
        val USER = AuthUser(uid = "uid-1", displayName = "Sam Lee", email = "sam@example.com", photoUrl = null)
    }
}

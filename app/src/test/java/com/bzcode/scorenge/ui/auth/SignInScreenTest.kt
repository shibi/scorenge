package com.bzcode.scorenge.ui.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buttonStartsSignInAndErrorsAreShown() {
        var taps = 0
        compose.setContent {
            ScorengeTheme { SignInScreen(SignInUiState(errorMessage = "Sign-in failed. Please try again."), onSignIn = { taps++ }) }
        }

        compose.onNodeWithText("Continue with Google").performClick()
        assertEquals(1, taps)
        compose.onNodeWithText("Sign-in failed. Please try again.").assertExists()
    }

    @Test
    fun buttonIsDisabledWhileSigningIn() {
        compose.setContent { ScorengeTheme { SignInScreen(SignInUiState(inProgress = true), onSignIn = {}) } }

        compose.onNode(androidx.compose.ui.test.hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun googleIsTheOnlySignInOption() {
        compose.setContent { ScorengeTheme { SignInScreen(SignInUiState(), onSignIn = {}) } }

        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithText("Continue with Google").assertExists()
        compose.onNodeWithContentDescription("Scorenge").assertExists()
    }
}

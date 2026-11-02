package com.bzcode.scorenge.ui.match

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoundScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val base = RoundUiState(
        loading = false,
        matchTitle = "Golf night",
        roundNumber = 2,
        totalRounds = 5,
        status = RoundStatus.ACTIVE,
        startedAt = 0L,
        elapsedMillis = 125_000L,
        openedAt = null, // a paused clock keeps the test from ticking
        fields = listOf(
            ScoreField("s1", "Maya", "4", totalBefore = 10),
            ScoreField("s2", "Leo", "", totalBefore = 8),
        ),
        leading = listOf(
            LeaderboardEntry("s2", "Leo", 8, null, 8, 1),
            LeaderboardEntry("s1", "Maya", 10, 4, 14, 2),
        ),
        missingCount = 1,
    )

    @Test
    fun showsRoundClockLeaderAndBlocksCompletingUntilEveryScoreIsIn() {
        val typed = mutableListOf<Pair<String, String>>()
        compose.setContent {
            ScorengeTheme {
                RoundScreen(state = base, onBack = {}, onScoreChange = { id, text -> typed += id to text }, onComplete = {})
            }
        }

        compose.onNodeWithText("Round 2 of 5").assertExists()
        compose.onNodeWithText("2:05").assertExists()
        compose.onNodeWithText("Leading now").assertExists()
        compose.onNodeWithText("Leo · 8").assertExists()
        compose.onNodeWithText("Enter 1 more score to complete the round.").assertExists()
        compose.onNodeWithText("Complete round").assertIsNotEnabled()

        compose.onNodeWithContentDescription("Score for Leo").performTextInput("6")
        assertEquals(listOf("s2" to "6"), typed)
    }

    @Test
    fun completeIsEnabledOnceAllScoresAreIn() {
        var completed = 0
        compose.setContent {
            ScorengeTheme {
                RoundScreen(state = base.copy(missingCount = 0), onBack = {}, onScoreChange = { _, _ -> }, onComplete = { completed++ })
            }
        }
        compose.onNodeWithText("Complete round").assertIsEnabled().performClick()
        assertEquals(1, completed)
    }

    @Test
    fun completedRoundsAreReadOnly() {
        compose.setContent {
            ScorengeTheme {
                RoundScreen(state = base.copy(status = RoundStatus.COMPLETED), onBack = {}, onScoreChange = { _, _ -> }, onComplete = {})
            }
        }
        compose.onNodeWithText("Complete round").assertDoesNotExist()
        compose.onNodeWithText("This round is complete and its scores are locked.").assertExists()
        compose.onNodeWithContentDescription("Score for Maya").assertIsNotEnabled()
    }
}

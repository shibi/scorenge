package com.bzcode.scorenge.ui.match

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.domain.match.FinishBlocker
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = MatchUiState(
        loading = false,
        title = "Golf night",
        gameTitle = "Golf",
        totalRounds = 3,
        completedRounds = 1,
        subjectNames = listOf("Maya", "Leo"),
        standings = listOf(
            LeaderboardEntry("s2", "Leo", 4, null, 4, 1),
            LeaderboardEntry("s1", "Maya", 7, null, 7, 2),
        ),
        rounds = listOf(
            RoundRow(1, RoundStatus.COMPLETED, 65_000L, listOf(7, 4)),
            RoundRow(2, RoundStatus.PENDING, 0L, listOf(null, null)),
        ),
        nextRound = NextRound.Start(2),
    )

    @Test
    fun startsTheNextRoundAndConfirmsFinishing() {
        var opened: Int? = null
        var finished = 0
        compose.setContent {
            ScorengeTheme {
                MatchScreen(state, onBack = {}, onOpenRound = { opened = it }, onFinish = { finished++ }, onShowResults = {})
            }
        }

        compose.onNodeWithText("1 of 3 rounds played").assertExists()
        compose.onNodeWithText("7 · 4").assertExists()
        compose.onNodeWithText("1:05").assertExists()
        compose.onNodeWithText("Start round 2").performClick()
        assertEquals(2, opened)

        compose.onNodeWithText("Finish match").performClick()
        compose.onNodeWithText("Finish match?").assertExists()
        assertEquals(0, finished)
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, finished)
    }

    @Test
    fun explainsWhyFinishingIsBlocked() {
        compose.setContent {
            ScorengeTheme {
                MatchScreen(
                    state.copy(nextRound = NextRound.Continue(2), finishBlocker = FinishBlocker.ROUND_IN_PROGRESS),
                    onBack = {},
                    onOpenRound = {},
                    onFinish = {},
                    onShowResults = {},
                )
            }
        }
        compose.onNodeWithText("Continue round 2").assertExists()
        compose.onNodeWithText("Finish match").assertIsNotEnabled()
        compose.onNodeWithText(FinishBlocker.ROUND_IN_PROGRESS.message).assertExists()
    }
}

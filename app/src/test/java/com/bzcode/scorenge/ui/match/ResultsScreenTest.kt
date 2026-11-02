package com.bzcode.scorenge.ui.match

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.domain.scoring.CumulativeSeries
import com.bzcode.scorenge.domain.scoring.LeaderboardEntry
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsWinnerPodiumAndScoreChart() {
        val state = MatchUiState(
            loading = false,
            title = "Golf night",
            finished = true,
            completedRounds = 2,
            standings = listOf(
                LeaderboardEntry("s1", "Maya", 12, null, 12, 1),
                LeaderboardEntry("s2", "Leo", 9, null, 9, 2),
                LeaderboardEntry("s3", "Ana", 4, null, 4, 3),
            ),
            series = listOf(
                CumulativeSeries("s1", "Maya", listOf(5, 12)),
                CumulativeSeries("s2", "Leo", listOf(6, 9)),
                CumulativeSeries("s3", "Ana", listOf(1, 4)),
            ),
        )
        compose.setContent { ScorengeTheme { ResultsScreen(state, onDone = {}) } }

        compose.onNodeWithText("Maya wins!").assertExists()
        compose.onNodeWithText("1st").assertExists()
        compose.onNodeWithText("3rd").assertExists()
        compose.onNodeWithContentDescription("Score after each round. Maya: 5, 12. Leo: 6, 9. Ana: 1, 4").assertExists()
        // Podium, chart legend and final list each name every player.
        assertEquals(3, compose.onAllNodesWithText("Ana").fetchSemanticsNodes().size)
    }

    @Test
    fun ordinalsHandleTeens() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st"), listOf(1, 2, 3, 4, 11, 12, 13, 21).map(::ordinal))
    }
}

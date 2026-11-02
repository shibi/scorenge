package com.bzcode.scorenge.ui.games

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun game(id: String, title: String, description: String? = null) = GameEntity(
        id = id,
        userId = "uid-1",
        title = title,
        description = description,
        scoringDirection = ScoringDirection.HIGHER_WINS,
        defaultScoringMode = ScoringMode.INDIVIDUAL,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun filterMatchesTitleOrNotesIgnoringCase() {
        val games = listOf(game("1", "Carrom"), game("2", "Chess", "rapid"), game("3", "Rummy"))
        assertEquals(listOf("1"), filterGames(games, " car").map { it.id })
        assertEquals(listOf("2"), filterGames(games, "RAPID").map { it.id })
        assertEquals(games, filterGames(games, ""))
    }

    @Test
    fun listsGamesWithScoringAndOpensOneOnTap() {
        var opened: String? = null
        compose.setContent {
            ScorengeTheme {
                GamesScreen(
                    state = GamesUiState(loading = false, games = listOf(game("g1", "Carrom")), hasAnyGame = true),
                    onQueryChange = {},
                    onAddGame = {},
                    onOpenGame = { opened = it },
                )
            }
        }

        compose.onNodeWithText("Highest score wins · Each player").assertExists()
        compose.onNodeWithText("Carrom").performClick()
        assertEquals("g1", opened)
    }

    @Test
    fun showsEmptyStateBeforeAnyGame() {
        compose.setContent {
            ScorengeTheme {
                GamesScreen(GamesUiState(loading = false), onQueryChange = {}, onAddGame = {}, onOpenGame = {})
            }
        }
        compose.onNodeWithText("New game").assertExists()
        compose.onNodeWithText("Add the games you play, like Carrom or Rummy. Each game remembers how it's scored.").assertExists()
    }
}

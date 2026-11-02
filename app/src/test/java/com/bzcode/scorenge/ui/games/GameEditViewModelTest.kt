package com.bzcode.scorenge.ui.games

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.auth.AuthUser
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.repository.GameRepository
import com.bzcode.scorenge.data.repository.NameError
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.inlineTestDatabase
import com.bzcode.scorenge.seedUser
import com.bzcode.scorenge.ui.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GameEditViewModelTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var repository: GameRepository
    private val auth = FakeAuthRepository(AuthUser(TEST_USER, "Sam Lee", null, null))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inlineTestDatabase().also { it.seedUser() }
        repository = GameRepository(db.gameDao()) { 1_000L }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(gameId: String? = null) =
        GameEditViewModel(SavedStateHandle(mapOf("gameId" to gameId)), auth, repository)

    @Test
    fun createsAGameWithTheChosenSettings() {
        val vm = viewModel()
        assertTrue(vm.uiState.value.isNew)
        vm.onTitleChange("Golf")
        vm.onIconChange("🎯")
        vm.onDirectionChange(ScoringDirection.LOWER_WINS)
        vm.onModeChange(ScoringMode.TEAM)
        vm.save()

        assertTrue(vm.uiState.value.finished)
        val game = runBlocking { repository.observeActive(TEST_USER).first().single() }
        assertEquals("Golf", game.title)
        assertEquals("🎯", game.icon)
        assertEquals(ScoringDirection.LOWER_WINS, game.scoringDirection)
        assertEquals(ScoringMode.TEAM, game.defaultScoringMode)
    }

    @Test
    fun editingLoadsTheGameAndDuplicateTitleIsRejected() {
        viewModel().apply { onTitleChange("Carrom"); save() }
        viewModel().apply { onTitleChange("Chess"); save() }
        val chess = runBlocking { repository.observeActive(TEST_USER).first().first { it.title == "Chess" } }

        val vm = viewModel(chess.id)
        assertFalse(vm.uiState.value.isNew)
        assertEquals("Chess", vm.uiState.value.title)

        vm.onTitleChange("carrom")
        vm.save()
        assertEquals(NameError.DUPLICATE.message, vm.uiState.value.titleError)
        assertFalse(vm.uiState.value.finished)

        vm.delete()
        assertTrue(vm.uiState.value.finished)
        assertEquals(listOf("Carrom"), runBlocking { repository.observeActive(TEST_USER).first().map { it.title } })
    }
}

package com.bzcode.scorenge.ui.match

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.TEST_USER
import com.bzcode.scorenge.data.auth.AuthUser
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.repository.GameDraft
import com.bzcode.scorenge.data.repository.GameRepository
import com.bzcode.scorenge.data.repository.MatchRepository
import com.bzcode.scorenge.data.repository.PlayerRepository
import com.bzcode.scorenge.data.repository.SaveResult
import com.bzcode.scorenge.domain.match.SetupProblem
import com.bzcode.scorenge.domain.model.MatchScoring
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.inlineTestDatabase
import com.bzcode.scorenge.seedUser
import com.bzcode.scorenge.ui.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NewMatchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var db: ScorengeDatabase
    private lateinit var games: GameRepository
    private lateinit var players: PlayerRepository
    private lateinit var vm: NewMatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inlineTestDatabase().also { it.seedUser() }
        games = GameRepository(db.gameDao()) { 1_000L }
        players = PlayerRepository(db.playerDao()) { 1_000L }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun startViewModel() {
        vm = NewMatchViewModel(
            FakeAuthRepository(AuthUser(TEST_USER, "Sam Lee", null, null)),
            games,
            players,
            MatchRepository(db) { 0L },
        ) { 0L }
        vm.uiState.launchIn(scope.backgroundScope)
    }

    private fun addGame(title: String, mode: ScoringMode = ScoringMode.INDIVIDUAL) = runBlocking {
        (games.save(TEST_USER, null, GameDraft(title, null, null, ScoringDirection.HIGHER_WINS, mode)) as SaveResult.Saved).id
    }

    private fun addPlayer(name: String) = runBlocking { (players.save(TEST_USER, null, name) as SaveResult.Saved).id }

    @Test
    fun picksTheOnlyGameAndNeedsAPlayer() {
        val golf = addGame("Golf")
        startViewModel()

        val state = vm.uiState.value
        assertEquals(golf, state.form.gameId)
        assertEquals(listOf(SetupProblem.NO_PLAYERS), state.problems)
        assertFalse(state.canStart)
        assertTrue(state.defaultTitle.startsWith("Golf · "))
    }

    @Test
    fun selectsANewlyCreatedGameAndAppliesItsDefaultMode() {
        addGame("Golf")
        addGame("Chess")
        startViewModel()
        assertNull(vm.uiState.value.form.gameId)

        val bridge = addGame("Bridge", ScoringMode.TEAM)

        val form = vm.uiState.value.form
        assertEquals(bridge, form.gameId)
        assertEquals(ScoringMode.TEAM, form.scoringMode)
        assertEquals(listOf("Team 1", "Team 2"), form.teams.map { it.name })
    }

    @Test
    fun quickAddPicksTheNewPlayerAndShowsNameErrors() {
        addGame("Golf")
        addPlayer("Maya")
        startViewModel()

        vm.onNewPlayerNameChange("leo")
        vm.addNewPlayer()
        val state = vm.uiState.value
        assertEquals(listOf("leo"), state.players.filter { it.id in state.form.playerIds }.map { it.name })
        assertEquals("", state.form.newPlayerName)
        assertTrue(state.canStart)

        vm.onNewPlayerNameChange("MAYA")
        vm.addNewPlayer()
        assertTrue(vm.uiState.value.form.newPlayerError != null)
    }

    @Test
    fun movingAPlayerBetweenTeamsKeepsThemOnOneTeam() {
        addGame("Bridge", ScoringMode.TEAM)
        val maya = addPlayer("Maya")
        startViewModel()
        val (first, second) = vm.uiState.value.form.teams

        vm.toggleTeamPlayer(first.key, maya)
        vm.toggleTeamPlayer(second.key, maya)

        val teams = vm.uiState.value.form.teams
        assertEquals(emptyList<String>(), teams[0].playerIds)
        assertEquals(listOf(maya), teams[1].playerIds)
        assertEquals(listOf(SetupProblem.EMPTY_TEAM), vm.uiState.value.problems)

        vm.removeTeam(first.key)
        assertTrue(vm.uiState.value.canStart)
    }

    @Test
    fun scoringStartsFromTheGameAndCanBeFinishingOrder() = runBlocking {
        val golf = (games.save(TEST_USER, null, GameDraft("Golf", null, null, ScoringDirection.LOWER_WINS, ScoringMode.INDIVIDUAL)) as SaveResult.Saved).id
        val maya = addPlayer("Maya")
        startViewModel()
        assertEquals(golf, vm.uiState.value.form.gameId)
        assertEquals(MatchScoring.LOWEST_WINS, vm.uiState.value.form.scoring)

        vm.setScoring(MatchScoring.FINISHING_ORDER)
        vm.togglePlayer(maya)
        assertEquals(MatchScoring.FINISHING_ORDER, vm.uiState.value.form.toSetup().scoring)
        assertTrue(vm.uiState.value.canStart)
    }

    @Test
    fun fixedRoundsStayInRange() {
        addGame("Golf")
        startViewModel()
        vm.setTotalRounds(0)
        assertEquals(1, vm.uiState.value.form.totalRounds)
        vm.setTotalRounds(500)
        assertEquals(99, vm.uiState.value.form.totalRounds)
    }
}

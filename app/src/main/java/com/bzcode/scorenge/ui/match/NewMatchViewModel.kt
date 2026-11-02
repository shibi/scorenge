package com.bzcode.scorenge.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.repository.Clock
import com.bzcode.scorenge.data.repository.GameRepository
import com.bzcode.scorenge.data.repository.MatchRepository
import com.bzcode.scorenge.data.repository.PlayerRepository
import com.bzcode.scorenge.data.repository.SaveResult
import com.bzcode.scorenge.domain.match.MatchSetup
import com.bzcode.scorenge.domain.match.MatchSetupRules
import com.bzcode.scorenge.domain.match.SetupProblem
import com.bzcode.scorenge.domain.match.TeamSetup
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.ScoringMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** A team on the setup screen. [key] is local to this screen; real ids are made on start. */
data class TeamDraft(val key: String, val name: String, val playerIds: List<String>)

/** What the user has chosen so far. */
data class NewMatchForm(
    val gameId: String? = null,
    val title: String = "",
    val scoringMode: ScoringMode = ScoringMode.INDIVIDUAL,
    val roundMode: RoundMode = RoundMode.DYNAMIC,
    val totalRounds: Int = MatchSetupRules.DEFAULT_FIXED_ROUNDS,
    /** Individual mode, in the order picked. */
    val playerIds: List<String> = emptyList(),
    val teams: List<TeamDraft> = emptyList(),
    val nextTeamNumber: Int = 1,
    val newPlayerName: String = "",
    val newPlayerError: String? = null,
    val starting: Boolean = false,
    val startError: String? = null,
    /** Set when the match has started; the screen then opens it. */
    val startedEventId: String? = null,
) {
    fun toSetup() = MatchSetup(
        gameId = gameId,
        scoringMode = scoringMode,
        roundMode = roundMode,
        totalRounds = totalRounds,
        playerIds = playerIds,
        teams = teams.map { TeamSetup(it.name, it.playerIds) },
    )
}

data class NewMatchUiState(
    val loading: Boolean = true,
    val games: List<GameEntity> = emptyList(),
    val players: List<PlayerEntity> = emptyList(),
    val form: NewMatchForm = NewMatchForm(),
    /** Shown as the name field's placeholder and used when the name is left empty. */
    val defaultTitle: String = "",
    val problems: List<SetupProblem> = emptyList(),
) {
    val canStart: Boolean get() = !loading && problems.isEmpty() && !form.starting
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewMatchViewModel @Inject constructor(
    authRepository: AuthRepository,
    gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val clock: Clock,
) : ViewModel() {

    private val userId = authRepository.currentUser.filterNotNull().map { it.uid }.distinctUntilChanged()
    private val form = MutableStateFlow(NewMatchForm())
    private var knownGameIds: Set<String>? = null

    private val games = userId.flatMapLatest { gameRepository.observeActive(it) }
        .onEach(::autoSelectGame)
    private val players = userId.flatMapLatest { playerRepository.observeActive(it) }

    val uiState: StateFlow<NewMatchUiState> = combine(games, players, form) { games, players, form ->
        val game = games.firstOrNull { it.id == form.gameId }
        NewMatchUiState(
            loading = false,
            games = games,
            players = players,
            form = form,
            defaultTitle = game?.let { "${it.title} · ${dayLabel(clock.nowMillis())}" }.orEmpty(),
            problems = MatchSetupRules.problems(form.toSetup()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewMatchUiState())

    /** Picks the only game, or a game the user just created from this screen. */
    private fun autoSelectGame(games: List<GameEntity>) {
        val ids = games.map { it.id }.toSet()
        val known = knownGameIds
        knownGameIds = ids
        val current = form.value.gameId
        if (current != null && current in ids) return
        val pick = when {
            known != null -> games.firstOrNull { it.id !in known }
            games.size == 1 -> games.single()
            else -> null
        }
        if (pick != null) selectGame(pick) else if (current != null) form.update { it.copy(gameId = null) }
    }

    fun selectGame(game: GameEntity) = form.update {
        val switched = it.gameId != game.id
        it.copy(gameId = game.id).let { f -> if (switched) f.withMode(game.defaultScoringMode) else f }
    }

    fun onTitleChange(value: String) = form.update { it.copy(title = value) }

    fun setScoringMode(mode: ScoringMode) = form.update { it.withMode(mode) }

    fun setRoundMode(mode: RoundMode) = form.update { it.copy(roundMode = mode) }

    fun setTotalRounds(value: Int) = form.update {
        it.copy(totalRounds = value.coerceIn(MatchSetupRules.MIN_ROUNDS, MatchSetupRules.MAX_ROUNDS))
    }

    fun togglePlayer(playerId: String) = form.update {
        it.copy(playerIds = if (playerId in it.playerIds) it.playerIds - playerId else it.playerIds + playerId)
    }

    fun addTeam() = form.update { it.withNewTeam() }

    fun removeTeam(key: String) = form.update { f -> f.copy(teams = f.teams.filterNot { it.key == key }) }

    fun renameTeam(key: String, name: String) = form.update { f ->
        f.copy(teams = f.teams.map { if (it.key == key) it.copy(name = name) else it })
    }

    /** Adds the player to this team, or takes them off it. A player on another team moves here. */
    fun toggleTeamPlayer(key: String, playerId: String) = form.update { f ->
        val target = f.teams.firstOrNull { it.key == key } ?: return@update f
        val removing = playerId in target.playerIds
        f.copy(
            teams = f.teams.map { team ->
                when {
                    team.key == key && removing -> team.copy(playerIds = team.playerIds - playerId)
                    team.key == key -> team.copy(playerIds = team.playerIds + playerId)
                    else -> team.copy(playerIds = team.playerIds - playerId)
                }
            },
        )
    }

    fun onNewPlayerNameChange(value: String) = form.update { it.copy(newPlayerName = value, newPlayerError = null) }

    /** Creates a player from the quick-add field and, in individual mode, picks them. */
    fun addNewPlayer() {
        val name = form.value.newPlayerName
        viewModelScope.launch {
            when (val result = playerRepository.save(userId.first(), null, name)) {
                is SaveResult.Saved -> form.update {
                    it.copy(
                        newPlayerName = "",
                        newPlayerError = null,
                        playerIds = if (it.scoringMode == ScoringMode.INDIVIDUAL) it.playerIds + result.id else it.playerIds,
                    )
                }
                is SaveResult.Invalid -> form.update { it.copy(newPlayerError = result.error.message) }
            }
        }
    }

    fun start() {
        val state = uiState.value
        if (!state.canStart) return
        form.update { it.copy(starting = true, startError = null) }
        viewModelScope.launch {
            try {
                val title = state.form.title.ifBlank { state.defaultTitle }
                val eventId = matchRepository.startMatch(userId.first(), state.form.toSetup(), title)
                form.update { it.copy(starting = false, startedEventId = eventId) }
            } catch (e: IllegalArgumentException) {
                // A player or game was removed meanwhile; the lists refresh and the user can retry.
                form.update { it.copy(starting = false, startError = "Couldn't start the match. Check the players and game.") }
            }
        }
    }
}

private fun NewMatchForm.withMode(mode: ScoringMode): NewMatchForm {
    val switched = copy(scoringMode = mode)
    return if (mode == ScoringMode.TEAM && teams.isEmpty()) switched.withNewTeam().withNewTeam() else switched
}

private fun NewMatchForm.withNewTeam(): NewMatchForm =
    copy(teams = teams + TeamDraft(key = "team-$nextTeamNumber", name = "Team $nextTeamNumber", playerIds = emptyList()), nextTeamNumber = nextTeamNumber + 1)

private val DayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

internal fun dayLabel(epochMillis: Long): String =
    DayFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

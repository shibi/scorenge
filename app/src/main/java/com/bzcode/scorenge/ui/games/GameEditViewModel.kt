package com.bzcode.scorenge.ui.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.repository.GameDraft
import com.bzcode.scorenge.data.repository.GameRepository
import com.bzcode.scorenge.data.repository.SaveResult
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameEditUiState(
    val isNew: Boolean,
    val loading: Boolean,
    val title: String = "",
    val icon: String? = null,
    val description: String = "",
    val scoringDirection: ScoringDirection = ScoringDirection.HIGHER_WINS,
    val scoringMode: ScoringMode = ScoringMode.INDIVIDUAL,
    val titleError: String? = null,
    val saving: Boolean = false,
    /** Set once saved or deleted; the screen then closes. */
    val finished: Boolean = false,
)

@HiltViewModel
class GameEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    // Key matches GameEditRoute.gameId.
    private val gameId: String? = savedStateHandle.get<String>("gameId")

    private val _uiState = MutableStateFlow(GameEditUiState(isNew = gameId == null, loading = gameId != null))
    val uiState: StateFlow<GameEditUiState> = _uiState.asStateFlow()

    init {
        if (gameId != null) {
            viewModelScope.launch {
                val game = gameRepository.getById(gameId)
                _uiState.update {
                    if (game == null) {
                        it.copy(loading = false, finished = true)
                    } else {
                        it.copy(
                            loading = false,
                            title = game.title,
                            icon = game.icon,
                            description = game.description.orEmpty(),
                            scoringDirection = game.scoringDirection,
                            scoringMode = game.defaultScoringMode,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = null) }
    fun onIconChange(value: String?) = _uiState.update { it.copy(icon = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDirectionChange(value: ScoringDirection) = _uiState.update { it.copy(scoringDirection = value) }
    fun onModeChange(value: ScoringMode) = _uiState.update { it.copy(scoringMode = value) }

    fun save() {
        val state = _uiState.value
        if (state.saving || state.loading) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val userId = authRepository.currentUser.filterNotNull().first().uid
            val draft = GameDraft(
                title = state.title,
                icon = state.icon,
                description = state.description,
                scoringDirection = state.scoringDirection,
                defaultScoringMode = state.scoringMode,
            )
            when (val result = gameRepository.save(userId, gameId, draft)) {
                is SaveResult.Saved -> _uiState.update { it.copy(saving = false, finished = true) }
                is SaveResult.Invalid -> _uiState.update { it.copy(saving = false, titleError = result.error.message) }
            }
        }
    }

    fun delete() {
        val id = gameId ?: return
        viewModelScope.launch {
            gameRepository.delete(id)
            _uiState.update { it.copy(finished = true) }
        }
    }
}

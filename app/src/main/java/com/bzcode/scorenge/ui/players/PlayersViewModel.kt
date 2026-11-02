package com.bzcode.scorenge.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.repository.PlayerRepository
import com.bzcode.scorenge.data.repository.SaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayersUiState(
    val loading: Boolean = true,
    val players: List<PlayerEntity> = emptyList(),
)

/** The add or rename dialog. [playerId] is null when adding. */
data class PlayerEditorState(
    val playerId: String?,
    val name: String,
    val error: String? = null,
    val saving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayersViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val userId = authRepository.currentUser.filterNotNull().map { it.uid }.distinctUntilChanged()

    val uiState: StateFlow<PlayersUiState> = userId
        .flatMapLatest { playerRepository.observeActive(it) }
        .map { PlayersUiState(loading = false, players = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayersUiState())

    private val _editor = MutableStateFlow<PlayerEditorState?>(null)
    val editor: StateFlow<PlayerEditorState?> = _editor.asStateFlow()

    fun startAdd() {
        _editor.value = PlayerEditorState(playerId = null, name = "")
    }

    fun startEdit(player: PlayerEntity) {
        _editor.value = PlayerEditorState(playerId = player.id, name = player.name)
    }

    fun onNameChange(name: String) = _editor.update { it?.copy(name = name, error = null) }

    fun dismissEditor() {
        _editor.value = null
    }

    fun save() {
        val editor = _editor.value ?: return
        if (editor.saving) return
        _editor.value = editor.copy(saving = true)
        viewModelScope.launch {
            when (val result = playerRepository.save(userId.first(), editor.playerId, editor.name)) {
                is SaveResult.Saved -> _editor.value = null
                is SaveResult.Invalid -> _editor.update { it?.copy(saving = false, error = result.error.message) }
            }
        }
    }

    fun remove() {
        val playerId = _editor.value?.playerId ?: return
        _editor.value = null
        viewModelScope.launch { playerRepository.remove(playerId) }
    }
}

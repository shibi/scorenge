package com.bzcode.scorenge.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GamesUiState(
    val loading: Boolean = true,
    val query: String = "",
    /** Games matching [query]. */
    val games: List<GameEntity> = emptyList(),
    val hasAnyGame: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GamesViewModel @Inject constructor(
    authRepository: AuthRepository,
    gameRepository: GameRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val games = authRepository.currentUser.filterNotNull().map { it.uid }.distinctUntilChanged()
        .flatMapLatest { gameRepository.observeActive(it) }

    val uiState: StateFlow<GamesUiState> = combine(games, query) { all, q ->
        GamesUiState(loading = false, query = q, games = filterGames(all, q), hasAnyGame = all.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())

    val currentQuery: StateFlow<String> = query.asStateFlow()

    fun onQueryChange(value: String) {
        query.value = value
    }
}

/** Case-insensitive match on title or description. */
internal fun filterGames(games: List<GameEntity>, query: String): List<GameEntity> {
    val q = query.trim()
    if (q.isEmpty()) return games
    return games.filter { it.title.contains(q, ignoreCase = true) || it.description?.contains(q, ignoreCase = true) == true }
}

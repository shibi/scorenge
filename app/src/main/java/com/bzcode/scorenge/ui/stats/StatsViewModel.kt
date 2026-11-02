package com.bzcode.scorenge.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.repository.StatsRepository
import com.bzcode.scorenge.domain.stats.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(val loading: Boolean = true, val summary: StatsSummary = StatsSummary(0, emptyList(), emptyList()))

@HiltViewModel
class StatsViewModel @Inject constructor(
    authRepository: AuthRepository,
    statsRepository: StatsRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = authRepository.currentUser
        .filterNotNull()
        .flatMapLatest { statsRepository.observeSummary(it.uid) }
        .map { StatsUiState(loading = false, summary = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

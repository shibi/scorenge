package com.bzcode.scorenge.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.repository.InProgressMatch
import com.bzcode.scorenge.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    matchRepository: MatchRepository,
) : ViewModel() {

    /** The match to continue, or null when nothing is in progress. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val inProgress: StateFlow<InProgressMatch?> = authRepository.currentUser
        .flatMapLatest { user -> if (user == null) flowOf(null) else matchRepository.observeLatestInProgress(user.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

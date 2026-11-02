package com.bzcode.scorenge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.sync.SyncPrefs
import com.bzcode.scorenge.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BackupStatus(val lastBackedUpAt: Long? = null, val syncing: Boolean = false)

@HiltViewModel
class BackupViewModel @Inject constructor(
    authRepository: AuthRepository,
    prefs: SyncPrefs,
    private val scheduler: SyncScheduler,
) : ViewModel() {
    val status: StateFlow<BackupStatus> = combine(
        authRepository.currentUser,
        prefs.observeLastSynced(),
        scheduler.observeSyncing(),
    ) { user, lastSynced, syncing ->
        BackupStatus(lastBackedUpAt = user?.uid?.let { lastSynced[it] }, syncing = syncing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupStatus())

    fun backUpNow() = scheduler.syncNow()
}

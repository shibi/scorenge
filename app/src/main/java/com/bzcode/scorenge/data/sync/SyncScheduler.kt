package com.bzcode.scorenge.data.sync

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules backups: shortly after any local change, once when the app starts, and every few hours
 * as a safety net. Work waits for a network connection and retries with backoff.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ScorengeDatabase,
    private val authRepository: AuthRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val workManager get() = WorkManager.getInstance(context)
    private var started = false

    private val observer = object : InvalidationTracker.Observer(SYNCED_TABLES) {
        override fun onInvalidated(tables: Set<String>) = requestSync()
    }

    fun start() {
        if (started) return
        started = true
        db.invalidationTracker.addObserver(observer)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
                .setConstraints(networkConstraint)
                .build(),
        )
        // Each sign-in (including the one already saved at app start) restores if needed, then uploads.
        appScope.launch {
            authRepository.currentUser.map { it?.uid }.filterNotNull().distinctUntilChanged()
                .collect { requestSync(delaySeconds = 0) }
        }
    }

    /** Runs a sync soon. Changes made while one is waiting are picked up by that same run. */
    fun requestSync(delaySeconds: Long = DEBOUNCE_SECONDS) {
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    /** Runs a sync now, replacing one that is waiting. */
    fun syncNow() {
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    /** True while a sync is running. */
    fun observeSyncing(): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(ONE_TIME_WORK).map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING }
        }

    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val ONE_TIME_WORK = "sync"
        const val PERIODIC_WORK = "sync-periodic"
        const val PERIODIC_HOURS = 6L
        const val DEBOUNCE_SECONDS = 15L
        val SYNCED_TABLES = arrayOf(
            "players", "games", "events", "event_participants", "teams", "team_members",
            "scoring_subjects", "rounds", "round_scores",
        )
    }
}

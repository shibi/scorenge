package com.bzcode.scorenge.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bzcode.scorenge.data.auth.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Restores (first run on a device) and uploads the signed-in user's data. Retries with backoff. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = authRepository.currentUser.first()?.uid ?: return Result.success()
        return try {
            withTimeout(TIMEOUT_MILLIS) {
                engine.restoreIfNeeded(uid)
                engine.upload(uid)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed, will retry", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val TAG = "SyncWorker"
        const val TIMEOUT_MILLIS = 120_000L
        const val MAX_ATTEMPTS = 10
    }
}

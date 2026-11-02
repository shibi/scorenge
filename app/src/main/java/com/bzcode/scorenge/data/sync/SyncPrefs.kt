package com.bzcode.scorenge.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small per-user sync bookkeeping kept on the device: how far uploads have got, rows deleted
 * locally that still need deleting in the cloud, whether a restore was done, and the last backup time.
 */
@Singleton
class SyncPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sync", Context.MODE_PRIVATE)
    private val lastSynced = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Rows with updatedAt at or after this were not uploaded yet. */
    fun watermark(uid: String): Long = prefs.getLong("watermark_$uid", 0L)

    fun setWatermark(uid: String, value: Long) = prefs.edit { putLong("watermark_$uid", value) }

    /** Pending cloud deletes as "collection/id". */
    fun tombstones(uid: String): Set<String> = prefs.getStringSet("tombstones_$uid", emptySet()).orEmpty().toSet()

    fun addTombstones(uid: String, entries: Collection<String>) {
        if (entries.isEmpty()) return
        synchronized(this) { prefs.edit { putStringSet("tombstones_$uid", tombstones(uid) + entries) } }
    }

    fun removeTombstones(uid: String, entries: Collection<String>) {
        if (entries.isEmpty()) return
        synchronized(this) { prefs.edit { putStringSet("tombstones_$uid", tombstones(uid) - entries.toSet()) } }
    }

    /** True once this device has checked the cloud for the user's earlier data. */
    fun restoreChecked(uid: String): Boolean = prefs.getBoolean("restored_$uid", false)

    fun setRestoreChecked(uid: String) = prefs.edit { putBoolean("restored_$uid", true) }

    fun lastSyncedAt(uid: String): Long? = prefs.getLong("last_synced_$uid", 0L).takeIf { it > 0 }

    fun setLastSyncedAt(uid: String, value: Long) {
        prefs.edit { putLong("last_synced_$uid", value) }
        lastSynced.value = lastSynced.value + (uid to value)
    }

    /** Last successful backup per user, for Settings; seeded lazily from storage. */
    fun observeLastSynced(): StateFlow<Map<String, Long>> = lastSynced.asStateFlow()
}

package com.bzcode.scorenge.data.sync

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.withTransaction
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.repository.Clock
import javax.inject.Inject

/**
 * Backs up one user's data to the cloud and restores it on a new install.
 *
 * The device is the source of truth while playing. Uploads send every row changed since the last
 * upload (by updatedAt) plus deletes for rows removed locally. A restore runs once per user per
 * device, and only when the device has none of that user's data yet.
 */
class SyncEngine @Inject constructor(
    private val db: ScorengeDatabase,
    private val remote: RemoteStore,
    private val prefs: SyncPrefs,
    private val clock: Clock,
) {
    private val dao get() = db.syncDao()

    /** Uploads changes since the last successful upload. Throws if the cloud can't be reached. */
    suspend fun upload(uid: String) {
        val since = prefs.watermark(uid)
        // Take the watermark before reading, so rows changed during the upload go out next time.
        val startedAt = clock.nowMillis()

        db.userDao().getById(uid)?.let { user ->
            remote.putProfile(
                uid,
                mapOf(
                    "id" to user.id, "displayName" to user.displayName, "email" to user.email,
                    "createdAt" to user.createdAt, "updatedAt" to user.updatedAt,
                ),
            )
        }
        put(uid, SyncCollection.Players, dao.playersSince(uid, since))
        put(uid, SyncCollection.Games, dao.gamesSince(uid, since))
        put(uid, SyncCollection.Events, dao.eventsSince(uid, since))
        put(uid, SyncCollection.Participants, dao.participantsSince(uid, since))
        put(uid, SyncCollection.Teams, dao.teamsSince(uid, since))
        put(uid, SyncCollection.TeamMembers, dao.teamMembersSince(uid, since))
        put(uid, SyncCollection.Subjects, dao.subjectsSince(uid, since))
        put(uid, SyncCollection.Rounds, dao.roundsSince(uid, since))
        put(uid, SyncCollection.Scores, dao.scoresSince(uid, since))

        val tombstones = prefs.tombstones(uid)
        tombstones.groupBy({ it.substringBefore('/') }, { it.substringAfter('/') })
            .forEach { (collection, ids) -> remote.delete(uid, collection, ids) }
        prefs.removeTombstones(uid, tombstones)

        prefs.setWatermark(uid, startedAt)
        prefs.setLastSyncedAt(uid, clock.nowMillis())
    }

    /**
     * On a device with none of this user's data, copies their cloud data into the local database.
     * Runs at most once per user per device; afterwards uploads take over.
     *
     * @return the number of rows restored
     */
    suspend fun restoreIfNeeded(uid: String): Int {
        if (prefs.restoreChecked(uid)) return 0
        if (dao.hasLocalData(uid)) {
            prefs.setRestoreChecked(uid)
            return 0
        }
        val downloaded = SyncCollection.all.associateWith { remote.getAll(uid, it.name) }
        var restored = 0
        db.withTransaction {
            SyncCollection.all.forEach { collection ->
                downloaded.getValue(collection).forEach { doc ->
                    if (insert(collection, doc)) restored++
                }
            }
        }
        prefs.setRestoreChecked(uid)
        // Everything restored is already in the cloud.
        prefs.setWatermark(uid, clock.nowMillis())
        return restored
    }

    private suspend fun <T : Any> put(uid: String, collection: SyncCollection<T>, rows: List<T>) {
        if (rows.isEmpty()) return
        remote.put(uid, collection.name, rows.map { collection.id(it) to collection.toDoc(it) })
    }

    /** Inserts one cloud document. Skips documents that are malformed or point to a missing parent. */
    private suspend fun insert(collection: SyncCollection<*>, doc: Doc): Boolean = try {
        when (collection) {
            SyncCollection.Players -> dao.upsertPlayer(SyncCollection.Players.fromDoc(doc))
            SyncCollection.Games -> dao.upsertGame(SyncCollection.Games.fromDoc(doc))
            SyncCollection.Events -> dao.upsertEvent(SyncCollection.Events.fromDoc(doc))
            SyncCollection.Participants -> SyncCollection.Participants.fromDoc(doc)
                .also { requireParent(dao.eventExists(it.eventId)) }.let { dao.upsertParticipant(it) }
            SyncCollection.Teams -> SyncCollection.Teams.fromDoc(doc)
                .also { requireParent(dao.eventExists(it.eventId)) }.let { dao.upsertTeam(it) }
            SyncCollection.TeamMembers -> SyncCollection.TeamMembers.fromDoc(doc)
                .also { requireParent(dao.teamExists(it.teamId)) }.let { dao.upsertTeamMember(it) }
            SyncCollection.Subjects -> SyncCollection.Subjects.fromDoc(doc)
                .also { requireParent(dao.eventExists(it.eventId)) }.let { dao.upsertSubject(it) }
            SyncCollection.Rounds -> SyncCollection.Rounds.fromDoc(doc)
                .also { requireParent(dao.eventExists(it.eventId)) }.let { dao.upsertRound(it) }
            SyncCollection.Scores -> SyncCollection.Scores.fromDoc(doc)
                .also { requireParent(dao.roundExists(it.roundId)) }.let { dao.upsertScore(it) }
        }
        true
    } catch (e: MissingParentException) {
        Log.w(TAG, "Skipping ${collection.name} document whose parent is missing", e)
        false
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Skipping malformed ${collection.name} document", e)
        false
    } catch (e: SQLiteConstraintException) {
        Log.w(TAG, "Skipping ${collection.name} document whose parent is missing", e)
        false
    }

    /** Thrown when a document's parent wasn't restored; checked here so it never depends on SQLite foreign key enforcement. */
    private class MissingParentException : Exception()

    private fun requireParent(exists: Boolean) {
        if (!exists) throw MissingParentException()
    }

    private companion object {
        const val TAG = "SyncEngine"
    }
}

package com.bzcode.scorenge.data.sync

import javax.inject.Inject

/** Notes rows deleted on this device so the cloud copy can delete them too. */
fun interface DeletionLog {
    fun recordDeleted(uid: String, collection: SyncCollection<*>, ids: Collection<String>)

    companion object {
        val None = DeletionLog { _, _, _ -> }
    }
}

class PrefsDeletionLog @Inject constructor(private val prefs: SyncPrefs) : DeletionLog {
    override fun recordDeleted(uid: String, collection: SyncCollection<*>, ids: Collection<String>) {
        prefs.addTombstones(uid, ids.map { "${collection.name}/$it" })
    }
}

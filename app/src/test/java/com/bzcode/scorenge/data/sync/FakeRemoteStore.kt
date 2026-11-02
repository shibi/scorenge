package com.bzcode.scorenge.data.sync

/** In-memory cloud. Stores numbers as Long, as Firestore returns them. */
class FakeRemoteStore : RemoteStore {
    val collections = mutableMapOf<String, MutableMap<String, Doc>>()
    val profiles = mutableMapOf<String, Doc>()
    val putLog = mutableListOf<Pair<String, Int>>()
    var failing = false

    fun docs(collection: String): Map<String, Doc> = collections[collection].orEmpty()

    override suspend fun put(uid: String, collection: String, docs: List<Pair<String, Doc>>) {
        check(!failing) { "offline" }
        putLog += collection to docs.size
        val target = collections.getOrPut(collection) { mutableMapOf() }
        docs.forEach { (id, doc) -> target[id] = doc.mapValues { (_, v) -> if (v is Int) v.toLong() else v } }
    }

    override suspend fun delete(uid: String, collection: String, ids: List<String>) {
        check(!failing) { "offline" }
        ids.forEach { collections[collection]?.remove(it) }
    }

    override suspend fun getAll(uid: String, collection: String): List<Doc> {
        check(!failing) { "offline" }
        return docs(collection).values.toList()
    }

    override suspend fun putProfile(uid: String, profile: Doc) {
        check(!failing) { "offline" }
        profiles[uid] = profile
    }
}

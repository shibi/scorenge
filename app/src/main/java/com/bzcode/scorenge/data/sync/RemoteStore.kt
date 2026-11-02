package com.bzcode.scorenge.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** The cloud copy of one user's data. Firestore in the app; a fake in tests. */
interface RemoteStore {
    /** Writes (creates or replaces) documents in `users/{uid}/{collection}`. */
    suspend fun put(uid: String, collection: String, docs: List<Pair<String, Doc>>)

    /** Deletes documents from `users/{uid}/{collection}`. */
    suspend fun delete(uid: String, collection: String, ids: List<String>)

    /** Every document in `users/{uid}/{collection}`. */
    suspend fun getAll(uid: String, collection: String): List<Doc>

    /** Writes the `users/{uid}` profile document. */
    suspend fun putProfile(uid: String, profile: Doc)
}

class FirestoreRemoteStore @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RemoteStore {

    private fun collection(uid: String, name: String) =
        firestore.collection(USERS).document(uid).collection(name)

    override suspend fun put(uid: String, collection: String, docs: List<Pair<String, Doc>>) {
        val ref = collection(uid, collection)
        docs.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, doc) -> batch.set(ref.document(id), doc) }
            batch.commit().await()
        }
    }

    override suspend fun delete(uid: String, collection: String, ids: List<String>) {
        val ref = collection(uid, collection)
        ids.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(ref.document(it)) }
            batch.commit().await()
        }
    }

    override suspend fun getAll(uid: String, collection: String): List<Doc> =
        collection(uid, collection).get().await().documents.mapNotNull { it.data }

    override suspend fun putProfile(uid: String, profile: Doc) {
        firestore.collection(USERS).document(uid).set(profile).await()
    }

    private companion object {
        const val USERS = "users"

        /** Firestore allows 500 writes per batch. */
        const val BATCH_LIMIT = 400
    }
}

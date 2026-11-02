package com.bzcode.scorenge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking

/** In-memory database whose queries run on the calling thread, so view model coroutines finish inline. */
fun inlineTestDatabase(): ScorengeDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScorengeDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()

const val TEST_USER = "uid-1"

fun ScorengeDatabase.seedUser(id: String = TEST_USER) = runBlocking {
    userDao().upsert(UserEntity(id, "Sam Lee", null, null, createdAt = 0, updatedAt = 0))
}

package com.bzcode.scorenge.data.auth

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.data.local.ScorengeDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalUserStoreTest {
    private lateinit var db: ScorengeDatabase
    private lateinit var store: LocalUserStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScorengeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LocalUserStore(db.userDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertsThenUpdatesOnlyWhenTheProfileChanges() = runTest {
        val user = AuthUser("uid-1", "Sam Lee", "sam@example.com", null)

        store.save(user, nowMillis = 100)
        store.save(user, nowMillis = 200)
        val unchanged = db.userDao().getById("uid-1")!!
        assertEquals(100L, unchanged.createdAt)
        assertEquals(100L, unchanged.updatedAt)

        store.save(user.copy(displayName = "Sam L."), nowMillis = 300)
        val renamed = db.userDao().getById("uid-1")!!
        assertEquals("Sam L.", renamed.displayName)
        assertEquals(100L, renamed.createdAt)
        assertEquals(300L, renamed.updatedAt)
    }
}

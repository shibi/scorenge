package com.bzcode.scorenge.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ScorengeDatabase::class.java)

    @Test
    fun v1MatchesBecomeTypedScoreMatches() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, displayName, email, photoUrl, createdAt, updatedAt) VALUES ('u', 'Sam', NULL, NULL, 0, 0)")
            db.execSQL(
                "INSERT INTO games (id, userId, title, icon, description, scoringDirection, defaultScoringMode, createdAt, updatedAt, deletedAt) " +
                    "VALUES ('g', 'u', 'Golf', NULL, NULL, 'LOWER_WINS', 'INDIVIDUAL', 0, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO events (id, userId, gameId, title, scoringMode, scoringDirection, roundMode, totalRounds, status, " +
                    "startedAt, completedAt, lastPlayedAt, createdAt, updatedAt, deletedAt) " +
                    "VALUES ('e', 'u', 'g', 'Sunday', 'INDIVIDUAL', 'LOWER_WINS', 'DYNAMIC', NULL, 'IN_PROGRESS', 0, NULL, 0, 0, 0, NULL)",
            )
        }

        helper.runMigrationsAndValidate(DB, 2, true, ScorengeDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT scoreEntry, scoringDirection FROM events WHERE id = 'e'").use { c ->
                c.moveToFirst()
                assertEquals("POINTS", c.getString(0))
                assertEquals("LOWER_WINS", c.getString(1))
            }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}

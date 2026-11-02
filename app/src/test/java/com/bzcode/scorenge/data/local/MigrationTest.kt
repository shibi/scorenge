package com.bzcode.scorenge.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bzcode.scorenge.domain.model.ScoreEntry
import com.bzcode.scorenge.domain.model.ScoringDirection
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Builds a v1 database from the committed v1 schema JSON, then opens it with the current Room
 * database and its migrations. Room checks the migrated schema against the entities on open.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(DB)
    }

    @Test
    fun v1MatchesBecomeTypedScoreMatches() = runBlocking {
        createV1Database()

        val db = Room.databaseBuilder(context, ScorengeDatabase::class.java, DB)
            .addMigrations(ScorengeDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val event = db.eventDao().getById("e")!!
            assertEquals(ScoreEntry.POINTS, event.scoreEntry)
            assertEquals(ScoringDirection.LOWER_WINS, event.scoringDirection)
            assertEquals("Sunday", event.title)
        } finally {
            db.close()
        }
    }

    private fun createV1Database() {
        val schema = JSONObject(File(SCHEMA_V1).readText()).getJSONObject("database")
        val path = context.getDatabasePath(DB).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1

            db.execSQL("INSERT INTO users (id, displayName, email, photoUrl, createdAt, updatedAt) VALUES ('u', 'Sam', NULL, NULL, 0, 0)")
            db.execSQL(
                "INSERT INTO games (id, userId, title, description, icon, scoringDirection, defaultScoringMode, createdAt, updatedAt, deletedAt) " +
                    "VALUES ('g', 'u', 'Golf', NULL, NULL, 'LOWER_WINS', 'INDIVIDUAL', 0, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO events (id, userId, gameId, title, scoringMode, scoringDirection, roundMode, totalRounds, status, " +
                    "startedAt, completedAt, lastPlayedAt, createdAt, updatedAt, deletedAt) " +
                    "VALUES ('e', 'u', 'g', 'Sunday', 'INDIVIDUAL', 'LOWER_WINS', 'DYNAMIC', NULL, 'IN_PROGRESS', 0, NULL, 0, 0, 0, NULL)",
            )
        }
    }

    private companion object {
        const val DB = "migration-test.db"

        // Unit tests run with the app module as the working directory.
        const val SCHEMA_V1 = "schemas/com.bzcode.scorenge.data.local.ScorengeDatabase/1.json"
    }
}

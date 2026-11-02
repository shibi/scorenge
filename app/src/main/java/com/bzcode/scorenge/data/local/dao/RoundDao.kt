package com.bzcode.scorenge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    /** Plain insert: a duplicate round number in the same match must fail, not be merged. */
    @Insert
    suspend fun insert(round: RoundEntity)

    @Update
    suspend fun update(round: RoundEntity)

    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getById(id: String): RoundEntity?

    @Query("SELECT * FROM rounds WHERE eventId = :eventId ORDER BY roundNumber")
    fun observeForEvent(eventId: String): Flow<List<RoundEntity>>

    /** The match's latest round, whatever its status. */
    @Query("SELECT * FROM rounds WHERE eventId = :eventId ORDER BY roundNumber DESC LIMIT 1")
    suspend fun getLatest(eventId: String): RoundEntity?

    /** Saves a score; keyed by its id, so callers reuse the existing row's id for (round, subject). */
    @Upsert
    suspend fun upsertScore(score: RoundScoreEntity)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    fun observeScores(roundId: String): Flow<List<RoundScoreEntity>>

    /** Every score of a match, for totals and charts. */
    @Query(
        """
        SELECT round_scores.* FROM round_scores
        INNER JOIN rounds ON rounds.id = round_scores.roundId
        WHERE rounds.eventId = :eventId
        """,
    )
    fun observeScoresForEvent(eventId: String): Flow<List<RoundScoreEntity>>
}

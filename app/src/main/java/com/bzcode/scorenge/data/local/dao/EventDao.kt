package com.bzcode.scorenge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.TeamEntity
import com.bzcode.scorenge.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

/** Matches ("events") and the rows that make up their line-up. */
@Dao
interface EventDao {
    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: String): Flow<EventEntity?>

    /** In-progress matches, most recently played first. Drives "Continue match". */
    @Query(
        """
        SELECT * FROM events
        WHERE userId = :userId AND status = 'IN_PROGRESS' AND deletedAt IS NULL
        ORDER BY COALESCE(lastPlayedAt, startedAt, createdAt) DESC
        """,
    )
    fun observeInProgress(userId: String): Flow<List<EventEntity>>

    /** Finished matches, newest first. Drives "Recent matches". */
    @Query(
        """
        SELECT * FROM events
        WHERE userId = :userId AND status = 'COMPLETED' AND deletedAt IS NULL
        ORDER BY completedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentCompleted(userId: String, limit: Int): Flow<List<EventEntity>>

    /** Hard delete. Owned rows (participants, teams, subjects, rounds, scores) cascade. */
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    // Line-up rows use plain insert/update: @Upsert would turn a unique-index conflict into a
    // silent no-op update.

    @Insert
    suspend fun insertParticipants(participants: List<EventParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: EventParticipantEntity)

    @Query("SELECT * FROM event_participants WHERE eventId = :eventId ORDER BY sortOrder")
    suspend fun getParticipants(eventId: String): List<EventParticipantEntity>

    @Insert
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Query("SELECT * FROM teams WHERE eventId = :eventId ORDER BY sortOrder")
    suspend fun getTeams(eventId: String): List<TeamEntity>

    @Insert
    suspend fun insertTeamMembers(members: List<TeamMemberEntity>)

    @Insert
    suspend fun insertSubjects(subjects: List<ScoringSubjectEntity>)

    @Update
    suspend fun updateSubjects(subjects: List<ScoringSubjectEntity>)

    @Query("SELECT * FROM scoring_subjects WHERE eventId = :eventId ORDER BY sortOrder")
    fun observeSubjects(eventId: String): Flow<List<ScoringSubjectEntity>>
}

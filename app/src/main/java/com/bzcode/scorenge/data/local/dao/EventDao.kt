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

/** A finished match on Home's "Recent matches". */
data class RecentMatchRow(
    val eventId: String,
    val title: String,
    val completedAt: Long?,
    val gameIcon: String?,
    val winners: String?,
)

/** One player's result in one finished match; see [EventDao.observeFinishes]. */
data class FinishRow(
    val playerId: String,
    val playerName: String,
    val eventId: String,
    val gameId: String,
    val gameTitle: String,
    val gameIcon: String?,
    val finalRank: Int?,
)

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

    /** Finished matches with their game icon and winners ("Maya, Leo" on a tie), newest first. */
    @Query(
        """
        SELECT e.id AS eventId, e.title AS title, e.completedAt AS completedAt, g.icon AS gameIcon,
            (SELECT GROUP_CONCAT(s.name, ', ') FROM scoring_subjects s
             WHERE s.eventId = e.id AND s.finalRank = 1) AS winners
        FROM events e
        LEFT JOIN games g ON g.id = e.gameId
        WHERE e.userId = :userId AND e.status = 'COMPLETED' AND e.deletedAt IS NULL
        ORDER BY e.completedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentFinished(userId: String, limit: Int): Flow<List<RecentMatchRow>>

    /**
     * Each player's finish in each finished match. Team matches give every team member the
     * team's rank. Names come from the player list when the player still exists.
     */
    @Query(
        """
        SELECT p.playerId AS playerId, COALESCE(pl.name, p.playerName) AS playerName,
            e.id AS eventId, e.gameId AS gameId, COALESCE(g.title, '') AS gameTitle, g.icon AS gameIcon,
            s.finalRank AS finalRank
        FROM events e
        INNER JOIN scoring_subjects s ON s.eventId = e.id
        LEFT JOIN team_members tm ON tm.teamId = s.teamId
        INNER JOIN event_participants p ON p.id = COALESCE(s.participantId, tm.participantId)
        LEFT JOIN players pl ON pl.id = p.playerId
        LEFT JOIN games g ON g.id = e.gameId
        WHERE e.userId = :userId AND e.status = 'COMPLETED' AND e.deletedAt IS NULL
        ORDER BY e.completedAt
        """,
    )
    fun observeFinishes(userId: String): Flow<List<FinishRow>>

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
    suspend fun getSubjects(eventId: String): List<ScoringSubjectEntity>

    @Query("SELECT * FROM scoring_subjects WHERE eventId = :eventId ORDER BY sortOrder")
    fun observeSubjects(eventId: String): Flow<List<ScoringSubjectEntity>>
}

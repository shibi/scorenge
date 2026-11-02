package com.bzcode.scorenge.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.TeamEntity
import com.bzcode.scorenge.data.local.entity.TeamMemberEntity

/** Reads rows changed since the last upload, and writes rows restored from the cloud. */
@Dao
interface SyncDao {
    @Query("SELECT * FROM players WHERE userId = :uid AND updatedAt >= :since")
    suspend fun playersSince(uid: String, since: Long): List<PlayerEntity>

    @Query("SELECT * FROM games WHERE userId = :uid AND updatedAt >= :since")
    suspend fun gamesSince(uid: String, since: Long): List<GameEntity>

    @Query("SELECT * FROM events WHERE userId = :uid AND updatedAt >= :since")
    suspend fun eventsSince(uid: String, since: Long): List<EventEntity>

    @Query(
        """
        SELECT x.* FROM event_participants x INNER JOIN events e ON e.id = x.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun participantsSince(uid: String, since: Long): List<EventParticipantEntity>

    @Query(
        """
        SELECT x.* FROM teams x INNER JOIN events e ON e.id = x.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun teamsSince(uid: String, since: Long): List<TeamEntity>

    @Query(
        """
        SELECT x.* FROM team_members x
        INNER JOIN teams t ON t.id = x.teamId
        INNER JOIN events e ON e.id = t.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun teamMembersSince(uid: String, since: Long): List<TeamMemberEntity>

    @Query(
        """
        SELECT x.* FROM scoring_subjects x INNER JOIN events e ON e.id = x.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun subjectsSince(uid: String, since: Long): List<ScoringSubjectEntity>

    @Query(
        """
        SELECT x.* FROM rounds x INNER JOIN events e ON e.id = x.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun roundsSince(uid: String, since: Long): List<RoundEntity>

    @Query(
        """
        SELECT x.* FROM round_scores x
        INNER JOIN rounds r ON r.id = x.roundId
        INNER JOIN events e ON e.id = r.eventId
        WHERE e.userId = :uid AND x.updatedAt >= :since
        """,
    )
    suspend fun scoresSince(uid: String, since: Long): List<RoundScoreEntity>

    /** True when the user has any players, games or matches on this device. */
    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM players WHERE userId = :uid)
            OR EXISTS(SELECT 1 FROM games WHERE userId = :uid)
            OR EXISTS(SELECT 1 FROM events WHERE userId = :uid)
        """,
    )
    suspend fun hasLocalData(uid: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE id = :id)")
    suspend fun eventExists(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM teams WHERE id = :id)")
    suspend fun teamExists(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM rounds WHERE id = :id)")
    suspend fun roundExists(id: String): Boolean

    @Upsert suspend fun upsertPlayer(row: PlayerEntity)
    @Upsert suspend fun upsertGame(row: GameEntity)
    @Upsert suspend fun upsertEvent(row: EventEntity)
    @Upsert suspend fun upsertParticipant(row: EventParticipantEntity)
    @Upsert suspend fun upsertTeam(row: TeamEntity)
    @Upsert suspend fun upsertTeamMember(row: TeamMemberEntity)
    @Upsert suspend fun upsertSubject(row: ScoringSubjectEntity)
    @Upsert suspend fun upsertRound(row: RoundEntity)
    @Upsert suspend fun upsertScore(row: RoundScoreEntity)
}

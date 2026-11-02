package com.bzcode.scorenge.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.domain.model.ScoringSubjectType

/*
 * Room schema v1. Room is the local source of truth; Firestore holds a copy (step 7).
 *
 * Conventions:
 * - Ids are client-generated UUID strings, so rows can be synced without remapping.
 * - Times are epoch milliseconds (UTC).
 * - updatedAt is bumped on every write (last write wins when syncing).
 * - deletedAt marks a soft delete on the synced top-level rows (players, games, events).
 * - Rows owned by a match (participants, teams, subjects, rounds, scores) cascade from it.
 *   References to players and games are RESTRICT, so history can't lose its player or game.
 * - The UI says "match"; the data model keeps the spec's name, "event".
 */

/** The signed-in scorekeeper. [id] is the Firebase Auth uid. */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A person the scorekeeper plays with: just a name, not an account. */
@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("userId")],
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A game type such as Carrom or Chess, with the defaults new matches start from. */
@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("userId")],
)
data class GameEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    /** Optional emoji or icon key shown on game cards. */
    val icon: String? = null,
    val scoringDirection: ScoringDirection,
    val defaultScoringMode: ScoringMode,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * A match ("event" in the data model). Scoring settings are copied from the game at creation and
 * lock once the match starts. [totalRounds] is set only for [RoundMode.FIXED].
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("userId"), Index("gameId"), Index("status"), Index("completedAt")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val gameId: String,
    val title: String,
    val scoringMode: ScoringMode,
    val scoringDirection: ScoringDirection,
    val roundMode: RoundMode,
    val totalRounds: Int?,
    val status: EventStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    /** Last time a score was saved or a round opened; orders "Continue match" and recent lists. */
    val lastPlayedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A player taking part in a match. [playerName] is a snapshot so history keeps the name used then. */
@Entity(
    tableName = "event_participants",
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["eventId", "playerId"], unique = true), Index("playerId")],
)
data class EventParticipantEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val playerId: String,
    val playerName: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A team within one match (team mode only). */
@Entity(
    tableName = "teams",
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("eventId")],
)
data class TeamEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Puts a participant on a team. A participant is on at most one team. */
@Entity(
    tableName = "team_members",
    foreignKeys = [
        ForeignKey(entity = TeamEntity::class, parentColumns = ["id"], childColumns = ["teamId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventParticipantEntity::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("teamId"), Index(value = ["participantId"], unique = true)],
)
data class TeamMemberEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val participantId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * What receives scores: one participant (individual mode) or one team (team mode).
 * [finalRank] and [finalTotal] are stored when the match finishes, so stats need no recomputing.
 */
@Entity(
    tableName = "scoring_subjects",
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventParticipantEntity::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TeamEntity::class, parentColumns = ["id"], childColumns = ["teamId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("eventId"), Index("participantId"), Index("teamId")],
)
data class ScoringSubjectEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val type: ScoringSubjectType,
    val participantId: String?,
    val teamId: String?,
    val name: String,
    val sortOrder: Int,
    val finalRank: Int? = null,
    val finalTotal: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * One round of a match. Completing round N creates N+1 as PENDING; opening it makes it ACTIVE and
 * sets [startedAt]. Elapsed time pauses while the round page is closed: [elapsedMillis] holds the
 * saved total and [openedAt] the time the page was opened (null while closed). See RoundTimer.
 */
@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["eventId", "roundNumber"], unique = true), Index("status")],
)
data class RoundEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val roundNumber: Int,
    val status: RoundStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val elapsedMillis: Long = 0L,
    val openedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A subject's score in a round. A null [score] means "not entered yet", which differs from 0. */
@Entity(
    tableName = "round_scores",
    foreignKeys = [
        ForeignKey(entity = RoundEntity::class, parentColumns = ["id"], childColumns = ["roundId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ScoringSubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["roundId", "subjectId"], unique = true), Index("subjectId")],
)
data class RoundScoreEntity(
    @PrimaryKey val id: String,
    val roundId: String,
    val subjectId: String,
    val score: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

package com.bzcode.scorenge.data.sync

import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.TeamEntity
import com.bzcode.scorenge.data.local.entity.TeamMemberEntity
import com.bzcode.scorenge.domain.model.EventStatus
import com.bzcode.scorenge.domain.model.RoundMode
import com.bzcode.scorenge.domain.model.RoundStatus
import com.bzcode.scorenge.domain.model.ScoreEntry
import com.bzcode.scorenge.domain.model.ScoringDirection
import com.bzcode.scorenge.domain.model.ScoringMode
import com.bzcode.scorenge.domain.model.ScoringSubjectType

/** A document as stored in Firestore: plain strings, numbers and nulls. */
typealias Doc = Map<String, Any?>

/**
 * One synced table. Firestore keeps each table as `users/{uid}/{name}/{id}`, one document per row,
 * with the same field names as the Room columns. Listed parents first, so restoring in this order
 * never inserts a row before the row it points to.
 */
sealed class SyncCollection<T : Any>(val name: String) {
    abstract fun id(row: T): String
    abstract fun updatedAt(row: T): Long
    abstract fun toDoc(row: T): Doc
    abstract fun fromDoc(doc: Doc): T

    data object Players : SyncCollection<PlayerEntity>("players") {
        override fun id(row: PlayerEntity) = row.id
        override fun updatedAt(row: PlayerEntity) = row.updatedAt
        override fun toDoc(row: PlayerEntity): Doc = mapOf(
            "id" to row.id, "userId" to row.userId, "name" to row.name,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt, "deletedAt" to row.deletedAt,
        )
        override fun fromDoc(doc: Doc) = PlayerEntity(
            id = doc.str("id"), userId = doc.str("userId"), name = doc.str("name"),
            createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"), deletedAt = doc.longOrNull("deletedAt"),
        )
    }

    data object Games : SyncCollection<GameEntity>("games") {
        override fun id(row: GameEntity) = row.id
        override fun updatedAt(row: GameEntity) = row.updatedAt
        override fun toDoc(row: GameEntity): Doc = mapOf(
            "id" to row.id, "userId" to row.userId, "title" to row.title, "description" to row.description,
            "icon" to row.icon, "scoringDirection" to row.scoringDirection.name,
            "defaultScoringMode" to row.defaultScoringMode.name,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt, "deletedAt" to row.deletedAt,
        )
        override fun fromDoc(doc: Doc) = GameEntity(
            id = doc.str("id"), userId = doc.str("userId"), title = doc.str("title"),
            description = doc.strOrNull("description"), icon = doc.strOrNull("icon"),
            scoringDirection = ScoringDirection.valueOf(doc.str("scoringDirection")),
            defaultScoringMode = ScoringMode.valueOf(doc.str("defaultScoringMode")),
            createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"), deletedAt = doc.longOrNull("deletedAt"),
        )
    }

    data object Events : SyncCollection<EventEntity>("events") {
        override fun id(row: EventEntity) = row.id
        override fun updatedAt(row: EventEntity) = row.updatedAt
        override fun toDoc(row: EventEntity): Doc = mapOf(
            "id" to row.id, "userId" to row.userId, "gameId" to row.gameId, "title" to row.title,
            "scoringMode" to row.scoringMode.name, "scoringDirection" to row.scoringDirection.name,
            "roundMode" to row.roundMode.name, "totalRounds" to row.totalRounds, "status" to row.status.name,
            "startedAt" to row.startedAt, "completedAt" to row.completedAt, "lastPlayedAt" to row.lastPlayedAt,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt, "deletedAt" to row.deletedAt,
            "scoreEntry" to row.scoreEntry.name,
        )
        override fun fromDoc(doc: Doc) = EventEntity(
            id = doc.str("id"), userId = doc.str("userId"), gameId = doc.str("gameId"), title = doc.str("title"),
            scoringMode = ScoringMode.valueOf(doc.str("scoringMode")),
            scoringDirection = ScoringDirection.valueOf(doc.str("scoringDirection")),
            roundMode = RoundMode.valueOf(doc.str("roundMode")),
            totalRounds = doc.longOrNull("totalRounds")?.toInt(),
            status = EventStatus.valueOf(doc.str("status")),
            startedAt = doc.longOrNull("startedAt"), completedAt = doc.longOrNull("completedAt"),
            lastPlayedAt = doc.longOrNull("lastPlayedAt"),
            createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"), deletedAt = doc.longOrNull("deletedAt"),
            // Backups made before finishing-order scoring have no scoreEntry.
            scoreEntry = doc.strOrNull("scoreEntry")?.let(ScoreEntry::valueOf) ?: ScoreEntry.POINTS,
        )
    }

    data object Participants : SyncCollection<EventParticipantEntity>("event_participants") {
        override fun id(row: EventParticipantEntity) = row.id
        override fun updatedAt(row: EventParticipantEntity) = row.updatedAt
        override fun toDoc(row: EventParticipantEntity): Doc = mapOf(
            "id" to row.id, "eventId" to row.eventId, "playerId" to row.playerId, "playerName" to row.playerName,
            "sortOrder" to row.sortOrder, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )
        override fun fromDoc(doc: Doc) = EventParticipantEntity(
            id = doc.str("id"), eventId = doc.str("eventId"), playerId = doc.str("playerId"),
            playerName = doc.str("playerName"), sortOrder = doc.long("sortOrder").toInt(),
            createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    data object Teams : SyncCollection<TeamEntity>("teams") {
        override fun id(row: TeamEntity) = row.id
        override fun updatedAt(row: TeamEntity) = row.updatedAt
        override fun toDoc(row: TeamEntity): Doc = mapOf(
            "id" to row.id, "eventId" to row.eventId, "name" to row.name, "sortOrder" to row.sortOrder,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )
        override fun fromDoc(doc: Doc) = TeamEntity(
            id = doc.str("id"), eventId = doc.str("eventId"), name = doc.str("name"),
            sortOrder = doc.long("sortOrder").toInt(), createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    data object TeamMembers : SyncCollection<TeamMemberEntity>("team_members") {
        override fun id(row: TeamMemberEntity) = row.id
        override fun updatedAt(row: TeamMemberEntity) = row.updatedAt
        override fun toDoc(row: TeamMemberEntity): Doc = mapOf(
            "id" to row.id, "teamId" to row.teamId, "participantId" to row.participantId,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )
        override fun fromDoc(doc: Doc) = TeamMemberEntity(
            id = doc.str("id"), teamId = doc.str("teamId"), participantId = doc.str("participantId"),
            createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    data object Subjects : SyncCollection<ScoringSubjectEntity>("scoring_subjects") {
        override fun id(row: ScoringSubjectEntity) = row.id
        override fun updatedAt(row: ScoringSubjectEntity) = row.updatedAt
        override fun toDoc(row: ScoringSubjectEntity): Doc = mapOf(
            "id" to row.id, "eventId" to row.eventId, "type" to row.type.name, "participantId" to row.participantId,
            "teamId" to row.teamId, "name" to row.name, "sortOrder" to row.sortOrder, "finalRank" to row.finalRank,
            "finalTotal" to row.finalTotal, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )
        override fun fromDoc(doc: Doc) = ScoringSubjectEntity(
            id = doc.str("id"), eventId = doc.str("eventId"), type = ScoringSubjectType.valueOf(doc.str("type")),
            participantId = doc.strOrNull("participantId"), teamId = doc.strOrNull("teamId"), name = doc.str("name"),
            sortOrder = doc.long("sortOrder").toInt(), finalRank = doc.longOrNull("finalRank")?.toInt(),
            finalTotal = doc.longOrNull("finalTotal"), createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    data object Rounds : SyncCollection<RoundEntity>("rounds") {
        override fun id(row: RoundEntity) = row.id
        override fun updatedAt(row: RoundEntity) = row.updatedAt
        override fun toDoc(row: RoundEntity): Doc = mapOf(
            "id" to row.id, "eventId" to row.eventId, "roundNumber" to row.roundNumber, "status" to row.status.name,
            "startedAt" to row.startedAt, "completedAt" to row.completedAt, "elapsedMillis" to row.elapsedMillis,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )

        /** A round that was open when backed up comes back closed, so time away doesn't count. */
        override fun fromDoc(doc: Doc) = RoundEntity(
            id = doc.str("id"), eventId = doc.str("eventId"), roundNumber = doc.long("roundNumber").toInt(),
            status = RoundStatus.valueOf(doc.str("status")), startedAt = doc.longOrNull("startedAt"),
            completedAt = doc.longOrNull("completedAt"), elapsedMillis = doc.longOrNull("elapsedMillis") ?: 0L,
            openedAt = null, createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    data object Scores : SyncCollection<RoundScoreEntity>("round_scores") {
        override fun id(row: RoundScoreEntity) = row.id
        override fun updatedAt(row: RoundScoreEntity) = row.updatedAt
        override fun toDoc(row: RoundScoreEntity): Doc = mapOf(
            "id" to row.id, "roundId" to row.roundId, "subjectId" to row.subjectId, "score" to row.score,
            "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        )
        override fun fromDoc(doc: Doc) = RoundScoreEntity(
            id = doc.str("id"), roundId = doc.str("roundId"), subjectId = doc.str("subjectId"),
            score = doc.longOrNull("score"), createdAt = doc.long("createdAt"), updatedAt = doc.long("updatedAt"),
        )
    }

    companion object {
        /**
         * Parents before children. Lazy: the subclasses initialise this class first, so an eager list
         * built while one of them is still initialising would hold null for it.
         */
        val all: List<SyncCollection<*>> by lazy {
            listOf(Players, Games, Events, Participants, Teams, TeamMembers, Subjects, Rounds, Scores)
        }

        fun byName(name: String): SyncCollection<*>? = all.firstOrNull { it.name == name }
    }
}

private fun Doc.str(key: String): String = requireNotNull(this[key] as? String) { "Missing $key" }
private fun Doc.strOrNull(key: String): String? = this[key] as? String
private fun Doc.long(key: String): Long = requireNotNull(longOrNull(key)) { "Missing $key" }
private fun Doc.longOrNull(key: String): Long? = (this[key] as? Number)?.toLong()

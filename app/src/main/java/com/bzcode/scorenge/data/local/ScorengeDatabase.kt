package com.bzcode.scorenge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bzcode.scorenge.data.local.dao.EventDao
import com.bzcode.scorenge.data.local.dao.GameDao
import com.bzcode.scorenge.data.local.dao.PlayerDao
import com.bzcode.scorenge.data.local.dao.RoundDao
import com.bzcode.scorenge.data.local.dao.UserDao
import com.bzcode.scorenge.data.local.entity.EventEntity
import com.bzcode.scorenge.data.local.entity.EventParticipantEntity
import com.bzcode.scorenge.data.local.entity.GameEntity
import com.bzcode.scorenge.data.local.entity.PlayerEntity
import com.bzcode.scorenge.data.local.entity.RoundEntity
import com.bzcode.scorenge.data.local.entity.RoundScoreEntity
import com.bzcode.scorenge.data.local.entity.ScoringSubjectEntity
import com.bzcode.scorenge.data.local.entity.TeamEntity
import com.bzcode.scorenge.data.local.entity.TeamMemberEntity
import com.bzcode.scorenge.data.local.entity.UserEntity

/** Schema JSON is exported to app/schemas so later versions can be migration-tested against v1. */
@Database(
    entities = [
        UserEntity::class,
        PlayerEntity::class,
        GameEntity::class,
        EventEntity::class,
        EventParticipantEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        ScoringSubjectEntity::class,
        RoundEntity::class,
        RoundScoreEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ScorengeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun eventDao(): EventDao
    abstract fun roundDao(): RoundDao

    abstract fun syncDao(): com.bzcode.scorenge.data.local.dao.SyncDao

    companion object {
        const val NAME = "scorenge.db"
    }
}

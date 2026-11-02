package com.bzcode.scorenge.di

import android.content.Context
import androidx.room.Room
import com.bzcode.scorenge.data.local.ScorengeDatabase
import com.bzcode.scorenge.data.local.dao.EventDao
import com.bzcode.scorenge.data.local.dao.GameDao
import com.bzcode.scorenge.data.local.dao.PlayerDao
import com.bzcode.scorenge.data.local.dao.RoundDao
import com.bzcode.scorenge.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScorengeDatabase =
        Room.databaseBuilder(context, ScorengeDatabase::class.java, ScorengeDatabase.NAME)
            .addMigrations(ScorengeDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideUserDao(db: ScorengeDatabase): UserDao = db.userDao()
    @Provides fun providePlayerDao(db: ScorengeDatabase): PlayerDao = db.playerDao()
    @Provides fun provideGameDao(db: ScorengeDatabase): GameDao = db.gameDao()
    @Provides fun provideEventDao(db: ScorengeDatabase): EventDao = db.eventDao()
    @Provides fun provideRoundDao(db: ScorengeDatabase): RoundDao = db.roundDao()
}

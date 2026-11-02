package com.bzcode.scorenge.di

import com.bzcode.scorenge.data.sync.DeletionLog
import com.bzcode.scorenge.data.sync.FirestoreRemoteStore
import com.bzcode.scorenge.data.sync.PrefsDeletionLog
import com.bzcode.scorenge.data.sync.RemoteStore
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindRemoteStore(impl: FirestoreRemoteStore): RemoteStore

    @Binds
    abstract fun bindDeletionLog(impl: PrefsDeletionLog): DeletionLog

    companion object {
        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    }
}

package com.bzcode.scorenge.di

import com.bzcode.scorenge.data.auth.AuthRepository
import com.bzcode.scorenge.data.auth.CredentialManagerGoogleIdTokenProvider
import com.bzcode.scorenge.data.auth.FirebaseAuthRepository
import com.bzcode.scorenge.data.auth.GoogleIdTokenProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindGoogleIdTokenProvider(impl: CredentialManagerGoogleIdTokenProvider): GoogleIdTokenProvider

    companion object {
        @Provides
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}

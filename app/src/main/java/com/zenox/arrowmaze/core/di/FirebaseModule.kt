package com.zenox.arrowmaze.core.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import com.zenox.arrowmaze.core.firebase.auth.FirebaseAuthImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that exposes the Firebase SDK singletons and binds
 * [ArrowMazeAuth] → [FirebaseAuthImpl].
 *
 * The Firebase SDK initialises itself via the `FirebaseInitProvider`
 * content provider that the `firebase-common` manifest registers, so each
 * `getInstance()` call here is a no-arg singleton lookup — no [Context]
 * is required.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
}

/**
 * Binds the [ArrowMazeAuth] interface to its [FirebaseAuthImpl] concrete
 * implementation. Kept as a separate abstract class because [FirebaseModule]
 * is an `object` (it uses `@Provides`); `@Binds` requires an abstract class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindModule {

    @Binds
    @Singleton
    abstract fun bindArrowMazeAuth(impl: FirebaseAuthImpl): ArrowMazeAuth
}

package com.zenox.arrowmaze.core.firebase.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.zenox.arrowmaze.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that exposes the [FirebaseCrashlytics] singleton + the
 * collection-enabled flag set at app start.
 *
 * [CrashlyticsManager] and [CrashlyticsTree] are `@Inject constructor`-annotated
 * `@Singleton` classes — Hilt auto-discovers them — so this module only
 * needs to provide the underlying SDK singleton (which has no public
 * constructor).
 *
 * The `@Provides` for [FirebaseCrashlytics] also configures collection
 * enablement: `false` in debug (Timber.DebugTree handles logging instead),
 * `true` in release (CrashlyticsTree forwards breadcrumbs + records
 * non-fatals).
 */
@Module
@InstallIn(SingletonComponent::class)
object CrashlyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics {
        val instance = FirebaseCrashlytics.getInstance()
        // Collection is disabled by default in debug so dev crashes don't
        // pollute the Crashlytics dashboard. Release builds enable it so
        // real-world crashes surface in the console.
        instance.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        return instance
    }
}

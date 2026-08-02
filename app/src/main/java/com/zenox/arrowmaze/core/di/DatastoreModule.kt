package com.zenox.arrowmaze.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.datastore.ProgressDataStore
import com.zenox.arrowmaze.core.datastore.SessionDataStore
import com.zenox.arrowmaze.core.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for the three `DataStore<Preferences>` instances + their wrapper
 * classes. Each DataStore is created via a top-level `preferencesDataStore`
 * delegate (one per name) so that the singleton owner is the delegate itself,
 * not the Hilt component — this matches AndroidX's recommended pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    /** Qualifier for the settings DataStore. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class SettingsDataStoreQualifier

    /** Qualifier for the session DataStore. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class SessionDataStoreQualifier

    /** Qualifier for the progress DataStore. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ProgressDataStoreQualifier

    // Top-level delegates (one per DataStore name). Each delegate is a
    // singleton per-process; the Hilt provider below returns the same
    // DataStore instance via the delegate's `getValue(Context)` operator.
    private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = AppConstants.SETTINGS_DATASTORE,
    )
    private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
        name = AppConstants.SESSION_DATASTORE,
    )
    private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
        name = AppConstants.PROGRESS_DATASTORE,
    )

    @Provides
    @Singleton
    @SettingsDataStoreQualifier
    fun provideSettingsPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    @SessionDataStoreQualifier
    fun provideSessionPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.sessionDataStore

    @Provides
    @Singleton
    @ProgressDataStoreQualifier
    fun provideProgressPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.progressDataStore

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @SettingsDataStoreQualifier ds: DataStore<Preferences>,
    ): SettingsDataStore = SettingsDataStore(ds)

    @Provides
    @Singleton
    fun provideSessionDataStore(
        @SessionDataStoreQualifier ds: DataStore<Preferences>,
    ): SessionDataStore = SessionDataStore(ds)

    @Provides
    @Singleton
    fun provideProgressDataStore(
        @ProgressDataStoreQualifier ds: DataStore<Preferences>,
    ): ProgressDataStore = ProgressDataStore(ds)
}

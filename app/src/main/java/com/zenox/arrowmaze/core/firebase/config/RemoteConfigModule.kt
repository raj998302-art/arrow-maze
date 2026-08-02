package com.zenox.arrowmaze.core.firebase.config

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the Remote Config graph.
 *
 * [RemoteConfigManager] is `@Inject constructor`-able `@Singleton` — Hilt
 * auto-builds it once [com.google.firebase.remoteconfig.FirebaseRemoteConfig]
 * is provided by [com.zenox.arrowmaze.core.di.FirebaseModule].
 *
 * This module is kept as a single point of documentation for the Remote
 * Config dependency graph and as a future extension point (e.g. for binding
 * fakes in instrumented tests via `@TestInstallIn`).
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule

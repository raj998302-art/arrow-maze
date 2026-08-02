package com.zenox.arrowmaze.core.ads

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the AdMob graph.
 *
 * All four managers ([AdMobInitializer], [AdMobManager], [AppOpenAdManager],
 * [ConsentManager]) are `@Inject constructor`-able `@Singleton` classes — Hilt
 * auto-builds them without `@Provides`. This module is kept as a single point
 * of documentation for the ads dependency graph and as a future extension
 * point (e.g. for binding fakes in instrumented tests via `@TestInstallIn`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AdMobModule

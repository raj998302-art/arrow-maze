package com.zenox.arrowmaze.core.audio

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the audio graph.
 *
 * [AudioManager] is `@Inject constructor`-able `@Singleton` — Hilt auto-builds
 * it without `@Provides`. This module is kept as a single point of documentation
 * for the audio dependency graph and as a future extension point (e.g. for
 * binding fakes in instrumented tests via `@TestInstallIn`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule

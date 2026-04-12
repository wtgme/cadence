package io.cadence.music.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.cadence.music.data.api.GenerationBackend
import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.MusicRepository
import io.cadence.music.data.api.SongGenerationBackend
import io.cadence.music.domain.LLMParamsBuilder
import io.cadence.music.domain.ParamsBuilder
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(impl: MusicRepository): GenerationRepository

    /**
     * Default params strategy: OpenRouter LLM called once per session.
     * Falls back to rule-based params automatically if OpenRouter is unavailable.
     */
    @Binds
    @Singleton
    abstract fun bindParamsBuilder(impl: LLMParamsBuilder): ParamsBuilder

    /** Default generation backend: SongGeneration v2-large. */
    @Binds
    @Singleton
    abstract fun bindGenerationBackend(impl: SongGenerationBackend): GenerationBackend
}

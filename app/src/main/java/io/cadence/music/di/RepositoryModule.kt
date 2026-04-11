package io.cadence.music.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.MusicRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(impl: MusicRepository): GenerationRepository
}

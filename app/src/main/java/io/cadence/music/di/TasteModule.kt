package io.cadence.music.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.cadence.music.data.taste.TasteMemoryRepository
import io.cadence.music.data.taste.TasteMemoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TasteModule {

    @Binds
    @Singleton
    abstract fun bindTasteMemoryRepository(impl: TasteMemoryRepositoryImpl): TasteMemoryRepository
}

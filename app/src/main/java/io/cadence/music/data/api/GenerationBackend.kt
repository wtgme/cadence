package io.cadence.music.data.api

import kotlinx.coroutines.flow.Flow

interface GenerationBackend {
    val name: String
    suspend fun generate(params: SongParams): GenerationResult
    fun generateStream(params: SongParams): Flow<StreamingChunk>
    suspend fun healthCheck(): Boolean
}

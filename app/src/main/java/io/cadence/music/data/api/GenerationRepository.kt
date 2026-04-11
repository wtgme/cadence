package io.cadence.music.data.api

import java.io.File

sealed class GenerationResult {
    data class Success(val audioFile: File, val params: SongParams) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}

interface GenerationRepository {
    suspend fun generateClip(metricsContext: String): GenerationResult
}

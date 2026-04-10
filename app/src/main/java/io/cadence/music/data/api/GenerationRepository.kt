package io.cadence.music.data.api

interface GenerationRepository {
    suspend fun generateClip(metricsContext: String): GenerationResult
}

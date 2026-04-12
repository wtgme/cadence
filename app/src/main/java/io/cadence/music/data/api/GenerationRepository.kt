package io.cadence.music.data.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed class GenerationResult {
    data class Success(val audioFile: File, val params: SongParams) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}

interface GenerationRepository {
    /**
     * Params emitted as soon as Step 1 (OpenRouter) completes, before the much slower
     * Step 2 (audio rendering). UI observers can show style/lyrics while audio renders.
     */
    val translatedSongParams: StateFlow<SongParams?>

    /** Step 1 — OpenRouter prompt translation. Always returns usable params (falls back on error). */
    suspend fun translateMetrics(metricsContext: String): SongParams

    /**
     * Step 2 — Audio generation via the active [GenerationBackend].
     *
     * Emits one [StreamingChunk.Audio] (a complete MP3 file) followed by
     * [StreamingChunk.Complete]. The flow interface is designed for future backends
     * that may genuinely stream incremental chunks.
     *
     * Cancelling the collecting coroutine aborts the underlying HTTP call.
     */
    fun generateAudioStream(params: SongParams): Flow<StreamingChunk>
}

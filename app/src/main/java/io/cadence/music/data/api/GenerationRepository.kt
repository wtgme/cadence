package io.cadence.music.data.api

import io.cadence.music.data.model.MentalState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed class GenerationResult {
    data class Success(val audioFile: File, val params: SongParams) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}

interface GenerationRepository {
    /**
     * Params emitted as soon as Step 1 (Signal2Style) completes, before the much slower
     * Step 2 (audio rendering). UI observers can show style/lyrics while audio renders.
     */
    val translatedSongParams: StateFlow<SongParams?>

    /**
     * Mental state estimated by Step 1a (psychophysiologist LLM), populated before
     * [translatedSongParams]. Null until the first successful estimation.
     */
    val translatedMentalState: StateFlow<MentalState?>

    /** Step 1 (full) — Biometric context → MentalState → SongParams. Always returns usable params. */
    suspend fun translateMetrics(metricsContext: String): SongParams

    /**
     * Step 1b only — derives [SongParams] from an already-estimated [MentalState].
     * Called for every song within a session so taste memory and user adjustments are
     * reflected. [previousParams] is passed so the model can vary the style rather than
     * repeating the same genre/instrument combination.
     * Returns null if Step 1b fails — caller should fall back to [translateMetrics].
     */
    suspend fun translateMentalState(mentalState: MentalState, previousParams: SongParams? = null): SongParams?

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

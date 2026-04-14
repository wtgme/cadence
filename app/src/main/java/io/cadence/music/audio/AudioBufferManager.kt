package io.cadence.music.audio

import android.util.Log
import io.cadence.music.data.adjustment.UserAdjustmentRepository
import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.api.StreamingChunk
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.domain.ParamsBuilder
import io.cadence.music.domain.PromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a pre-generated buffer of audio files.
 *
 * Each song request calls [GenerationRepository.generateAudioStream], which emits a
 * single [StreamingChunk.Audio] (one complete MP3) followed by [StreamingChunk.Complete].
 * The interface is designed for future streaming backends that may emit multiple chunks.
 *
 * After each song completes the worker automatically triggers the next request,
 * creating a self-sustaining loop that keeps the queue ahead of playback.
 *
 * Context changes (scene shift, HR drift) call [drainAndReprime], which cancels the
 * in-flight request, flushes stale chunks, and restarts — reusing the session [SongParams].
 * Only [prime] (called on explicit start) clears the cached params to re-query OpenRouter.
 */
@Singleton
class AudioBufferManager @Inject constructor(
    private val musicRepository: GenerationRepository,
    private val paramsBuilder: ParamsBuilder,
    private val promptBuilder: PromptBuilder,
    private val userAdjustmentRepository: UserAdjustmentRepository,
) {
    // Holds pre-generated chunk files ready for ExoPlayer. Sized to buffer ~2 full songs
    // (each 90 s song produces ~3 chunks).
    private val queue = Channel<File>(capacity = 8)
    private val requestChannel = Channel<Unit>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var currentSensorState: SensorState = SensorState()
    @Volatile private var currentScene: Scene? = null

    @Volatile private var generationEpoch = 0L

    /**
     * Mental state estimated at session start (Step 1a). Set once by [prime]; cleared on
     * [cancelGeneration] or the next [prime]. Survives [drainAndReprime] and
     * [applyUserAdjustment] — the user's physiology doesn't change within a session.
     */
    @Volatile private var sessionMentalState: MentalState? = null

    /**
     * Params fetched from OpenRouter at the start of a session (or after a user adjustment).
     * Set to null by [prime] and [applyUserAdjustment] so the next generation re-queries
     * OpenRouter. NOT cleared by [drainAndReprime] — context shifts mid-session reuse the
     * same style rather than burning another OpenRouter call.
     */
    @Volatile private var sessionParams: SongParams? = null

    /**
     * Params of the last completed song. Passed to Step 1b so the model can vary the style
     * rather than repeating the same genre/instrument combination. Cleared on [prime] so
     * a fresh session doesn't carry over style from a previous one.
     */
    @Volatile private var previousSongParams: SongParams? = null

    /** Job for the currently active streaming request — cancelled by [drainAndReprime]. */
    @Volatile private var activeStreamingJob: Job? = null

    private val _chunksReady = MutableStateFlow(0)
    val chunksReady: StateFlow<Int> = _chunksReady

    private val _currentMetricsContext = MutableStateFlow("")
    val currentMetricsContext: StateFlow<String> = _currentMetricsContext

    private val _currentSongParams = MutableStateFlow<SongParams?>(null)
    val currentSongParams: StateFlow<SongParams?> = _currentSongParams

    private val _currentMentalState = MutableStateFlow<MentalState?>(null)
    val currentMentalState: StateFlow<MentalState?> = _currentMentalState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _songHistory = MutableStateFlow<List<GeneratedSong>>(emptyList())
    val songHistory: StateFlow<List<GeneratedSong>> = _songHistory

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress

    fun updateProgress(positionMs: Long, durationMs: Long) {
        _playbackProgress.value = PlaybackProgress(positionMs, durationMs)
    }

    init {
        startWorker()
    }

    private fun startWorker() {
        scope.launch {
            Log.d(TAG, "Worker: started, waiting for requests")
            for (request in requestChannel) {
                Log.d(TAG, "Worker: request picked up, epoch=$generationEpoch")
                val job = launch {
                    try {
                        processNextRequest()
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Worker: streaming job cancelled (epoch=$generationEpoch)")
                        throw e
                    } catch (e: Throwable) {
                        Log.e(TAG, "Worker: unexpected exception — keeping loop alive", e)
                        _lastError.value = e.message ?: "generation error"
                    }
                }
                activeStreamingJob = job
                job.join()
                Log.d(TAG, "Worker: job done, back to idle")
            }
            Log.w(TAG, "Worker loop exited — requestChannel closed")
        }
    }

    private suspend fun processNextRequest() {
        val myEpoch = generationEpoch
        val state = currentSensorState
        val scene = currentScene

        // Build metrics context for the Debug screen (instant — no network)
        _currentMetricsContext.value = promptBuilder.buildMetricsContext(state, scene)

        // Three-path param resolution:
        //   1. sessionParams != null → reuse (no network call; HR drift / scene change path)
        //   2. sessionParams == null, sessionMentalState != null → Step 1b only (user adjustment path)
        //   3. Both null → full re-query Step 1a + 1b (new session path)
        val params: SongParams = when {
            sessionParams != null -> sessionParams!!

            sessionMentalState != null -> {
                Log.d(TAG, "Worker: cached MentalState found — running Step 1b only")
                val rederived = musicRepository.translateMentalState(sessionMentalState!!, previousSongParams)
                if (rederived != null) {
                    sessionParams = rederived
                    Log.d(TAG, "Worker: Step 1b re-query done — descriptions=${rederived.descriptions}")
                    rederived
                } else {
                    Log.w(TAG, "Worker: Step 1b re-query failed — falling back to full re-query")
                    paramsBuilder.buildParams(state, scene).also {
                        sessionMentalState = musicRepository.translatedMentalState.value
                        sessionParams = it
                        Log.d(TAG, "Worker: full re-query fallback done — descriptions=${it.descriptions}")
                    }
                }
            }

            else -> {
                Log.d(TAG, "Worker: new session — running full re-query (Step 1a + 1b)")
                paramsBuilder.buildParams(state, scene).also {
                    sessionMentalState = musicRepository.translatedMentalState.value
                    sessionParams = it
                    Log.d(TAG, "Worker: OpenRouter params fetched — descriptions=${it.descriptions} type=${it.auto_prompt_audio_type}")
                }
            }
        }
        _currentSongParams.value = params
        _currentMentalState.value = musicRepository.translatedMentalState.value
        Log.d(TAG, "Worker: params ready — descriptions=${params.descriptions} type=${params.auto_prompt_audio_type} epoch=$myEpoch")

        if (myEpoch != generationEpoch) {
            Log.d(TAG, "Worker: epoch changed before streaming — discarding")
            return
        }

        var firstChunk = true
        musicRepository.generateAudioStream(params).collect { chunk ->
            when (chunk) {
                is StreamingChunk.Audio -> {
                    if (myEpoch != generationEpoch) {
                        chunk.file.delete()
                        Log.d(TAG, "Worker: stale chunk discarded (epoch mismatch)")
                        return@collect
                    }
                    if (firstChunk) {
                        firstChunk = false
                        recordSong(params)
                    }
                    Log.d(TAG, "Worker: queuing chunk ${chunk.index} (${chunk.file.name})")
                    queue.send(chunk.file)
                    _chunksReady.update { it + 1 }
                }
                is StreamingChunk.Error -> {
                    Log.e(TAG, "Worker: stream error — ${chunk.message}")
                    if (myEpoch == generationEpoch) _lastError.value = chunk.message
                }
                StreamingChunk.Complete -> {
                    Log.d(TAG, "Worker: stream complete — auto-triggering next song")
                    // Save the just-finished params for Step 1b variety, then clear
                    // sessionParams so the next song re-runs Step 1b with the latest
                    // taste memory. sessionMentalState is kept — Step 1a is not repeated.
                    if (myEpoch == generationEpoch) {
                        previousSongParams = params
                        sessionParams = null
                        requestChannel.trySend(Unit)
                    }
                }
            }
        }
    }

    private fun recordSong(params: SongParams) {
        val song = GeneratedSong(
            id          = System.currentTimeMillis(),
            params      = params,
            mentalState = _currentMentalState.value,
            scene       = currentScene,
            generatedAt = System.currentTimeMillis(),
        )
        _songHistory.update { history -> (listOf(song) + history).take(MAX_HISTORY) }
    }

    /**
     * Start a new playback session. Clears both the cached [MentalState] and [SongParams]
     * so the next generation runs the full Step 1a + 1b pipeline with fresh biometrics.
     * Does NOT bump the epoch.
     */
    fun prime(sensorState: SensorState, scene: Scene?) {
        sessionMentalState = null  // re-estimate mental state for the new session
        sessionParams = null       // force full OpenRouter re-query
        previousSongParams = null  // don't carry style history across sessions
        _currentMetricsContext.value = ""
        _currentSongParams.value = null
        _currentMentalState.value = null
        enqueueGeneration(sensorState, scene)
    }

    /**
     * Applies a user-initiated music adjustment (genre, energy, free text).
     * Clears [sessionParams] so the next generation re-queries OpenRouter with the updated
     * preference hint injected via [UserAdjustmentRepository]. After that one call the result
     * is cached as the new [sessionParams] for the rest of the session.
     */
    fun applyUserAdjustment(sensorState: SensorState, scene: Scene?) {
        sessionParams = null
        drainAndReprime(sensorState, scene)
    }

    /**
     * Cancel any in-flight generation, flush all queued chunks, and restart with new
     * context. Bumps the epoch so stale results are discarded.
     *
     * Does NOT clear [sessionParams] — mid-session context shifts (HR drift, scene change)
     * reuse the style established at session start rather than burning another OpenRouter call.
     */
    fun drainAndReprime(sensorState: SensorState, scene: Scene?) {
        activeStreamingJob?.cancel()
        generationEpoch++
        while (true) {
            val polled = queue.tryReceive()
            if (polled.isFailure) break
            polled.getOrNull()?.delete()
        }
        _lastError.value = null
        enqueueGeneration(sensorState, scene)
    }

    private fun enqueueGeneration(sensorState: SensorState, scene: Scene?) {
        currentSensorState = sensorState
        currentScene = scene
        _chunksReady.value = 0
        requestChannel.trySend(Unit)
    }

    suspend fun takeNext(): File? = try {
        queue.receive()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "takeNext failed", e)
        null
    }

    fun retryGeneration() {
        _lastError.value = null
        requestChannel.trySend(Unit)
    }

    /**
     * Called by [MusicPlayerService] when the user requested skip but no buffered song was
     * available. Resets [chunksReady] to 0 so the UI immediately shows the buffering state
     * rather than appearing stuck on the previous song.
     */
    fun notifySkipToNext() {
        _chunksReady.value = 0
    }

    /**
     * Cancels any in-flight generation request and flushes the queue.
     * Called when the user stops playback so the server request is aborted immediately
     * rather than running to completion in the background.
     */
    fun cancelGeneration() {
        activeStreamingJob?.cancel()
        generationEpoch++
        while (true) {
            val polled = queue.tryReceive()
            if (polled.isFailure) break
            polled.getOrNull()?.delete()
        }
        _chunksReady.value = 0
        _lastError.value = null
        sessionMentalState = null
        sessionParams = null
        previousSongParams = null
        Log.d(TAG, "Generation cancelled (epoch=$generationEpoch)")
    }

    fun updateSensorState(state: SensorState) {
        currentSensorState = state
    }

    fun updateScene(scene: Scene?) {
        currentScene = scene
    }

    companion object {
        private const val TAG = "AudioBufferManager"
        private const val MAX_HISTORY = 50
    }
}

package io.cadence.music.audio

import android.util.Log
import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.GenerationResult
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.domain.PromptBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a 2-item buffer of pre-generated audio clips using the two-step
 * generation chain (Gemini Flash-Lite → Gemini Lyria 3).
 */
@Singleton
class AudioBufferManager @Inject constructor(
    private val musicRepository: GenerationRepository,
    private val promptBuilder: PromptBuilder,
) {
    private val queue = Channel<File>(capacity = 2)
    private val requestChannel = Channel<Unit>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentSensorState: SensorState = SensorState()
    private var currentScene: Scene? = null

    @Volatile private var generationEpoch = 0L

    private val _chunksReady = MutableStateFlow(0)
    val chunksReady: StateFlow<Int> = _chunksReady

    private val _currentMetricsContext = MutableStateFlow("")
    val currentMetricsContext: StateFlow<String> = _currentMetricsContext

    private val _currentSongParams = MutableStateFlow<SongParams?>(null)
    val currentSongParams: StateFlow<SongParams?> = _currentSongParams

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    init {
        startWorker()
    }

    private fun startWorker() {
        scope.launch {
            for (request in requestChannel) {
                processNextRequest()
                delay(1000) // Small breather between consecutive generations
            }
        }
    }

    private suspend fun processNextRequest() {
        val state = currentSensorState
        val scene = currentScene
        val myEpoch = generationEpoch
        
        val metricsContext = promptBuilder.buildMetricsContext(state, scene)
        _currentMetricsContext.value = metricsContext
        
        val result = musicRepository.generateClip(metricsContext)
        when (result) {
            is GenerationResult.Success -> {
                if (myEpoch != generationEpoch) {
                    result.audioFile.delete()
                    return
                }
                _currentSongParams.value = result.params
                queue.send(result.audioFile)
                _chunksReady.update { it + 1 }
            }
            is GenerationResult.Error -> {
                Log.w(TAG, "Generation failed: ${result.message}, retrying once")
                delay(2000) // Longer delay before retry
                val retry = musicRepository.generateClip(metricsContext)
                when (retry) {
                    is GenerationResult.Success -> {
                        if (myEpoch != generationEpoch) {
                            retry.audioFile.delete()
                            return
                        }
                        _currentSongParams.value = retry.params
                        queue.send(retry.audioFile)
                        _chunksReady.update { it + 1 }
                    }
                    is GenerationResult.Error -> {
                        Log.e(TAG, "Retry failed: ${retry.message}")
                        if (myEpoch == generationEpoch) _lastError.value = retry.message
                    }
                }
            }
        }
    }

    fun prime(sensorState: SensorState, scene: Scene?) {
        currentSensorState = sensorState
        currentScene = scene
        _chunksReady.value = 0
        repeat(2) { requestChannel.trySend(Unit) }
    }

    fun onChunkStarted() {
        requestChannel.trySend(Unit)
    }

    suspend fun takeNext(): File? = try {
        queue.receive()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "takeNext failed", e)
        null
    }

    fun drainAndReprime(sensorState: SensorState, scene: Scene?) {
        generationEpoch++  // Invalidate all in-flight generation coroutines
        while (true) {
            val polled = queue.tryReceive()
            if (polled.isFailure) break
            polled.getOrNull()?.delete()
        }
        _chunksReady.value = 0
        _lastError.value = null
        prime(sensorState, scene)
    }

    fun retryGeneration() {
        _lastError.value = null
        requestChannel.trySend(Unit)
    }

    fun updateSensorState(state: SensorState) {
        currentSensorState = state
    }

    fun updateScene(scene: Scene?) {
        currentScene = scene
    }

    companion object {
        private const val TAG = "AudioBufferManager"
    }
}

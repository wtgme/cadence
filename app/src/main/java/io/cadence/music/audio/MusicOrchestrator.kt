package io.cadence.music.audio

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.data.sensor.LocationService
import io.cadence.music.data.sensor.SensorStateCollector
import io.cadence.music.domain.SceneDetector
import io.cadence.music.domain.SceneStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

enum class PlaybackState { IDLE, BUFFERING, PLAYING }

@Singleton
class MusicOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorStateCollector: SensorStateCollector,
    private val sceneDetector: SceneDetector,
    private val sceneStateMachine: SceneStateMachine,
    private val bufferManager: AudioBufferManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentScene = MutableStateFlow<Scene?>(null)
    val currentScene: StateFlow<Scene?> = _currentScene

    private val _candidateScene = MutableStateFlow<Scene?>(null)
    val candidateScene: StateFlow<Scene?> = _candidateScene

    private val _currentSensorState = MutableStateFlow(SensorState())
    val currentSensorState: StateFlow<SensorState> = _currentSensorState

    private val _hasHealthPermissions = MutableStateFlow(true)
    val hasHealthPermissions: StateFlow<Boolean> = _hasHealthPermissions

    val healthDiagnostic: StateFlow<String?> = sensorStateCollector.healthDiagnostic

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    val chunksReady: StateFlow<Int> = bufferManager.chunksReady
    val currentMetricsContext: StateFlow<String> = bufferManager.currentMetricsContext
    val currentSongParams: StateFlow<SongParams?> = bufferManager.currentSongParams
    val currentMentalState: StateFlow<MentalState?> = bufferManager.currentMentalState
    val lastError: StateFlow<String?> = bufferManager.lastError
    val songHistory = bufferManager.songHistory
    val playbackProgress = bufferManager.playbackProgress

    private var detectionJob: Job? = null
    private var sceneJob: Job? = null
    private var bufferJob: Job? = null
    private var playbackStarted = false

    /** HR at last generation — only regenerate on significant drift. */
    private var lastGeneratedHr = 0

    fun startDetection() {
        if (detectionJob != null) return
        context.startForegroundService(Intent(context, LocationService::class.java))
        sensorStateCollector.start()

        detectionJob = scope.launch {
            sensorStateCollector.sensorState.collectLatest { state ->
                _currentSensorState.value = state
                bufferManager.updateSensorState(state)
                _candidateScene.value = sceneDetector.detect(state)
                sceneStateMachine.process(state)

                // Check if HR has drifted enough to regenerate
                if (playbackStarted && lastGeneratedHr > 0) {
                    val currentHr = state.heartRate
                    if (currentHr > 0 && abs(currentHr - lastGeneratedHr) >= HR_DRIFT_THRESHOLD) {
                        Log.d(TAG, "HR drift: $lastGeneratedHr → $currentHr — repriming")
                        lastGeneratedHr = currentHr
                        bufferManager.drainAndReprime(state, _currentScene.value)
                    }
                }
            }
        }

        sceneJob = scope.launch {
            sceneStateMachine.confirmedScene.collect { scene ->
                Log.d(TAG, "Scene confirmed: ${scene.displayName()}")
                val previous = _currentScene.value
                _currentScene.value = scene
                bufferManager.updateScene(scene)
                // Context shift — regenerate buffer with new vibe
                if (playbackStarted && previous != null && previous != scene) {
                    Log.d(TAG, "Context shift $previous → $scene — repriming buffer")
                    lastGeneratedHr = _currentSensorState.value.heartRate
                    bufferManager.drainAndReprime(_currentSensorState.value, scene)
                }
            }
        }
    }

    fun startPlayback() {
        startDetection() // restart detection if stopped by a previous stop()
        playbackStarted = true
        _playbackState.value = PlaybackState.BUFFERING

        context.startService(Intent(context, MusicPlayerService::class.java))

        bufferJob?.cancel()
        bufferJob = scope.launch {
            // Refresh biometrics before priming so generation uses the latest sensor data.
            Log.d(TAG, "startPlayback: refreshing biometrics before generation")
            sensorStateCollector.refreshAll()

            lastGeneratedHr = _currentSensorState.value.heartRate
            bufferManager.prime(_currentSensorState.value, _currentScene.value)

            // Wait for first chunk (~18 s with streaming) then start playback immediately.
            // The buffer worker self-sustains — no explicit song 2 kick-off needed.
            bufferManager.chunksReady.first { it >= 1 }
            Log.d(TAG, "First chunk ready — starting playback")
            _playbackState.value = PlaybackState.PLAYING
            context.startService(Intent(context, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY
            })
        }
    }

    fun stop() {
        playbackStarted = false
        bufferJob?.cancel()
        bufferJob = null
        _playbackState.value = PlaybackState.IDLE
        detectionJob?.cancel()
        detectionJob = null
        sceneJob?.cancel()
        sceneJob = null
        sensorStateCollector.stop()
        context.stopService(Intent(context, LocationService::class.java))
        context.stopService(Intent(context, MusicPlayerService::class.java))
    }

    fun retryGeneration() = bufferManager.retryGeneration()

    fun skipToNext() {
        if (playbackStarted) {
            context.startService(Intent(context, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_SKIP_NEXT
            })
        }
    }

    fun skipToPrevious() {
        if (playbackStarted) {
            context.startService(Intent(context, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_SKIP_PREV
            })
        }
    }

    fun seekTo(positionMs: Long) {
        if (playbackStarted) {
            context.startService(Intent(context, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_SEEK
                putExtra(MusicPlayerService.EXTRA_SEEK_POSITION_MS, positionMs)
            })
        }
    }

    suspend fun forceScene(scene: Scene) {
        sceneStateMachine.forceScene(scene)
    }

    suspend fun refreshBiometrics() {
        _hasHealthPermissions.value = sensorStateCollector.hasHeartRatePermission()
        Log.d(TAG, "Refresh: HC heart rate permission granted = ${_hasHealthPermissions.value}")
        sensorStateCollector.refreshAll()
        // Push updated state to UI even if the detection loop was stopped
        _currentSensorState.value = sensorStateCollector.sensorState.first()
    }

    suspend fun checkHealthPermissions() {
        _hasHealthPermissions.value = sensorStateCollector.hasHeartRatePermission()
    }

    companion object {
        private const val TAG = "MusicOrchestrator"
        /** Only regenerate music when HR changes by ±15 bpm. */
        private const val HR_DRIFT_THRESHOLD = 15
    }
}

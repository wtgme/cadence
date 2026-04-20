package io.cadence.music.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cadence.music.audio.MusicOrchestrator
import io.cadence.music.audio.PlaybackProgress
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.data.model.UserMusicAdjustment
import io.cadence.music.data.model.UserTasteMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val orchestrator: MusicOrchestrator,
) : ViewModel() {

    val currentScene: StateFlow<Scene?> = orchestrator.currentScene
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val candidateScene: StateFlow<Scene?> = orchestrator.candidateScene
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sensorState: StateFlow<SensorState> = orchestrator.currentSensorState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorState())

    val playbackState: StateFlow<PlaybackState> = orchestrator.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState.IDLE)

    val chunksReady: StateFlow<Int> = orchestrator.chunksReady
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentMetricsContext: StateFlow<String> = orchestrator.currentMetricsContext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val currentSongParams: StateFlow<SongParams?> = orchestrator.currentSongParams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentMentalState: StateFlow<MentalState?> = orchestrator.currentMentalState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastError: StateFlow<String?> = orchestrator.lastError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songHistory: StateFlow<List<GeneratedSong>> = orchestrator.songHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackProgress: StateFlow<PlaybackProgress> = orchestrator.playbackProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackProgress())

    val hasPrevious: StateFlow<Boolean> = orchestrator.hasPrevious
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val tasteMemory: StateFlow<UserTasteMemory> = orchestrator.tasteMemory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserTasteMemory())

    val currentAdjustment: StateFlow<UserMusicAdjustment> = orchestrator.currentAdjustment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserMusicAdjustment())

    private val _isRefreshingBiometrics = MutableStateFlow(false)
    val isRefreshingBiometrics: StateFlow<Boolean> = _isRefreshingBiometrics.asStateFlow()

    /** Epoch-ms when the current BUFFERING phase started; 0 when not buffering. */
    private val _generationStartMs = MutableStateFlow(0L)
    val generationStartMs: StateFlow<Long> = _generationStartMs.asStateFlow()

    val isAdaptingToHrDrift: StateFlow<Boolean> = orchestrator.isAdaptingToHrDrift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hasHealthPermissions: StateFlow<Boolean> = orchestrator.hasHealthPermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val healthDiagnostic: StateFlow<String?> = orchestrator.healthDiagnostic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        orchestrator.startDetection()
        viewModelScope.launch { orchestrator.checkHealthPermissions() }
        viewModelScope.launch {
            orchestrator.playbackState.collect { state ->
                if (state == PlaybackState.BUFFERING) {
                    _generationStartMs.value = System.currentTimeMillis()
                }
            }
        }
    }

    fun startPlayback() = orchestrator.startPlayback()

    fun stop() = orchestrator.stop()

    fun overrideScene(scene: Scene) {
        viewModelScope.launch { orchestrator.forceScene(scene) }
    }

    fun retryGeneration() = orchestrator.retryGeneration()

    /** Explicit thumbs-up: records as a full positive listen signal. */
    fun thumbsUp() {
        val params = currentSongParams.value ?: return
        orchestrator.recordListenResult(params, 1.0f)
    }

    /** Explicit thumbs-down: records as a strong skip signal. */
    fun thumbsDown() {
        val params = currentSongParams.value ?: return
        orchestrator.recordListenResult(params, 0.0f)
    }

    fun resetTasteMemory() = orchestrator.resetTasteMemory()

    fun toggleGenre(genre: String) = orchestrator.toggleGenre(genre)
    fun clearGenres() = orchestrator.clearGenres()
    fun setEnergyBias(delta: Int) = orchestrator.setEnergyBias(delta)
    fun submitFreeText(text: String) = orchestrator.submitFreeText(text)

    fun skipToNext() {
        // Auto-record how much of the current song was heard before skipping
        val params = currentSongParams.value
        if (params != null) {
            val progress = playbackProgress.value
            if (progress.durationMs > 0L) {
                val fraction = progress.positionMs.toFloat() / progress.durationMs
                orchestrator.recordListenResult(params, fraction)
            }
        }
        orchestrator.skipToNext()
    }

    fun skipToPrevious() = orchestrator.skipToPrevious()

    fun seekTo(positionMs: Long) = orchestrator.seekTo(positionMs)

    fun recheckHealthPermissions() {
        viewModelScope.launch { orchestrator.checkHealthPermissions() }
    }

    fun refreshBiometrics() {
        if (_isRefreshingBiometrics.value) return
        viewModelScope.launch {
            _isRefreshingBiometrics.value = true
            try {
                orchestrator.refreshBiometrics()
            } finally {
                _isRefreshingBiometrics.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        orchestrator.stop()
    }
}

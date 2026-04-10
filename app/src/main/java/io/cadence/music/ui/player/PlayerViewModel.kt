package io.cadence.music.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cadence.music.audio.MusicOrchestrator
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val lastError: StateFlow<String?> = orchestrator.lastError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        orchestrator.startDetection()
    }

    fun startPlayback() = orchestrator.startPlayback()

    fun stop() = orchestrator.stop()

    fun overrideScene(scene: Scene) {
        viewModelScope.launch { orchestrator.forceScene(scene) }
    }

    fun retryGeneration() = orchestrator.retryGeneration()

    fun refreshBiometrics() {
        viewModelScope.launch { orchestrator.refreshBiometrics() }
    }

    override fun onCleared() {
        super.onCleared()
        orchestrator.stop()
    }
}

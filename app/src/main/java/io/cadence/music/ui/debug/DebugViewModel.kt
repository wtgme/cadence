package io.cadence.music.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cadence.music.audio.MusicOrchestrator
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.data.sensor.SensorStateCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    sensorStateCollector: SensorStateCollector,
    orchestrator: MusicOrchestrator,
) : ViewModel() {

    val sensorState: StateFlow<SensorState> = sensorStateCollector.sensorState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorState())

    val scene: StateFlow<Scene?> = orchestrator.currentScene
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val candidateScene: StateFlow<Scene?> = orchestrator.candidateScene
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mentalState: StateFlow<MentalState?> = orchestrator.currentMentalState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

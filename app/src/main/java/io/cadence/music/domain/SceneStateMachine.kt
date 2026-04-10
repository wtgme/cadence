package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 15-second debounce state machine.
 *
 * Uses its own long-lived scope for the debounce timer — NOT the collectLatest scope
 * that calls process(). collectLatest cancels its child coroutines on every new emission
 * (every 2s), which previously prevented the debounce from ever firing.
 */
@Singleton
class SceneStateMachine @Inject constructor(
    private val detector: SceneDetector,
    dispatcher: CoroutineDispatcher,
) {
    private val _confirmedScene = MutableSharedFlow<Scene>(replay = 1)
    val confirmedScene: Flow<Scene> = _confirmedScene.distinctUntilChanged()

    // Own scope so the debounce timer survives across collectLatest cancellations
    private val debounceScope = CoroutineScope(SupervisorJob() + dispatcher)
    private var pendingJob: Job? = null
    private var lastEmittedScene: Scene? = null
    private var pendingCandidate: Scene? = null

    fun process(state: SensorState) {
        val candidate = detector.detect(state)

        if (candidate == lastEmittedScene) {
            pendingJob?.cancel()
            pendingCandidate = null
            return
        }

        // Same candidate already waiting — let the existing timer finish
        if (candidate == pendingCandidate) return

        pendingJob?.cancel()
        pendingCandidate = candidate
        pendingJob = debounceScope.launch {
            delay(DEBOUNCE_MS)
            lastEmittedScene = candidate
            pendingCandidate = null
            _confirmedScene.emit(candidate)
        }
    }

    suspend fun forceScene(scene: Scene) {
        pendingJob?.cancel()
        lastEmittedScene = scene
        _confirmedScene.emit(scene)
    }

    companion object {
        const val DEBOUNCE_MS = 15_000L
    }
}

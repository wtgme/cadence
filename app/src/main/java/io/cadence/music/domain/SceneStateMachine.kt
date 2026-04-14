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

    /**
     * Set by [forceScene]; suppresses auto-detection changes until the user
     * overrides again or [resetOverride] is called (e.g. on playback stop).
     */
    private var userForcedScene: Scene? = null

    fun process(state: SensorState) {
        val candidate = detector.detect(state)

        // Active user override — block auto-detection from reverting the scene.
        // If the sensor eventually agrees with the forced scene, lift the override
        // so normal detection can take over from here.
        userForcedScene?.let { forced ->
            if (candidate != forced) {
                pendingJob?.cancel()
                pendingCandidate = null
                return
            } else {
                userForcedScene = null   // sensor now agrees — resume normal detection
            }
        }

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
        pendingCandidate = null
        userForcedScene = scene
        lastEmittedScene = scene
        _confirmedScene.emit(scene)
    }

    /** Call when starting a fresh detection session so auto-detection resumes cleanly. */
    fun resetOverride() {
        userForcedScene = null
        pendingJob?.cancel()
        pendingCandidate = null
    }

    companion object {
        const val DEBOUNCE_MS = 15_000L
    }
}

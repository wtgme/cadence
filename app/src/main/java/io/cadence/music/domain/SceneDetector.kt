package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneDetector @Inject constructor() {

    fun detect(state: SensorState): Scene = when {
        // Driving: High speed
        state.speedKmh >= DRIVING_SPEED_THRESHOLD -> Scene.COMMUTING

        // Running: Mid-high speed or very high HR
        state.speedKmh >= RUNNING_SPEED_THRESHOLD || state.heartRate > RUNNING_HR_THRESHOLD -> Scene.RUNNING

        // Walking: Human walking speed
        state.speedKmh >= WALKING_SPEED_THRESHOLD -> Scene.WALKING

        // Traffic: Moving slow in a vehicle context (between walking and running speed but usually on roads)
        // Or just very slow movement that isn't resting
        state.speedKmh > RESTING_SPEED_THRESHOLD && state.speedKmh < RUNNING_SPEED_THRESHOLD -> Scene.STUCK_IN_TRAFFIC

        // Default: stationary
        else -> Scene.RESTING
    }

    companion object {
        const val DRIVING_SPEED_THRESHOLD = 25f    // km/h
        const val RUNNING_SPEED_THRESHOLD = 8f     // km/h
        const val WALKING_SPEED_THRESHOLD = 3f     // km/h
        const val RUNNING_HR_THRESHOLD = 135       // bpm
        const val RESTING_SPEED_THRESHOLD = 1f     // km/h
    }
}

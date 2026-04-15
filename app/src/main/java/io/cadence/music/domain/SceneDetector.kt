package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneDetector @Inject constructor() {

    fun detect(state: SensorState): Scene = when {
        // Commuting: vehicle speed
        state.speedKmh >= COMMUTING_SPEED_THRESHOLD -> Scene.COMMUTING

        // Running: high speed or very high HR (gym sprint, treadmill)
        state.speedKmh >= RUNNING_SPEED_THRESHOLD || state.heartRate > RUNNING_HR_THRESHOLD -> Scene.RUNNING

        // Cycling: moderate speed but HR not in running zone
        state.speedKmh >= CYCLING_SPEED_THRESHOLD -> Scene.CYCLING

        // Walking: human pace
        state.speedKmh >= WALKING_SPEED_THRESHOLD -> Scene.WALKING

        // Stationary — distinguish by HR and time of day
        state.heartRate > WORKOUT_HR_THRESHOLD -> Scene.WORKOUT

        state.hourOfDay in FOCUS_HOUR_START..FOCUS_HOUR_END -> Scene.FOCUS

        else -> Scene.RESTING
    }

    companion object {
        const val COMMUTING_SPEED_THRESHOLD = 25f   // km/h
        const val RUNNING_SPEED_THRESHOLD   = 8f    // km/h
        const val CYCLING_SPEED_THRESHOLD   = 4f    // km/h  (above brisk walk)
        const val WALKING_SPEED_THRESHOLD   = 2f    // km/h
        const val RUNNING_HR_THRESHOLD      = 135   // bpm
        const val WORKOUT_HR_THRESHOLD      = 100   // bpm  (stationary elevated HR → gym)
        const val FOCUS_HOUR_START          = 6     // 06:00 inclusive
        const val FOCUS_HOUR_END            = 18    // 18:00 inclusive
    }
}

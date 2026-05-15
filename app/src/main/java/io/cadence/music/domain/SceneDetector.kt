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

        // Party: evening/night, elevated HR (but not workout-level), weekend or Friday night
        isPartyContext(state) -> Scene.PARTY

        // Stationary — distinguish by HR and time of day
        state.heartRate > WORKOUT_HR_THRESHOLD -> Scene.WORKOUT

        state.hourOfDay in FOCUS_HOUR_START..FOCUS_HOUR_END -> Scene.FOCUS

        else -> Scene.RESTING
    }

    private fun isPartyContext(state: SensorState): Boolean {
        if (state.hourOfDay < PARTY_HOUR_START && state.hourOfDay > PARTY_HOUR_END_MORNING) return false
        if (state.heartRate <= PARTY_HR_THRESHOLD) return false
        // Weekend (Fri evening, Sat, Sun) or any night with elevated HR
        val isWeekendWindow = state.dayOfWeek == 1 || state.dayOfWeek == 7 // Sun, Sat
                || (state.dayOfWeek == 6 && state.hourOfDay >= PARTY_HOUR_START) // Fri evening
        return isWeekendWindow || state.heartRate > PARTY_HR_STRONG
    }

    companion object {
        const val COMMUTING_SPEED_THRESHOLD = 25f   // km/h
        const val RUNNING_SPEED_THRESHOLD   = 8f    // km/h
        const val CYCLING_SPEED_THRESHOLD   = 4f    // km/h  (above brisk walk)
        const val WALKING_SPEED_THRESHOLD   = 2f    // km/h
        const val RUNNING_HR_THRESHOLD      = 135   // bpm
        const val WORKOUT_HR_THRESHOLD      = 120   // bpm  (stationary elevated HR → gym)
        const val FOCUS_HOUR_START          = 6     // 06:00 inclusive
        const val FOCUS_HOUR_END            = 18    // 18:00 inclusive
        const val PARTY_HOUR_START          = 20    // 20:00 — earliest party detection
        const val PARTY_HOUR_END_MORNING    = 4     // up to 04:00 (late night)
        const val PARTY_HR_THRESHOLD        = 75    // bpm — minimum HR for party
        const val PARTY_HR_STRONG           = 90    // bpm — any night (even weekday) if HR elevated
    }
}

package io.cadence.music.domain

import io.cadence.music.data.model.ActiveWorkoutType
import io.cadence.music.data.model.MotionActivity
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneDetector @Inject constructor() {

    fun detect(state: SensorState): Scene {
        // 1. Watch-reported workout is the highest-confidence signal — a sensor on the
        //    wrist already classified the activity. Use it directly, regardless of GPS
        //    speed (handles treadmill, stationary bike, rowing, elliptical, etc.).
        state.activeWorkoutType?.let { workout ->
            return when (workout) {
                ActiveWorkoutType.RUNNING, ActiveWorkoutType.HIIT -> Scene.RUNNING
                ActiveWorkoutType.CYCLING -> Scene.CYCLING
                ActiveWorkoutType.WALKING -> Scene.WALKING
                ActiveWorkoutType.ROWING, ActiveWorkoutType.ELLIPTICAL, ActiveWorkoutType.OTHER -> Scene.WORKOUT
            }
        }

        // 2. Outdoor activity — GPS gives a reliable speed signal, prefer it.
        when {
            state.speedKmh >= COMMUTING_SPEED_THRESHOLD -> return Scene.COMMUTING
            state.speedKmh >= RUNNING_SPEED_THRESHOLD || state.heartRate > RUNNING_HR_THRESHOLD -> return Scene.RUNNING
            state.speedKmh >= CYCLING_SPEED_THRESHOLD -> return Scene.CYCLING
            state.speedKmh >= WALKING_SPEED_THRESHOLD -> return Scene.WALKING
        }

        // 3. Indoor / stationary path — GPS is ~0, so trust the on-device motion
        //    classifier. Only treat running/cycling/walking as authoritative; the
        //    stationary/automotive verdicts add no information over the fallthrough.
        if (state.speedKmh < WALKING_SPEED_THRESHOLD) {
            when (state.motionActivity) {
                MotionActivity.RUNNING -> return Scene.RUNNING
                MotionActivity.CYCLING -> return Scene.CYCLING
                MotionActivity.WALKING -> return Scene.WALKING
                MotionActivity.STATIONARY, MotionActivity.AUTOMOTIVE, MotionActivity.UNKNOWN, null -> Unit
            }
        }

        return when {
            isPartyContext(state) -> Scene.PARTY
            state.heartRate > WORKOUT_HR_THRESHOLD -> Scene.WORKOUT
            state.hourOfDay in FOCUS_HOUR_START..FOCUS_HOUR_END -> Scene.FOCUS
            else -> Scene.RESTING
        }
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

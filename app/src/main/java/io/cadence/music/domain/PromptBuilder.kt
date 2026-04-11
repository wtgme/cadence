package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds a raw metrics context string from sensor data.
 * This is NOT the final music prompt — it gets passed to the AI Producer
 * which translates it into structured song parameters.
 */
@Singleton
class PromptBuilder @Inject constructor() {

    fun buildMetricsContext(state: SensorState, scene: Scene?): String {
        val activityLabel = when (scene) {
            Scene.RUNNING -> "Running"
            Scene.WALKING -> "Walking"
            Scene.COMMUTING -> "Driving/Commuting"
            Scene.STUCK_IN_TRAFFIC -> "Driving in Traffic"
            Scene.RESTING -> "Resting"
            null -> "Stationary"
        }

        val sleepLabel = when {
            state.sleepScore == 0  -> "unknown"
            state.sleepScore >= 80 -> "Well-rested"
            state.sleepScore >= 50 -> "Average sleep"
            else -> "Poorly rested"
        }

        val timeLabel = when (state.hourOfDay) {
            in 5..8 -> "Early morning"
            in 9..11 -> "Morning"
            in 12..13 -> "Midday"
            in 14..17 -> "Afternoon"
            in 18..20 -> "Evening"
            else -> "Night"
        }

        return buildString {
            append("Activity: $activityLabel, ")
            append("GPS Speed: ${"%.1f".format(state.speedKmh)} km/h, ")
            append("Weather: ${state.weather}, ")
            append("HR: ${if (state.heartRate > 0) "${state.heartRate} bpm" else "unknown"}, ")
            append("Location: ${"%.4f".format(state.latitude)}, ${"%.4f".format(state.longitude)}, ")
            append("Today: ${state.stepsToday} steps, ${state.activityMinutesToday} mins, ${"%.0f".format(state.caloriesBurned)} kcal, ")
            if (state.readinessScore > 0) {
                append("Readiness: ${state.readinessScore}/100 (${state.readinessBreakdown}), ")
            }
            append("Sleep: $sleepLabel${if (state.sleepScore > 0) " (${state.sleepScore}/100)" else ""}, ")
            append("Time: $timeLabel")
        }
    }
}

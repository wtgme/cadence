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
private fun Int.toTierName() = when (this) { 4 -> "Very High"; 3 -> "High"; 2 -> "Medium"; else -> "Low" }

@Singleton
class PromptBuilder @Inject constructor() {

    fun buildMetricsContext(state: SensorState, scene: Scene?): String {
        val activityLabel = when (scene) {
            Scene.RUNNING   -> "Running"
            Scene.CYCLING   -> "Cycling"
            Scene.WALKING   -> "Walking"
            Scene.COMMUTING -> "Travelling"
            Scene.WORKOUT   -> "Working Out"
            Scene.FOCUS     -> "Focus/Study"
            Scene.PARTY     -> "Party/Social"
            Scene.RESTING   -> "Resting"
            null            -> "Stationary"
        }

        val sleepLabel = when {
            state.sleepScore == 0  -> "unknown"
            state.sleepScore >= 80 -> "Well-rested"
            state.sleepScore >= 50 -> "Average sleep"
            else -> "Poorly rested"
        }

        val dayLabel = when (state.dayOfWeek) {
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            else -> "Saturday"
        }
        val isWeekend = state.dayOfWeek == 1 || state.dayOfWeek == 7

        val amPm = if (state.hourOfDay < 12) "am" else "pm"
        val hour12 = when (state.hourOfDay % 12) { 0 -> 12; else -> state.hourOfDay % 12 }
        val timeStr = "$hour12:${state.minuteOfHour.toString().padStart(2, '0')}$amPm"

        val timeLabel = when (state.hourOfDay) {
            in 5..8 -> "Early morning"
            in 9..11 -> "Morning"
            in 12..13 -> "Midday"
            in 14..17 -> "Afternoon"
            in 18..20 -> "Evening"
            else -> "Night"
        }

        val weatherMood = when {
            state.weather.contains("sun", ignoreCase = true) ||
            state.weather.contains("clear", ignoreCase = true) -> "sunny — favour major key, bright valence, higher energy"
            state.weather.contains("rain", ignoreCase = true) ||
            state.weather.contains("storm", ignoreCase = true) -> "rainy — favour minor key, introspective, lower energy"
            state.weather.contains("cloud", ignoreCase = true) ||
            state.weather.contains("overcast", ignoreCase = true) -> "overcast — soothing, acoustic, neutral valence"
            else -> state.weather
        }

        // Pre-computed music guidance derived from the research framework.
        // Priority: SpO2 safety → contextual state → readiness capacity → sleep.
        val musicGuidance = buildString {
            // 1. SpO2 safety override
            if (state.spo2 in 1..93) {
                append("⚠ SpO2 low (${state.spo2}%) — use <60 BPM, nature-inspired ambient only. ")
            }
            // 2. Iso-principle: match current arousal state first (HR + scene + time),
            //    then apply readiness as the capacity ceiling — not the target.
            //    Night/evening and resting scenes always cap at low-to-medium energy.
            val readinessTier = when {
                state.readinessScore >= 76 -> 4
                state.readinessScore >= 51 -> 3
                state.readinessScore >= 26 -> 2
                state.readinessScore in 1..25 -> 1
                else -> 0  // unknown
            }
            val contextCap = when {
                state.hourOfDay >= 21 || state.hourOfDay < 5 -> 1   // Night → Low
                state.hourOfDay in 18..20 -> 2                       // Evening → Medium cap
                scene == Scene.PARTY -> 4                            // Party → uncapped (high energy)
                scene == Scene.RESTING || scene == null -> 2         // Resting → Medium cap
                scene == Scene.FOCUS -> 2                            // Focus → Medium cap (calm, non-distracting)
                else -> 4
            }
            val effectiveTier = if (readinessTier > 0) minOf(readinessTier, contextCap) else contextCap
            val (tierLabel, bpmNote) = when (effectiveTier) {
                4    -> "Very High" to "target 145+ BPM (sympathetic drive)"
                3    -> "High"      to "target 110–130 BPM (flow state)"
                2    -> "Medium"    to "target 90–110 BPM (active recovery)"
                else -> "Low"       to "target <60 BPM (parasympathetic rebound)"
            }
            if (readinessTier > 0 && readinessTier != effectiveTier) {
                append("Readiness capacity: ${readinessTier.toTierName()} — capped to $tierLabel by current context ($bpmNote). ")
            } else {
                append("Energy tier: $tierLabel — $bpmNote. ")
            }
            // 3. Sleep architecture modifiers (values stored as percentages 0–100)
            if (state.sleepRemPct > 0f && state.sleepRemPct < 15f) {
                append("Low REM sleep (${state.sleepRemPct.toInt()}%) — use simple melodies, high melodic clarity. ")
            }
            if (state.sleepDeepPct > 0f && state.sleepDeepPct < 10f) {
                append("Low deep sleep (${state.sleepDeepPct.toInt()}%) — reduce percussive density, avoid heavy drums. ")
            }
        }.trim()

        return buildString {
            append("Activity: $activityLabel, ")
            append("GPS Speed: ${"%.1f".format(state.speedKmh)} km/h, ")
            append("Weather: $weatherMood, ")
            append("HR: ${if (state.heartRate > 0) "${state.heartRate} bpm" else "unknown"}, ")
            if (state.spo2 > 0) append("SpO2: ${state.spo2}%, ")
            append("Location: ${"%.4f".format(state.latitude)}, ${"%.4f".format(state.longitude)}, ")
            append("Today: ${state.stepsToday} steps, ${state.activityMinutesToday} mins, ${"%.0f".format(state.caloriesBurned)} kcal, ")
            if (state.readinessScore > 0) {
                append("Readiness: ${state.readinessScore}/100 (${state.readinessBreakdown}), ")
            }
            val sleepDetail = buildString {
                append(sleepLabel)
                if (state.sleepScore > 0) append(" (${state.sleepScore}/100)")
                if (state.sleepDeepPct > 0f) append(", deep ${state.sleepDeepPct.toInt()}%")
                if (state.sleepRemPct > 0f)  append(", REM ${state.sleepRemPct.toInt()}%")
            }
            append("Sleep: $sleepDetail, ")
            append("Time: $timeLabel ($timeStr), ")
            append("Day: $dayLabel${if (isWeekend) " (weekend)" else " (weekday)"}")
            if (musicGuidance.isNotEmpty()) append("\nMusic guidance: $musicGuidance")
        }
    }
}

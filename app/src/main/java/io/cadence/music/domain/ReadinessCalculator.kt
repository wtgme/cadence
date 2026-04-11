package io.cadence.music.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Daily readiness score, 1..100 (0 = unknown).
 *
 * Combines sleep quality, HRV-vs-baseline, resting HR-vs-baseline, and previous-day
 * training load. Each component is transparent and weighted so the breakdown is
 * inspectable — no black-box magic. Components missing their signal contribute 0.
 *
 * This is NOT Samsung's Energy Score — it's our own interpretation using the same
 * kinds of signals (sleep, HRV, RHR, load). The number will be correlated with
 * Samsung's but won't match exactly.
 */
@Singleton
class ReadinessCalculator @Inject constructor() {

    data class Inputs(
        val sleepScore: Int,           // 0..100, 0 = unknown
        val hrvToday: Float,           // ms RMSSD, 0 = unknown
        val hrvBaseline: Float,        // 14-day mean, 0 = unknown
        val restingHrToday: Int,       // bpm, 0 = unknown
        val restingHrBaseline: Float,  // 14-day mean, 0 = unknown
        val yesterdayActiveKcal: Float, // 0 = unknown
        val activeKcalBaseline: Float,  // daily mean over baseline window, 0 = unknown
    )

    data class Result(
        val score: Int,          // 0 = unknown; 1..100 otherwise
        val breakdown: String,   // short human-readable component summary
    )

    fun compute(inputs: Inputs): Result {
        val parts = mutableListOf<Pair<String, Int>>()
        var score = 50

        // Sleep: weight 50% (largest single driver). Maps 0..100 → -25..+25.
        if (inputs.sleepScore > 0) {
            val sleepComp = ((inputs.sleepScore - 50) * 0.5f).roundToInt().coerceIn(-25, 25)
            score += sleepComp
            parts += "Sleep" to sleepComp
        }

        // HRV: higher than baseline = better recovery. ±15 max.
        if (inputs.hrvToday > 0f && inputs.hrvBaseline > 0f) {
            val pctDelta = (inputs.hrvToday - inputs.hrvBaseline) / inputs.hrvBaseline
            val hrvComp = (pctDelta * 100f).roundToInt().coerceIn(-15, 15)
            score += hrvComp
            parts += "HRV" to hrvComp
        }

        // Resting HR: lower than baseline = better recovery. ±15 max.
        // Each 1bpm below baseline ≈ +2 points.
        if (inputs.restingHrToday > 0 && inputs.restingHrBaseline > 0f) {
            val delta = inputs.restingHrBaseline - inputs.restingHrToday
            val rhrComp = (delta * 2f).roundToInt().coerceIn(-15, 15)
            score += rhrComp
            parts += "RHR" to rhrComp
        }

        // Yesterday's load: heavy day yesterday → small penalty.
        if (inputs.yesterdayActiveKcal > 0f && inputs.activeKcalBaseline > 0f) {
            val ratio = inputs.yesterdayActiveKcal / inputs.activeKcalBaseline
            val penalty = when {
                ratio >= 1.5f -> -10
                ratio >= 1.2f -> -5
                else -> 0
            }
            if (penalty != 0) {
                score += penalty
                parts += "Load" to penalty
            }
        }

        // Need at least one real signal — otherwise we return "unknown" rather than 50.
        if (parts.isEmpty()) {
            return Result(score = 0, breakdown = "no data")
        }

        val clamped = score.coerceIn(1, 100)
        val breakdown = parts.joinToString(", ") { (label, delta) ->
            val sign = if (delta >= 0) "+" else ""
            "$label $sign$delta"
        }
        return Result(score = clamped, breakdown = breakdown)
    }
}

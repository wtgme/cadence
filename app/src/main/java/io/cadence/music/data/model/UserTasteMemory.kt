package io.cadence.music.data.model

import com.squareup.moshi.JsonClass

/**
 * Persistent taste profile accumulated across listening sessions.
 *
 * Scores are exponential moving averages in [-1, +1]:
 *   +1 = strongly preferred, -1 = strongly disliked, 0 = neutral / no data yet
 *
 * [genreScores]        — genre-level preferences,       e.g. "electronic" → +0.72
 * [tagScores]          — all tag preferences (genre + emotion + instrument)
 * [contextGenreScores] — scene-scoped genre preferences, key = "SCENE_NAME:genre"
 * [feedbackCount]      — total feedback signals recorded across all sessions
 * [lastUpdatedMs]      — epoch-ms of the most recent update
 */
@JsonClass(generateAdapter = true)
data class UserTasteMemory(
    val genreScores: Map<String, Float> = emptyMap(),
    val tagScores: Map<String, Float> = emptyMap(),
    val contextGenreScores: Map<String, Float> = emptyMap(),
    val feedbackCount: Int = 0,
    val lastUpdatedMs: Long = 0L,
)

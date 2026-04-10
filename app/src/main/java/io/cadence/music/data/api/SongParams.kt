package io.cadence.music.data.api

import com.squareup.moshi.JsonClass

/**
 * Structured parameters for the new music generation API.
 * Produced by Step 1 translation from health metrics.
 */
@JsonClass(generateAdapter = true)
data class SongParams(
    val lyric: String,
    val tags: String? = null,
    val duration: Float = 240.0f,
    val topk: Int = 50,
    val temperature: Float = 1.0f,
    val cfg_scale: Float = 1.5f
)

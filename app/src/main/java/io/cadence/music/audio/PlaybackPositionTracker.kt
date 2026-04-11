package io.cadence.music.audio

data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

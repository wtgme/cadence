package io.cadence.music.data.model

import io.cadence.music.data.api.SongParams

data class GeneratedSong(
    val id: Long,
    val params: SongParams,
    val mentalState: MentalState?,
    val scene: Scene?,
    val generatedAt: Long,
)

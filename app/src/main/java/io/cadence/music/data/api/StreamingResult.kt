package io.cadence.music.data.api

import java.io.File

sealed class StreamingChunk {
    data class Audio(val file: File, val index: Int, val params: SongParams) : StreamingChunk()
    data object Complete : StreamingChunk()
    data class Error(val message: String) : StreamingChunk()
}

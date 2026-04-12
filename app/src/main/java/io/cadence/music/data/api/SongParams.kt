package io.cadence.music.data.api

import com.squareup.moshi.JsonClass

/**
 * Parameters for the SongGeneration `/generate` endpoint.
 *
 * Server schema:
 *   lyric                 : string (required) — section-tagged lyrics; ignored in bgm mode (server uses ".")
 *   descriptions          : string? — comma-separated style tags, e.g. "electronic,energetic"
 *   auto_prompt_audio_type: string? — reference style; one of Pop/Latin/Rock/Electronic/Metal/
 *                                     Country/R&B-Soul/Ballad/Jazz/World/Hip-Hop/Funk/Soundtrack/Auto
 *   generate_type         : string  — "bgm" (default, pure instrumental), "mixed", "vocal"
 *
 * For pure instrumental output (default), [generate_type] should be "bgm".
 * The [lyric] field is then ignored server-side; pass a minimal placeholder.
 */
@JsonClass(generateAdapter = true)
data class SongParams(
    val lyric: String,
    val descriptions: String? = null,
    val auto_prompt_audio_type: String? = null,
    val generate_type: String = "bgm",
)

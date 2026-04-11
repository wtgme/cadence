package io.cadence.music.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import io.cadence.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-step music generation pipeline using self-hosted servers:
 *   1. Gemma  (port 8001) — translates health metrics into [SongParams]
 *   2. MusicGen (port 8000) — renders [SongParams] into an audio clip
 */
@Singleton
class MusicRepository @Inject constructor(
    private val cacheDir: File,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) : GenerationRepository {

    private val gemmaClient = okHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val musicGenClient = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val songParamsAdapter = moshi.adapter(SongParams::class.java)

    override suspend fun generateClip(metricsContext: String): GenerationResult {
        val params = translateMetrics(metricsContext)
        Log.d(TAG, "SongParams: tags=${params.tags} lyric=${params.lyric.take(60)}…")
        return generateAudio(params)
    }

    // ── Step 1: Gemma → SongParams ────────────────────────────────────────────

    private suspend fun translateMetrics(metricsContext: String): SongParams =
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "messages" to listOf(mapOf("role" to "user", "content" to "$SYSTEM_INSTRUCTION\n\nUser metrics: $metricsContext")),
                    "max_new_tokens" to 1024,
                    "temperature" to 0.7,
                )
                @Suppress("UNCHECKED_CAST")
                val json = moshi.adapter(Map::class.java as Class<Map<String, Any>>).toJson(body)
                val request = Request.Builder()
                    .url("${BuildConfig.GEMMA_BASE_URL}/generate")
                    .post(json.toRequestBody(JSON))
                    .build()

                val response = gemmaClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemma ${response.code} — using fallback params")
                    return@withContext fallbackParams(metricsContext)
                }

                @Suppress("UNCHECKED_CAST")
                val responseMap = moshi.adapter(Map::class.java).fromJson(response.body?.string().orEmpty()) as? Map<String, Any>
                val text = (responseMap?.get("text") as? String)?.trim()

                if (text.isNullOrBlank()) return@withContext fallbackParams(metricsContext)

                val params = songParamsAdapter.fromJson(extractJson(text))
                if (params != null && params.lyric.isNotBlank()) params
                else fallbackParams(metricsContext)
            } catch (e: Exception) {
                Log.e(TAG, "Gemma translation failed", e)
                fallbackParams(metricsContext)
            }
        }

    // ── Step 2: MusicGen → audio file ─────────────────────────────────────────

    private suspend fun generateAudio(params: SongParams): GenerationResult =
        withContext(Dispatchers.IO) {
            try {
                val json = songParamsAdapter.toJson(params)
                val request = Request.Builder()
                    .url("${BuildConfig.SONGGEN_BASE_URL}/generate")
                    .post(json.toRequestBody(JSON))
                    .build()

                val response = musicGenClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val msg = "MusicGen returned ${response.code}"
                    Log.w(TAG, msg)
                    return@withContext GenerationResult.Error(msg)
                }

                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val file = File(cacheDir, "music_${System.currentTimeMillis()}.mp3")
                    file.writeBytes(bytes)
                    Log.d(TAG, "Generated ${file.length() / 1024}KB — ${file.name}")
                    GenerationResult.Success(file, params)
                } else {
                    GenerationResult.Error("No audio data from MusicGen")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MusicGen failed", e)
                GenerationResult.Error(e.message ?: "Unknown error")
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun extractJson(text: String): String {
        val fenceOpen = text.indexOf("```")
        val fenceClose = text.lastIndexOf("```")
        val content = if (fenceOpen >= 0 && fenceClose > fenceOpen) {
            val start = text.indexOf('\n', fenceOpen).takeIf { it >= 0 }?.plus(1) ?: (fenceOpen + 3)
            text.substring(start, fenceClose).trim()
        } else text
        val braceStart = content.indexOf('{')
        val braceEnd = content.lastIndexOf('}')
        return if (braceStart >= 0 && braceEnd > braceStart) content.substring(braceStart, braceEnd + 1) else content
    }

    private fun fallbackParams(metricsContext: String): SongParams {
        val hr = Regex("HR: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 90
        val energy = Regex("Energy: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 50
        val speed = Regex("Speed: ([\\d.]+)").find(metricsContext)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val rainy = metricsContext.contains("Rain", ignoreCase = true) || metricsContext.contains("Cold", ignoreCase = true)

        val genre = when {
            energy >= 76 && !rainy -> "pop, rock"
            energy >= 76           -> "electronic, funk"
            energy >= 26           -> "r&b, soul"
            else                   -> "jazz, ballad"
        }
        val bpm = when {
            speed > 20f -> 80; speed > 8f -> 160; speed > 5f -> 125; speed > 2f -> 100; else -> 70
        }
        val vocals = if (energy > 50) "mixed vocals" else "instrumental, bgm"
        val intensity = when {
            hr > 140 -> "high-energy, driving beat, percussive"
            hr > 100 -> "upbeat, rhythmic, moderate energy"
            else     -> "mellow, calm, soft"
        }
        val lyric = when {
            hr > 140 -> "[chorus]\nMove to the beat; Push through the heat;\n[verse]\nEvery step drives the fire in me;"
            hr > 100 -> "[verse]\nWalking down the open road\nLeaving all I've ever known\n[chorus]\nKeep on moving keep on rolling"
            else     -> "[verse]\nSoft lights glowing in the dark...\nBreathing slowly letting go...\n[outro]\nRest now... close your weary eyes..."
        }
        return SongParams(
            lyric = lyric,
            tags = "$genre, $bpm BPM, $intensity, $vocals",
            duration = 240f,
            topk = if (energy > 50) 50 else 250,
            temperature = if (hr > 100) 0.8f else 1.2f,
            cfg_scale = if (hr > 100) 1.5f else 2.5f,
        )
    }

    companion object {
        private const val TAG = "MusicRepository"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private val SYSTEM_INSTRUCTION = """
            You are a clinical music therapist and AI music producer. Your job is to translate real-time biometric signals into precise song generation parameters that are scientifically matched to the user's physiological state.

            Output ONLY a valid JSON object with exactly these fields: "lyric", "tags", "duration", "topk", "temperature", "cfg_scale". No explanation, no markdown.

            ═══════════════════════════════════════════
            STEP 1 — GENRE (affects "tags")
            ═══════════════════════════════════════════
            Energy Score 76–100 + Sunny/Warm  → Pop, Rock, Hip-Hop
            Energy Score 76–100 + Rainy/Cold  → Electronic, Metal, Funk
            Energy Score 26–75  + Any weather → R&B/Soul, Country, World Music
            Energy Score 0–25   + Any weather → Jazz, Ballad, Cinematic Soundtrack

            ═══════════════════════════════════════════
            STEP 2 — TEMPO & INTENSITY (affects "tags")
            ═══════════════════════════════════════════
            Stationary / GPS < 2 km/h  → 60–80 BPM, ambient, minimal
            Walking     / GPS 2–5 km/h → 100 BPM, mellow, steady
            Jogging     / GPS 5–8 km/h → 120–130 BPM, rhythmic, upbeat
            Running     / GPS > 8 km/h → 150–180 BPM, high-energy, driving beat
            Driving     / GPS > 20 km/h→ 60–100 BPM, chill, alert

            Refine from Heart Rate:
            - HR high + ongoing activity  → match BPM to HR, percussive
            - HR high + slowing activity  → start at HR BPM, tag "gradually slowing to 100 BPM, calming"
            - HR normal or low            → use activity-based BPM

            ═══════════════════════════════════════════
            STEP 3 — VOCAL TYPE (affects "tags")
            ═══════════════════════════════════════════
            Energy > 50                       → "mixed vocals"
            Energy < 25 OR Sleep Score < 70   → "instrumental, bgm"
            SpO2 primary concern              → "acapella, vocal"

            ═══════════════════════════════════════════
            STEP 4 — LYRIC STRUCTURE (affects "lyric")
            ═══════════════════════════════════════════
            HR > 140: short punchy [chorus]-first, semicolons
            HR 100–140: balanced [verse]/[chorus]/[bridge]
            HR < 100: flowing [verse] and [outro] with "..." pauses

            ═══════════════════════════════════════════
            STEP 5 — REMAINING FIELDS
            ═══════════════════════════════════════════
            "duration": 240.0
            "topk": 50 structured / 250 experimental
            "temperature": 0.8 active / 1.2 rest
            "cfg_scale": 1.5 active / 2.5 sleep/recovery

            IMPORTANT: Output ONLY the JSON object. No explanation, no markdown fences.
        """.trimIndent()
    }
}

package io.cadence.music.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import io.cadence.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-step music generation pipeline:
 *   Step 1 — OpenRouter (minimax-m2.5:free): biometric context → [SongParams]
 *   Step 2 — [GenerationBackend] (SongGeneration): [SongParams] → audio file
 */
@Singleton
class MusicRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val backend: GenerationBackend,
) : GenerationRepository {

    private val openRouterClient = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val _translatedSongParams = MutableStateFlow<SongParams?>(null)
    override val translatedSongParams: StateFlow<SongParams?> = _translatedSongParams

    private fun publishTranslatedParams(params: SongParams) {
        Log.d(TAG, "SongParams: descriptions=${params.descriptions} auto_prompt_type=${params.auto_prompt_audio_type}")
        _translatedSongParams.value = params
    }

    // ── Step 1: OpenRouter (minimax-m2.5) → SongParams ────────────────────────

    override suspend fun translateMetrics(metricsContext: String): SongParams =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.OPENROUTER_API_KEY
            if (apiKey.isBlank()) {
                Log.w(TAG, "OPENROUTER_API_KEY missing — using fallback params")
                return@withContext fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            }
            try {
                @Suppress("UNCHECKED_CAST")
                val bodyJson = moshi.adapter(Map::class.java as Class<Map<String, Any>>).toJson(mapOf(
                    "model" to OPENROUTER_MODEL,
                    "messages" to listOf(
                        mapOf("role" to "system", "content" to SYSTEM_INSTRUCTION),
                        mapOf("role" to "user", "content" to "Biometric & environmental snapshot:\n$metricsContext"),
                    ),
                    "temperature" to 0.7,
                ))
                val request = Request.Builder()
                    .url("$OPENROUTER_BASE_URL/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("HTTP-Referer", "https://cadence.music")
                    .header("X-Title", "Cadence")
                    .post(bodyJson.toRequestBody(JSON))
                    .build()

                Log.d(TAG, "OpenRouter → POST /chat/completions model=$OPENROUTER_MODEL")
                logChunked("OpenRouter system prompt", SYSTEM_INSTRUCTION)
                logChunked("OpenRouter user prompt", "Biometric & environmental snapshot:\n$metricsContext")

                var lastCode = -1
                var lastErrBody = ""
                for (attempt in 1..MAX_ATTEMPTS) {
                    val tStart = System.currentTimeMillis()
                    val result: SongParams? = try {
                        val call = openRouterClient.newCall(request.newBuilder().build())
                        call.execute().use { response ->
                            val elapsed = System.currentTimeMillis() - tStart
                            if (!response.isSuccessful) {
                                lastCode = response.code
                                lastErrBody = response.body?.string()?.take(300).orEmpty()
                                Log.w(TAG, "OpenRouter ← HTTP ${response.code} in ${elapsed}ms (attempt $attempt/$MAX_ATTEMPTS) — $lastErrBody")
                                null
                            } else {
                                val raw = response.body?.string().orEmpty()
                                Log.d(TAG, "OpenRouter ← HTTP ${response.code} in ${elapsed}ms (${raw.length}B)")
                                val content = extractContent(raw)
                                if (content.isNullOrBlank()) {
                                    Log.w(TAG, "OpenRouter returned empty content — raw head: ${raw.take(300)}")
                                    null
                                } else {
                                    Log.d(TAG, "OpenRouter content (${content.length}B): ${content.take(400)}")
                                    val parsed = parseSongParams(extractJson(content))
                                    if (parsed != null) {
                                        Log.d(TAG, "OpenRouter parsed → descriptions=${parsed.descriptions} auto_prompt=${parsed.auto_prompt_audio_type}")
                                        parsed
                                    } else {
                                        Log.w(TAG, "OpenRouter parse failed (descriptions missing or schema mismatch)")
                                        null
                                    }
                                }
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        lastCode = -1
                        Log.w(TAG, "OpenRouter timed out (attempt $attempt/$MAX_ATTEMPTS)")
                        null
                    }
                    if (result != null) {
                        publishTranslatedParams(result)
                        return@withContext result
                    }
                    val retriable = lastCode == -1 || lastCode == 429 || lastCode in 500..599
                    if (!retriable || attempt == MAX_ATTEMPTS) break
                    val backoffMs = 2000L * (1 shl (attempt - 1))
                    Log.d(TAG, "OpenRouter retrying after ${backoffMs}ms (HTTP $lastCode)")
                    delay(backoffMs)
                }
                Log.w(TAG, "OpenRouter gave up after $MAX_ATTEMPTS attempts (last HTTP $lastCode) — using fallback params")
                fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            } catch (e: Exception) {
                Log.e(TAG, "OpenRouter translation failed (${e.javaClass.simpleName}: ${e.message}) — using fallback params")
                fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            }
        }

    /**
     * Parse LLM response into [SongParams].
     * Accepts: lyric/lyrics, descriptions/tags, auto_prompt_audio_type.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSongParams(json: String): SongParams? {
        val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<String, Any?> ?: return null

        // descriptions is the primary signal — treat a missing/blank response as a parse failure
        val descriptions = when (val d = map["descriptions"] ?: map["tags"]) {
            is String -> d.trim().ifBlank { null }
            is List<*> -> d.filterNotNull().joinToString(",") { it.toString().trim() }.ifBlank { null }
            else -> null
        }
        if (descriptions == null) return null

        val autoPromptType = (map["auto_prompt_audio_type"] as? String)?.trim()
            ?.takeIf { it in AUTO_PROMPT_TYPES }

        return SongParams(
            lyric = ".",   // bgm mode ignores lyric entirely on the server
            descriptions = descriptions,
            auto_prompt_audio_type = autoPromptType,
            generate_type = "bgm",
        )
    }

    // ── Step 2: GenerationBackend → audio ────────────────────────────────────

    override fun generateAudioStream(params: SongParams): Flow<StreamingChunk> =
        backend.generateStream(params)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fallbackParams(metricsContext: String): SongParams {
        val hr        = Regex("HR: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 90
        val speed     = Regex("Speed: ([\\d.]+)").find(metricsContext)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val readiness = Regex("Readiness: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val spo2      = Regex("SpO2: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val isRainy   = metricsContext.contains("rainy", ignoreCase = true) ||
                        metricsContext.contains("rain", ignoreCase = true)

        // SpO2 safety override
        if (spo2 in 1..93) {
            return SongParams(lyric = ".", descriptions = "ambient,slow,peaceful,atmospheric", auto_prompt_audio_type = "Soundtrack", generate_type = "bgm")
        }

        // Energy tier from readiness, falling back to HR + speed
        val energyTier = when {
            readiness >= 76              -> 4
            readiness >= 51              -> 3
            readiness >= 26              -> 2
            readiness in 1..25           -> 1
            hr > 140 || speed > 8f      -> 4
            speed > 3f || hr in 100..139 -> 3
            hr in 70..99                 -> 2
            else                         -> 1
        }

        val moodTag = if (isRainy) "melancholic" else "uplifting"

        val (descriptions, autoType) = when (energyTier) {
            4    -> "electronic,energetic,powerful,synthesizer,drum machine" to "Electronic"
            3    -> "pop,energetic,$moodTag,bass guitar" to "Pop"
            2    -> "jazz,focused,$moodTag,piano,saxophone" to "Jazz"
            else -> "ambient,calm,peaceful,dreamy,piano" to "Soundtrack"
        }

        return SongParams(lyric = ".", descriptions = descriptions, auto_prompt_audio_type = autoType, generate_type = "bgm")
    }

    private fun logChunked(label: String, text: String) {
        if (text.isEmpty()) { Log.d(TAG, "$label [empty]"); return }
        val chunkSize = 1000
        val total = (text.length + chunkSize - 1) / chunkSize
        var idx = 0; var chunk = 0
        while (idx < text.length) {
            val end = minOf(idx + chunkSize, text.length)
            Log.d(TAG, "$label [${++chunk}/$total]\n${text.substring(idx, end)}")
            idx = end
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractContent(raw: String): String? {
        if (raw.isBlank()) return null
        val root = moshi.adapter(Map::class.java).fromJson(raw) as? Map<String, Any> ?: return null
        val choices = root["choices"] as? List<Map<String, Any>> ?: return null
        val message = choices.firstOrNull()?.get("message") as? Map<String, Any> ?: return null
        return (message["content"] as? String)?.trim()
    }

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

    companion object {
        private const val TAG = "MusicRepository"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
        private const val OPENROUTER_MODEL = "minimax/minimax-m2.5:free"
        private const val MAX_ATTEMPTS = 3

        private val AUTO_PROMPT_TYPES = setOf(
            "Pop", "Latin", "Rock", "Electronic", "Metal", "Country",
            "R&B/Soul", "Ballad", "Jazz", "World", "Hip-Hop", "Funk", "Soundtrack", "Auto",
        )

        /**
         * System prompt for OpenRouter. Asks for SongGeneration-compatible JSON.
         *
         * The server always uses generate_type="bgm" (pure instrumental), which ignores
         * the lyric field entirely — so we always pass lyric=".". Only descriptions and
         * auto_prompt_audio_type affect the actual sound output.
         *
         * Server applies: "[Musicality-very-high], [Pure-Music], " + descriptions.lowercase()
         * before passing to the model, so tags must be plain lowercase words.
         */
        private val SYSTEM_INSTRUCTION = """
            You are a biometric-aware music producer. Translate a real-time sensor snapshot into
            music style parameters for an AI music generation model. Output ONLY a valid JSON
            object — no explanation, no markdown fences.

            JSON fields:
              "descriptions": 3–6 comma-separated lowercase tags from these dimensions:
                  Genre    : pop, jazz, rock, electronic, ambient, classical, funk, r&b, hip-hop, folk, new-age, blues
                  Emotion  : energetic, calm, peaceful, uplifting, melancholic, introspective, focused,
                             euphoric, powerful, dreamy, relaxing, sad, cheerful, romantic
                  Instrument: piano, synthesizer, electric guitar, acoustic guitar, drums, drum machine,
                              bass guitar, strings, violin, saxophone, trumpet, flute
              "auto_prompt_audio_type": MUST be exactly one of:
                  Pop, Latin, Rock, Electronic, Metal, Country, R&B/Soul, Ballad, Jazz, World, Hip-Hop, Funk, Soundtrack, Auto
                  NOTE: "Ambient" is NOT valid — use Soundtrack instead.

            Encode tempo through genre — do NOT use words like fast/slow/mid-tempo/driving/upbeat:
              145+ BPM → electronic or rock + energetic/powerful + drums
              110–130 BPM → pop or funk + energetic/uplifting + bass guitar
              90–110 BPM  → jazz or folk + focused/cheerful + piano or saxophone
              <60 BPM     → ambient or classical or new-age + calm/peaceful/dreamy + piano or strings

            Rules:
              - Follow the Energy Tier in Music guidance exactly
              - Sunny weather → add uplifting or euphoric; rainy → add melancholic or introspective (not both)
              - Low REM / low deep sleep flags → no drums or drum machine; use piano, strings, or acoustic guitar
              - Night/evening → add dreamy or introspective; no drum machine
              - Never mix contradictory emotions (e.g. calm + energetic)

            Examples:
            User: Activity: Stationary, HR: 52 bpm, Time: Night (10pm), Weather: overcast
            Music guidance: Energy tier: Low — target <60 BPM (parasympathetic rebound).
            Assistant: {"descriptions": "ambient, peaceful, dreamy, relaxing, piano, strings", "auto_prompt_audio_type": "Soundtrack"}

            User: Activity: Running, HR: 162 bpm, Time: Morning (8am), Weather: sunny
            Music guidance: Energy tier: Very High — target 145+ BPM (sympathetic drive).
            Assistant: {"descriptions": "electronic, energetic, powerful, euphoric, synthesizer, drums", "auto_prompt_audio_type": "Electronic"}

            User: Activity: Driving/Commuting, HR: 80 bpm, Time: Morning (8am), Weather: overcast
            Music guidance: Energy tier: High — target 110–130 BPM (flow state).
            Assistant: {"descriptions": "pop, energetic, focused, uplifting, bass guitar, synthesizer", "auto_prompt_audio_type": "Pop"}

            User: Activity: Stationary, HR: 68 bpm, Time: Afternoon (2pm), Weather: sunny
            Music guidance: Energy tier: High — target 110–130 BPM (flow state).
            Assistant: {"descriptions": "pop, focused, uplifting, cheerful, synthesizer, bass guitar", "auto_prompt_audio_type": "Pop"}

            User: Activity: Walking, HR: 95 bpm, Time: Evening (7pm), Weather: rainy
            Music guidance: Readiness capacity: Medium — capped to Medium. Low deep sleep (8%) — reduce percussive density. Low REM (14%) — use simple melodies.
            Assistant: {"descriptions": "jazz, melancholic, introspective, focused, piano, acoustic guitar", "auto_prompt_audio_type": "Jazz"}
        """.trimIndent()
    }
}

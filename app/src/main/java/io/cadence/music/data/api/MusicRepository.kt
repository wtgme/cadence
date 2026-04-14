package io.cadence.music.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import io.cadence.music.BuildConfig
import io.cadence.music.data.adjustment.UserAdjustmentRepository
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.taste.TasteMemoryRepository
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
 * Three-step music generation pipeline:
 *   Step 1a — OpenRouter (nemotron): biometric context → [MentalState]
 *   Step 1b — OpenRouter (nemotron): [MentalState] → [SongParams]
 *   Step 2  — [GenerationBackend] (SongGeneration): [SongParams] → audio file
 *
 * Fallback chain if the two-query path fails:
 *   1. Two-query (1a → 1b)         ← preferred
 *   2. Single-query (original prompt) ← if either step fails
 *   3. Hardcoded [fallbackParams]   ← if OpenRouter is unreachable
 */
@Singleton
class MusicRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val backend: GenerationBackend,
    private val tasteMemory: TasteMemoryRepository,
    private val userAdjustmentRepository: UserAdjustmentRepository,
) : GenerationRepository {

    private val openRouterClient = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val _translatedSongParams = MutableStateFlow<SongParams?>(null)
    override val translatedSongParams: StateFlow<SongParams?> = _translatedSongParams

    private val _translatedMentalState = MutableStateFlow<MentalState?>(null)
    override val translatedMentalState: StateFlow<MentalState?> = _translatedMentalState

    private fun publishTranslatedParams(params: SongParams) {
        Log.d(TAG, "SongParams: descriptions=${params.descriptions} auto_prompt_type=${params.auto_prompt_audio_type}")
        _translatedSongParams.value = params
    }

    // ── Step 1 (public): two-query pipeline with single-query fallback ─────────

    override suspend fun translateMetrics(metricsContext: String): SongParams =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.OPENROUTER_API_KEY
            if (apiKey.isBlank()) {
                Log.w(TAG, "OPENROUTER_API_KEY missing — using fallback params")
                return@withContext fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            }
            try {
                // Step 1a: sensor metrics → mental state
                val mentalState = estimateMentalState(metricsContext, apiKey)
                _translatedMentalState.value = mentalState
                Log.d(TAG, "Step 1a: arousal=${mentalState?.arousal} valence=${mentalState?.valence} stress=${mentalState?.stress} energy=${mentalState?.energy} focus=${mentalState?.focus} mood=${mentalState?.mood}")

                if (mentalState != null && mentalState.arousal != null && mentalState.valence != null) {
                    // Step 1b: mental state → song params
                    val params = translateMentalStateToParams(mentalState, apiKey)
                    if (params != null) {
                        publishTranslatedParams(params)
                        return@withContext params
                    }
                    Log.w(TAG, "Step 1b failed — falling back to single-query")
                } else {
                    Log.w(TAG, "Step 1a failed (missing arousal/valence) — falling back to single-query")
                }

                // Single-query fallback (original SYSTEM_INSTRUCTION)
                val singleResult = singleQueryTranslation(metricsContext, apiKey)
                if (singleResult != null) {
                    publishTranslatedParams(singleResult)
                    return@withContext singleResult
                }

                fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            } catch (e: Exception) {
                Log.e(TAG, "OpenRouter translation failed (${e.javaClass.simpleName}: ${e.message}) — using fallback params")
                fallbackParams(metricsContext).also { publishTranslatedParams(it) }
            }
        }

    // ── Step 1a: sensor metrics → MentalState ─────────────────────────────────

    private suspend fun estimateMentalState(metricsContext: String, apiKey: String): MentalState? {
        Log.d(TAG, "Step 1a: estimating mental state")
        logChunked("Step 1a system", MENTAL_STATE_SYSTEM)
        logChunked("Step 1a user", metricsContext)

        val rawText = callOpenRouter(
            apiKey = apiKey,
            systemPrompt = MENTAL_STATE_SYSTEM,
            userMessage = "Biometric sensor snapshot:\n$metricsContext",
            label = "Step 1a",
        ) ?: return null

        return parseMentalState(extractJson(rawText), rawText)
    }

    // ── Step 1b only (public): cached MentalState → SongParams ───────────────

    override suspend fun translateMentalState(mentalState: MentalState, previousParams: SongParams?): SongParams? =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.OPENROUTER_API_KEY
            if (apiKey.isBlank()) return@withContext null
            try {
                translateMentalStateToParams(mentalState, apiKey, previousParams)?.also { publishTranslatedParams(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Step 1b re-query failed (${e.javaClass.simpleName}: ${e.message})")
                null
            }
        }

    // ── Step 1b: MentalState → SongParams ─────────────────────────────────────

    private suspend fun translateMentalStateToParams(mentalState: MentalState, apiKey: String, previousParams: SongParams? = null): SongParams? {
        val mentalStateJson = buildMentalStateJson(mentalState)
        val tasteContext = tasteMemory.buildTasteContext()
        val adjustmentHint = userAdjustmentRepository.adjustment.value.toPromptHint()
        val userMessage = buildString {
            append("User's current mental state:\n$mentalStateJson")
            if (tasteContext.isNotEmpty()) {
                append("\n\n$tasteContext")
            }
            if (adjustmentHint != null) {
                append("\n\n$adjustmentHint")
            }
            if (previousParams != null) {
                append("\n\nPrevious song: descriptions=\"${previousParams.descriptions}\"" +
                    (previousParams.auto_prompt_audio_type?.let { ", auto_prompt_audio_type=\"$it\"" } ?: "") +
                    ". Choose a noticeably different style — vary the primary genre or instruments.")
            }
        }

        Log.d(TAG, "Step 1b: translating mental state to song params")
        logChunked("Step 1b system", SONG_PARAMS_FROM_MENTAL_STATE_SYSTEM)
        logChunked("Step 1b user", userMessage)

        val rawText = callOpenRouter(
            apiKey = apiKey,
            systemPrompt = SONG_PARAMS_FROM_MENTAL_STATE_SYSTEM,
            userMessage = userMessage,
            label = "Step 1b",
        ) ?: return null

        return parseSongParams(extractJson(rawText))
    }

    // ── Single-query fallback ─────────────────────────────────────────────────

    private suspend fun singleQueryTranslation(metricsContext: String, apiKey: String): SongParams? {
        Log.d(TAG, "Single-query fallback: translating metrics directly")
        logChunked("Single-query system", SYSTEM_INSTRUCTION)

        val rawText = callOpenRouter(
            apiKey = apiKey,
            systemPrompt = SYSTEM_INSTRUCTION,
            userMessage = "Biometric & environmental snapshot:\n$metricsContext",
            label = "single-query",
        ) ?: return null

        return parseSongParams(extractJson(rawText))
    }

    // ── OpenRouter call with retry ────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private suspend fun callOpenRouter(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        label: String,
    ): String? {
        val bodyJson = moshi.adapter(Map::class.java as Class<Map<String, Any>>).toJson(mapOf(
            "model" to OPENROUTER_MODEL,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userMessage),
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

        Log.d(TAG, "OpenRouter → POST /chat/completions model=$OPENROUTER_MODEL [$label]")

        var lastCode = -1
        var lastErrBody = ""
        for (attempt in 1..MAX_ATTEMPTS) {
            val tStart = System.currentTimeMillis()
            val result: String? = try {
                val call = openRouterClient.newCall(request.newBuilder().build())
                call.execute().use { response ->
                    val elapsed = System.currentTimeMillis() - tStart
                    if (!response.isSuccessful) {
                        lastCode = response.code
                        lastErrBody = response.body?.string()?.take(300).orEmpty()
                        Log.w(TAG, "OpenRouter [$label] ← HTTP ${response.code} in ${elapsed}ms (attempt $attempt/$MAX_ATTEMPTS) — $lastErrBody")
                        null
                    } else {
                        val raw = response.body?.string().orEmpty()
                        Log.d(TAG, "OpenRouter [$label] ← HTTP ${response.code} in ${elapsed}ms (${raw.length}B)")
                        val content = extractContent(raw)
                        if (content.isNullOrBlank()) {
                            // Log a window around the first non-whitespace character to reveal the actual content
                            val firstNonWs = raw.indexOfFirst { !it.isWhitespace() }
                            val snippet = if (firstNonWs >= 0) raw.substring(firstNonWs, minOf(firstNonWs + 400, raw.length)) else "(all whitespace)"
                            Log.w(TAG, "OpenRouter [$label] empty content — first non-ws at $firstNonWs: $snippet")
                            null
                        } else {
                            Log.d(TAG, "OpenRouter [$label] content (${content.length}B): ${content.take(400)}")
                            content
                        }
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastCode = -1
                Log.w(TAG, "OpenRouter [$label] timed out (attempt $attempt/$MAX_ATTEMPTS)")
                null
            }

            if (result != null) return result

            val retriable = lastCode == -1 || lastCode == 429 || lastCode in 500..599
            if (!retriable || attempt == MAX_ATTEMPTS) break
            val backoffMs = 2000L * (1 shl (attempt - 1))
            Log.d(TAG, "OpenRouter [$label] retrying after ${backoffMs}ms (HTTP $lastCode)")
            delay(backoffMs)
        }
        Log.w(TAG, "OpenRouter [$label] gave up after $MAX_ATTEMPTS attempts (last HTTP $lastCode)")
        return null
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseMentalState(json: String, rawLlmText: String): MentalState? {
        @Suppress("UNCHECKED_CAST")
        val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<String, Any?> ?: return null
        val arousal = (map["arousal"] as? Number)?.toInt()
        val valence = (map["valence"] as? Number)?.toInt()
        // Require at least arousal and valence for a usable mental state
        if (arousal == null && valence == null) return null
        return MentalState(
            arousal = arousal,
            valence = valence,
            stress  = (map["stress"] as? Number)?.toInt(),
            energy  = (map["energy"] as? Number)?.toInt(),
            focus   = (map["focus"] as? Number)?.toInt(),
            mood    = (map["mood"] as? String)?.trim(),
            rawLlmText = rawLlmText,
        )
    }

    /**
     * Serialises [MentalState] fields (excluding [MentalState.rawLlmText]) to a JSON string
     * for use as the Step 1b user message.
     */
    private fun buildMentalStateJson(ms: MentalState): String {
        val map = buildMap<String, Any> {
            ms.arousal?.let { put("arousal", it) }
            ms.valence?.let { put("valence", it) }
            ms.stress?.let  { put("stress", it) }
            ms.energy?.let  { put("energy", it) }
            ms.focus?.let   { put("focus", it) }
            ms.mood?.let    { put("mood", it) }
        }
        @Suppress("UNCHECKED_CAST")
        return moshi.adapter(Map::class.java as Class<Map<String, Any>>).toJson(map)
    }

    /**
     * Parse LLM response into [SongParams].
     * Accepts: descriptions/tags, auto_prompt_audio_type.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSongParams(json: String): SongParams? {
        val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<String, Any?> ?: return null

        val descriptions = when (val d = map["descriptions"] ?: map["tags"]) {
            is String -> d.trim().ifBlank { null }
            is List<*> -> d.filterNotNull().joinToString(",") { it.toString().trim() }.ifBlank { null }
            else -> null
        }
        if (descriptions == null) return null

        val autoPromptType = (map["auto_prompt_audio_type"] as? String)?.trim()
            ?.takeIf { it in AUTO_PROMPT_TYPES }

        return SongParams(
            lyric = ".",
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

        if (spo2 in 1..93) {
            return SongParams(lyric = ".", descriptions = "ambient,slow,peaceful,atmospheric", auto_prompt_audio_type = "Soundtrack", generate_type = "bgm")
        }

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
        val chunkSize = 800
        val total = (text.length + chunkSize - 1) / chunkSize
        var idx = 0; var chunk = 0
        while (idx < text.length) {
            val end = minOf(idx + chunkSize, text.length)
            // Use a prefix on each line so the tag stays visible in filtered logcat output
            val body = text.substring(idx, end)
                .lines()
                .joinToString("\n") { "  $it" }
            Log.d(TAG, "$label [${++chunk}/$total] $body")
            idx = end
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractContent(raw: String): String? {
        if (raw.isBlank()) return null
        val root = moshi.adapter(Map::class.java).fromJson(raw) as? Map<String, Any> ?: return null
        val choices = root["choices"] as? List<Map<String, Any>> ?: return null
        val message = choices.firstOrNull()?.get("message") as? Map<String, Any> ?: return null
        val content = (message["content"] as? String)?.trim()
        if (!content.isNullOrBlank()) return content
        // Some reasoning models (e.g. nvidia/nemotron) return null content and put the
        // actual response in a "reasoning" field. Fall back to it so we don't burn a retry.
        val reasoning = (message["reasoning"] as? String)?.trim()
        if (!reasoning.isNullOrBlank()) {
            Log.d(TAG, "extractContent: content was empty — using reasoning field (${reasoning.length}B)")
        }
        return reasoning.takeIf { !it.isNullOrBlank() }
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
        private const val OPENROUTER_MODEL = "openrouter/free"
        private const val MAX_ATTEMPTS = 3

        private val AUTO_PROMPT_TYPES = setOf(
            "Pop", "Latin", "Rock", "Electronic", "Metal", "Country",
            "R&B/Soul", "Ballad", "Jazz", "World", "Hip-Hop", "Funk", "Soundtrack", "Auto",
        )

        // ── Step 1a: sensor metrics → MentalState ─────────────────────────────

        private val MENTAL_STATE_SYSTEM = """
            You are a psychophysiologist trained in the Russell circumplex model of affect.
            Given real-time biometric and environmental sensor data, estimate the user's current
            mental and physiological state. Output ONLY a valid JSON object — no explanation,
            no markdown fences.

            JSON schema:
              "arousal": integer 0–10  (Russell circumplex activation axis.
                         0 = deeply relaxed/asleep, 5 = neutral alertness, 10 = maximally activated/agitated)
              "valence": integer -5 to +5  (Russell circumplex pleasure axis.
                         -5 = very distressed/miserable, 0 = neutral, +5 = very happy/elated)
              "stress":  integer 0–10  (psychological stress. 0 = completely relaxed, 10 = extreme stress)
              "energy":  integer 0–10  (subjective physical energy. 0 = exhausted/depleted, 10 = fully energised)
              "focus":   integer 0–10  (attentional focus. 0 = scattered/drowsy, 10 = deep sustained concentration)
              "mood":    string — one short phrase (e.g. "alert and motivated", "tired but content")

            Interpretation guidelines:
              - Arousal and energy are DIFFERENT. A stressed commuter may have high arousal (tense, elevated HR)
                but LOW energy (poorly rested, depleted). A runner has high arousal AND high energy.
              - Stress and arousal are DIFFERENT. High arousal can be positive (exercise, excitement) or
                negative (anxiety, stress). Use sleep quality, readiness score, time, and context to disambiguate.
              - Focus depends on context: stationary + afternoon + good sleep → focused work;
                stationary + night + low readiness → drowsy.
              - The "Music guidance" line is a mechanical heuristic. You may DISAGREE with it when the
                biometric context suggests a different state. For example: if readiness says "High" but
                sleep was poor and it is Monday morning during a commute, the user is likely tired and
                stressed, not energised.
              - Use the FULL range of each scale. Do not cluster everything around 5.

            Key signals:
              - Heart rate > 100 at rest → stress or post-exercise; > 140 → active exercise
              - Low readiness + high activity → pushing through fatigue
              - Deep sleep < 10% → impaired physical recovery (lower energy)
              - REM < 15% → impaired cognitive function (lower focus)
              - SpO2 < 94% → physiological distress (high stress, low energy)
              - Late night (after 9 pm) → circadian low (lower arousal/energy)
              - Rain/overcast → lower valence; sun/clear → higher valence
              - Weekday commute → more stress than weekend leisure
        """.trimIndent()

        // ── Step 1b: MentalState → SongParams ─────────────────────────────────

        private val SONG_PARAMS_FROM_MENTAL_STATE_SYSTEM = """
            You are an expert music therapist and producer. Given a user's current mental and
            physiological state, select instrumental music parameters for an AI music generation model.
            Output ONLY a valid JSON object — no explanation, no markdown fences.

            JSON fields:
              "descriptions": 3–6 comma-separated lowercase tags from these pools:
                  Genre    : pop, jazz, rock, electronic, ambient, classical, funk, r&b, hip-hop, folk, new-age, blues
                  Emotion  : energetic, calm, peaceful, uplifting, melancholic, introspective, focused,
                             euphoric, powerful, dreamy, relaxing, sad, cheerful, romantic
                  Instrument: piano, synthesizer, electric guitar, acoustic guitar, drums, drum machine,
                              bass guitar, strings, violin, saxophone, trumpet, flute
              "auto_prompt_audio_type": exactly one of:
                  Pop, Latin, Rock, Electronic, Metal, Country, R&B/Soul, Ballad, Jazz, World,
                  Hip-Hop, Funk, Soundtrack, Auto
                  NOTE: "Ambient" is NOT valid — use Soundtrack instead.

            DECISION PROCEDURE — follow this exact priority order:

            PRIORITY 1 — STRESS OVERRIDE (mandatory):
              If stress >= 7: select a calming genre ONLY.
                Allowed genres    : ambient, classical, jazz, new-age, folk
                Allowed emotions  : calm, peaceful, relaxing, dreamy, introspective
                Allowed instruments: piano, strings, acoustic guitar, flute, saxophone
                auto_prompt_audio_type MUST be one of: Soundtrack, Jazz, Ballad
                Do NOT use: electronic, rock, pop, funk, drums, drum machine, energetic, powerful, euphoric
                This rule applies REGARDLESS of arousal or energy values.

            PRIORITY 2 — ISO-PRINCIPLE (match then nudge):
              Match music energy to current arousal; nudge valence gently upward. Do not jump more
              than one tier from current arousal.
                arousal 8–10 → electronic, rock, or pop + energetic/powerful/euphoric + drums/synthesizer/electric guitar
                               auto_prompt_audio_type: Electronic, Rock, or Pop
                arousal 5–7  → pop, funk, or r&b + focused/uplifting/cheerful + bass guitar/synthesizer/piano
                               auto_prompt_audio_type: Pop, Funk, or R&B/Soul
                arousal 3–4  → jazz, folk, or classical + focused/cheerful/introspective + piano/saxophone/acoustic guitar
                               auto_prompt_audio_type: Jazz, Ballad, or Soundtrack
                arousal 0–2  → ambient, classical, or new-age + calm/peaceful/dreamy + piano/strings/flute
                               auto_prompt_audio_type: Soundtrack or Ballad

            PRIORITY 3 — MODIFIERS (apply after Priorities 1 and 2):
              - If focus >= 7: include "focused". Prefer piano, strings, acoustic guitar (minimal percussion).
              - If energy <= 3 AND arousal >= 5: user is pushing through fatigue — dial back one arousal tier.
              - If valence <= -2: include exactly one of "melancholic" or "introspective" (not both).
              - If valence >= 3: include exactly one of "uplifting" or "euphoric" (not both).

            CONSTRAINTS:
              - Never combine contradictory emotions: calm+energetic, peaceful+powerful, dreamy+euphoric.
              - Encode tempo through genre — do NOT use words like fast, slow, mid-tempo, driving, upbeat.
              - Include at least one instrument tag and at least one emotion tag.
              - descriptions must be a single comma-separated string, not an array.
              - If a "Previous song" is provided: do not repeat the exact same primary genre AND
                instrument combination. Stay within the same arousal tier but vary the sound.
        """.trimIndent()

        // ── Single-query fallback (original prompt) ────────────────────────────

        /**
         * Original single-query prompt. Used as a fallback when either step 1a or 1b fails.
         * Kept intentionally separate from the two-query prompts.
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
        """.trimIndent()
    }
}

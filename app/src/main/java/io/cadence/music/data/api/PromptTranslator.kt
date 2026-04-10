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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Gemma (self-hosted) to translate raw health metrics into structured
 * song generation parameters for the new music generation API.
 *
 * This is Step 1 of the two-step chain:
 *   Metrics → Gemma → SongParams → SongGeneration API → Audio
 */
@Singleton
class PromptTranslator @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) {

    private val client = okHttpClient.newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val songParamsAdapter = moshi.adapter(SongParams::class.java)

    /**
     * Translates raw metrics context into structured [SongParams].
     * Falls back to generic params if the model call fails.
     */
    suspend fun translate(metricsContext: String): SongParams = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = "$SYSTEM_INSTRUCTION\n\nUser metrics: $metricsContext"
            val body = mapOf(
                "messages" to listOf(mapOf("role" to "user", "content" to fullPrompt)),
                "max_new_tokens" to 1024,
                "temperature" to 0.7,
            )
            val json = moshi.adapter(Map::class.java).toJson(body)
            val request = Request.Builder()
                .url(GEMMA_URL)
                .post(json.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemma returned ${response.code}")
                return@withContext fallbackParams(metricsContext)
            }

            val responseBody = response.body?.string()
            val responseMap = moshi.adapter(Map::class.java).fromJson(responseBody.orEmpty())
            val text = (responseMap?.get("text") as? String)?.trim()

            if (text.isNullOrBlank()) {
                Log.w(TAG, "Empty response from Gemma — using fallback")
                return@withContext fallbackParams(metricsContext)
            }

            val jsonStr = extractJson(text)
            val params = songParamsAdapter.fromJson(jsonStr)
            if (params != null && params.lyric.isNotBlank()) {
                Log.d(TAG, "Translated params: lyric=${params.lyric.take(60)}… tags=${params.tags}")
                params
            } else {
                Log.w(TAG, "Failed to parse SongParams from Gemma output")
                fallbackParams(metricsContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemma translation failed", e)
            fallbackParams(metricsContext)
        }
    }

    private fun extractJson(text: String): String {
        // Strip markdown code fence (```json ... ``` or ``` ... ```) if present
        val fenceOpen = text.indexOf("```")
        val fenceClose = text.lastIndexOf("```")
        val content = if (fenceOpen >= 0 && fenceClose > fenceOpen) {
            val bodyStart = text.indexOf('\n', fenceOpen).takeIf { it >= 0 }?.plus(1) ?: (fenceOpen + 3)
            text.substring(bodyStart, fenceClose).trim()
        } else text
        val braceStart = content.indexOf('{')
        val braceEnd = content.lastIndexOf('}')
        return if (braceStart >= 0 && braceEnd > braceStart) content.substring(braceStart, braceEnd + 1) else content
    }

    private fun fallbackParams(metricsContext: String): SongParams {
        val hr = Regex("HR: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 90
        val energy = Regex("Energy: (\\d+)").find(metricsContext)?.groupValues?.get(1)?.toIntOrNull() ?: 50
        val speed = Regex("Speed: ([\\d.]+)").find(metricsContext)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val weather = if (metricsContext.contains("Rain", ignoreCase = true) ||
                         metricsContext.contains("Cold", ignoreCase = true)) "rainy" else "sunny"

        // Genre from Energy + Weather (Step 1)
        val genre = when {
            energy >= 76 && weather == "sunny" -> "pop, rock"
            energy >= 76 -> "electronic, funk"
            energy >= 26 -> "r&b, soul"
            else -> "jazz, ballad"
        }

        // Tempo from speed (Step 2)
        val bpm = when {
            speed > 20f -> 80   // driving
            speed > 8f  -> 160  // running
            speed > 5f  -> 125  // jogging
            speed > 2f  -> 100  // walking
            else        -> 70   // stationary
        }

        // Vocal type from Energy (Step 3)
        val vocalTag = if (energy > 50) "mixed vocals" else "instrumental, bgm"

        // Intensity tag from HR
        val intensityTag = when {
            hr > 140 -> "high-energy, driving beat, percussive"
            hr > 100 -> "upbeat, rhythmic, moderate energy"
            else     -> "mellow, calm, soft"
        }

        val tags = "$genre, ${bpm} BPM, $intensityTag, $vocalTag"

        // Lyric structure from HR zone (Step 4)
        val lyric = when {
            hr > 140 -> "[chorus]\nMove to the beat; Push through the heat;\n[verse]\nEvery step drives the fire in me;\n[chorus]\nMove to the beat; Push through the heat;"
            hr > 100 -> "[verse]\nWalking down the open road\nLeaving all I've ever known\nWindows down and feeling free\nThis is where I'm meant to be\n[chorus]\nKeep on moving keep on rolling\nLife is calling and I'm going"
            else     -> "[verse]\nSoft lights glowing in the dark...\nQuiet whispers leave their mark...\n[verse]\nBreathing slowly letting go...\nDrifting with the evening flow...\n[outro]\nRest now... close your weary eyes...\nFloat beneath the starlit skies..."
        }

        return SongParams(
            lyric = lyric,
            tags = tags,
            duration = 240f,
            topk = if (energy > 50) 50 else 250,
            temperature = if (hr > 100) 0.8f else 1.2f,
            cfg_scale = if (hr > 100) 1.5f else 2.5f,
        )
    }

    companion object {
        private const val TAG = "PromptTranslator"
        private val GEMMA_URL = "${BuildConfig.GEMMA_BASE_URL}/generate"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val SYSTEM_INSTRUCTION = """
            You are a clinical music therapist and AI music producer. Your job is to translate real-time biometric signals into precise song generation parameters that are scientifically matched to the user's physiological state.

            Output ONLY a valid JSON object with exactly these fields: "lyric", "tags", "duration", "topk", "temperature", "cfg_scale". No explanation, no markdown.

            ═══════════════════════════════════════════
            STEP 1 — GENRE (affects "tags")
            ═══════════════════════════════════════════
            Select the base genre using Energy Score and Weather together:

            Energy Score 76–100 + Sunny/Warm  → Pop, Rock, Hip-Hop
              (High reserves + positive weather = high-valence, energetic music)
            Energy Score 76–100 + Rainy/Cold  → Electronic, Metal, Funk
              (High energy allows intensity even in reflective conditions)
            Energy Score 26–75  + Any weather → R&B/Soul, Country, World Music
              (Moderate energy suits steady mid-tempo "flow state" genres)
            Energy Score 0–25   + Any weather → Jazz, Ballad, Cinematic Soundtrack
              (Low reserves require restorative, lower-intensity music)

            ═══════════════════════════════════════════
            STEP 2 — TEMPO & INTENSITY (affects "tags")
            ═══════════════════════════════════════════
            Set BPM and intensity from GPS Speed and Steps:

            Stationary / GPS < 2 km/h     → 60–80 BPM, ambient, minimal
            Walking     / GPS 2–5 km/h    → 100 BPM, mellow, steady
            Jogging     / GPS 5–8 km/h    → 120–130 BPM, rhythmic, upbeat
            Running     / GPS > 8 km/h    → 150–180 BPM, high-energy, driving beat
            Driving     / GPS > 20 km/h   → 60–100 BPM, chill, alert
              (Driving: intentionally lower BPM to prevent overstimulation)

            Refine arousal from Heart Rate:
            - HR high + activity ongoing  → match BPM to HR to maintain cadence (e.g., 160 BPM, percussive)
            - HR high + activity slowing  → Iso-principle: start at current HR BPM, add "gradually slowing to 100 BPM, calming"
            - HR normal or low            → use activity-based BPM above

            Adjust complexity from Sleep Score:
            - Sleep Score < 70  → simple structure, soft melodies, no sudden changes (avoids cognitive strain)
            - Sleep Score 70–85 → balanced structure, moderate dynamics
            - Sleep Score > 85  → complex layers, wide dynamic range, experimental allowed

            ═══════════════════════════════════════════
            STEP 3 — VOCAL TYPE (affects "tags")
            ═══════════════════════════════════════════
            Choose vocal mode based on Energy Score and Sleep Score:

            Energy Score > 50                   → include "mixed vocals" in tags
              (Lyrics provide 18% higher motivational effect during active tasks)
            Energy Score < 25 OR Sleep Score < 70 → include "instrumental, bgm" in tags
              (Non-lyrical music more effective at reducing HR, anxiety, blood pressure)
            SpO2 is the primary concern         → include "acapella, vocal" in tags
              (Singing activates respiratory muscles, shown to raise SpO2 during recovery)

            ═══════════════════════════════════════════
            STEP 4 — LYRIC STRUCTURE (affects "lyric")
            ═══════════════════════════════════════════
            Structure lyrics based on Heart Rate Zone:

            High Intensity (HR > 140) / Intervals:
              Use short, punchy, repetitive sections separated with semicolons.
              Match fast breathing rhythm. Prioritize [chorus] repetition.
              Example pattern: [chorus] Move to the beat; [verse] Keep the pace; [chorus] Reach the peak;

            Moderate Activity (HR 100–140):
              Balanced verse/chorus structure, energising but not breathless.
              Use [verse], [chorus], one [bridge].

            Relaxation / Rest (HR < 100):
              Long, flowing phrases with natural pauses (use "...").
              Emphasise [verse] and [outro], minimal [chorus].
              Example pattern: [verse] Breathing in the quiet;...; [outro] Drift to the calm;

            If vocal type is instrumental/bgm: still write "lyric" as descriptive scene-setting text
            (the model uses it as a thematic guide even without vocals).

            ═══════════════════════════════════════════
            STEP 5 — REMAINING FIELDS
            ═══════════════════════════════════════════
            - "duration": 240.0 (default unless activity is very short)
            - "topk": 50 for structured output; raise to 250 for experimental/high-sleep
            - "temperature": 0.8 for focused activity; 1.2 for rest/ambient
            - "cfg_scale": 1.5 for active; 2.5 for sleep/recovery (stronger style adherence)

            ═══════════════════════════════════════════
            DECISION EXAMPLE
            ═══════════════════════════════════════════
            Input: HR=145, GPS=10 km/h, Weather=Sunny, Energy=80, Sleep=82, SpO2=98
            → Genre: Rock (Energy 80 + Sunny)
            → Tempo: 150 BPM, driving drums, high-energy (GPS 10 km/h running)
            → Arousal: match HR 145 BPM, percussive
            → Vocals: mixed vocals (Energy > 50)
            → Lyric: short punchy chorus-first structure (HR > 140)
            → tags: "rock, 150 BPM, high-energy, driving drums, motivational, mixed vocals"

            IMPORTANT: Output ONLY the JSON object. No explanation, no markdown fences.
        """.trimIndent()
    }
}

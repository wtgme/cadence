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
import javax.inject.Inject
import javax.inject.Singleton

sealed class GenerationResult {
    data class Success(val audioFile: File, val params: SongParams) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}

/**
 * Two-step music generation chain using self-hosted servers:
 *   1. [PromptTranslator] (Gemma): metrics → [SongParams]
 *   2. SongGeneration API: SongParams → audio clip
 */
@Singleton
class LyriaMusicRepository @Inject constructor(
    private val cacheDir: File,
    private val promptTranslator: PromptTranslator,
    private val client: OkHttpClient,
    private val moshi: Moshi,
) : GenerationRepository {

    private val songParamsAdapter = moshi.adapter(SongParams::class.java)

    override suspend fun generateClip(metricsContext: String): GenerationResult {
        // Step 1: Translate metrics into structured song parameters
        val songParams = promptTranslator.translate(metricsContext)
        Log.d(TAG, "Song params: lyric=${songParams.lyric.take(60)}… tags=${songParams.tags}")

        // Step 2: Generate audio via new SongGeneration API
        return generateAudio(songParams)
    }

    private suspend fun generateAudio(params: SongParams): GenerationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating audio clip via new SongGeneration API…")
            val json = songParamsAdapter.toJson(params)
            val request = Request.Builder()
                .url(SONGGEN_URL)
                .post(json.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val msg = "SongGeneration API returned ${response.code}"
                Log.w(TAG, msg)
                return@withContext GenerationResult.Error(msg)
            }

            val audioBytes = response.body?.bytes()
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                val file = File(cacheDir, "songgen_${System.currentTimeMillis()}.mp3")
                file.writeBytes(audioBytes)
                Log.d(TAG, "Generated ${file.length() / 1024}KB audio: ${file.name}")
                GenerationResult.Success(file, params)
            } else {
                Log.w(TAG, "No audio data in SongGeneration response")
                GenerationResult.Error("No audio data returned from SongGeneration API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SongGeneration failed", e)
            GenerationResult.Error(e.message ?: "Unknown error")
        }
    }

    companion object {
        private const val TAG = "SongGenRepo"
        private val SONGGEN_URL = "${BuildConfig.SONGGEN_BASE_URL}/generate"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

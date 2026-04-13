package io.cadence.music.data.api

import android.util.Log
import io.cadence.music.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Music generation backend backed by the SongGeneration v2-large server.
 *
 * Sends `POST /generate` with `{lyric, descriptions, auto_prompt_audio_type, generate_type}`.
 * The server has no streaming endpoint; [generateStream] wraps [generate] as a single-chunk
 * flow (one complete MP3 per emission), designed for future streaming backends.
 *
 * Retries up to [MAX_ATTEMPTS] times with exponential backoff on transient server errors
 * (503 model-loading, 500/502/504 transient failures). Response body is streamed to disk
 * to avoid holding a 20-50 MB MP3 in memory.
 */
@Singleton
class SongGenerationBackend @Inject constructor(
    private val cacheDir: File,
    private val okHttpClient: OkHttpClient,
) : GenerationBackend {

    override val name = "SongGeneration-v2-large"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)   // generation takes 60-120s
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(params: SongParams): GenerationResult =
        withContext(Dispatchers.IO) {
            var lastError = GenerationResult.Error("Unknown error")
            for (attempt in 1..MAX_ATTEMPTS) {
                val result = attemptGenerate(params)
                if (result is GenerationResult.Success) return@withContext result
                lastError = result as GenerationResult.Error
                if (!isRetriable(lastError) || attempt == MAX_ATTEMPTS) break
                val backoffMs = 3000L * (1 shl (attempt - 1))   // 3s, 6s
                Log.w(TAG, "$name retrying after ${backoffMs}ms (attempt $attempt/$MAX_ATTEMPTS): ${lastError.message}")
                delay(backoffMs)
            }
            lastError
        }

    /**
     * Executes one generation attempt and properly cancels the OkHttp call if the
     * coroutine is cancelled mid-flight (e.g. user pressed Stop).
     *
     * OkHttp's [execute] is a blocking call — coroutine cancellation cannot interrupt
     * it via a suspension point. [suspendCancellableCoroutine] lets us register an
     * [invokeOnCancellation] handler that explicitly calls [okhttp3.Call.cancel],
     * which causes [execute] to throw [IOException] and unblocks the thread.
     */
    private suspend fun attemptGenerate(params: SongParams): GenerationResult {
        val body = JSONObject().apply {
            put("lyric", params.lyric)
            params.descriptions?.let { put("descriptions", it) }
            params.auto_prompt_audio_type?.let { put("auto_prompt_audio_type", it) }
            put("generate_type", params.generate_type)
        }.toString()

        Log.d(TAG, "$name → POST /generate " +
            "descriptions=${params.descriptions} " +
            "auto_prompt_type=${params.auto_prompt_audio_type} " +
            "type=${params.generate_type}"
        )

        val request = Request.Builder()
            .url("${BuildConfig.SONGGEN_BASE_URL}/generate")
            .post(body.toRequestBody(JSON))
            .build()

        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)

            // Cancel the OkHttp call (and unblock the thread) when the coroutine is cancelled
            cont.invokeOnCancellation {
                Log.d(TAG, "$name: request cancelled — aborting OkHttp call")
                call.cancel()
            }

            val result = try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string()?.take(300).orEmpty()
                        val msg = "HTTP ${response.code}${if (errBody.isNotEmpty()) ": $errBody" else ""}"
                        Log.w(TAG, "$name ← $msg")
                        GenerationResult.Error(msg)
                    } else {
                        val file = File(cacheDir, "music_${System.currentTimeMillis()}.mp3")
                        response.body?.byteStream()?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (file.length() > 0) {
                            Log.d(TAG, "$name ← ${file.length() / 1024}KB saved as ${file.name}")
                            GenerationResult.Success(file, params)
                        } else {
                            file.delete()
                            GenerationResult.Error("No audio data from $name")
                        }
                    }
                }
            } catch (e: IOException) {
                if (cont.isCancelled) return@suspendCancellableCoroutine  // swallow; caller gets CancellationException
                Log.e(TAG, "$name generate failed: ${e.message}")
                GenerationResult.Error(e.message ?: "IO error")
            } catch (e: Exception) {
                if (cont.isCancelled) return@suspendCancellableCoroutine
                Log.e(TAG, "$name generate failed", e)
                GenerationResult.Error(e.message ?: "Unknown error")
            }

            if (cont.isActive) cont.resume(result)
        }
    }

    private fun isRetriable(error: GenerationResult.Error): Boolean {
        val msg = error.message
        return msg.startsWith("HTTP 429") ||
            msg.startsWith("HTTP 500") ||
            msg.startsWith("HTTP 502") ||
            msg.startsWith("HTTP 503") ||
            msg.startsWith("HTTP 504")
    }

    override fun generateStream(params: SongParams): Flow<StreamingChunk> = flow {
        when (val result = generate(params)) {
            is GenerationResult.Success -> {
                emit(StreamingChunk.Audio(result.audioFile, 0, params))
                emit(StreamingChunk.Complete)
            }
            is GenerationResult.Error -> emit(StreamingChunk.Error(result.message))
        }
    }

    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BuildConfig.SONGGEN_BASE_URL}/health")
                .get()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    companion object {
        private const val TAG = "SongGenerationBackend"
        private const val MAX_ATTEMPTS = 3
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

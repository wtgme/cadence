package io.cadence.music.data.api

import android.util.Log
import io.cadence.music.BuildConfig
import io.cadence.music.data.settings.ApiSettingsRepository
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
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongGenerationBackend @Inject constructor(
    private val cacheDir: File,
    private val okHttpClient: OkHttpClient,
    private val apiSettings: ApiSettingsRepository,
) : GenerationBackend {

    override val name get() = "SongGen-${apiSettings.current.songGenModel}"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
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
                val backoffMs = 3000L * (1 shl (attempt - 1))
                Log.w(TAG, "$name retrying after ${backoffMs}ms (attempt $attempt/$MAX_ATTEMPTS): ${lastError.message}")
                delay(backoffMs)
            }
            lastError
        }

    private suspend fun attemptGenerate(params: SongParams): GenerationResult {
        val settings = apiSettings.current
        val body = JSONObject().apply {
            put("model", settings.songGenModel)
            put("prompt", params.descriptions ?: "")
            put("lyrics", params.lyric)
            put("audio_setting", JSONObject().apply {
                put("sample_rate", 44100)
                put("bitrate", 256000)
                put("format", "mp3")
            })
        }.toString()

        Log.d(TAG, "$name → POST /v1/music_generation descriptions=${params.descriptions}")

        val request = Request.Builder()
            .url(settings.songGenBaseUrl)
            .apply { if (settings.songGenApiKey.isNotBlank()) header("Authorization", "Bearer ${settings.songGenApiKey}") }
            .post(body.toRequestBody(JSON))
            .build()

        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)

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
                        val responseJson = JSONObject(response.body?.string() ?: "{}")
                        val statusCode = responseJson.optJSONObject("base_resp")?.optInt("status_code", -1) ?: -1
                        if (statusCode != 0) {
                            val msg = responseJson.optJSONObject("base_resp")?.optString("status_msg") ?: "Unknown error"
                            Log.w(TAG, "$name ← error: $msg")
                            GenerationResult.Error(msg)
                        } else {
                            val hexAudio = responseJson.optJSONObject("data")?.optString("audio", "") ?: ""
                            if (hexAudio.isEmpty()) {
                                GenerationResult.Error("No audio data from $name")
                            } else {
                                val audioBytes = hexAudio.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                val file = File(cacheDir, "music_${System.currentTimeMillis()}.mp3")
                                file.writeBytes(audioBytes)
                                if (file.length() > 0) {
                                    Log.d(TAG, "$name ← ${file.length() / 1024}KB saved as ${file.name}")
                                    GenerationResult.Success(file, params)
                                } else {
                                    file.delete()
                                    GenerationResult.Error("No audio data from $name")
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                if (cont.isCancelled) return@suspendCancellableCoroutine
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
        if (isDefaultApi()) {
            streamFromDefaultApi(params).collect { emit(it) }
        } else {
            when (val result = generate(params)) {
                is GenerationResult.Success -> {
                    emit(StreamingChunk.Audio(result.audioFile, 0, params))
                    emit(StreamingChunk.Complete)
                }
                is GenerationResult.Error -> emit(StreamingChunk.Error(result.message))
            }
        }
    }

    private fun isDefaultApi(): Boolean =
        apiSettings.current.songGenBaseUrl.trimEnd('/') == BuildConfig.SONGGEN_BASE_URL.trimEnd('/')

    private fun streamingUrl(baseUrl: String): String =
        baseUrl.trimEnd('/').replace(Regex("/v1/music_generation$"), "") + "/generate_stream"

    private fun streamFromDefaultApi(params: SongParams): Flow<StreamingChunk> = flow {
        val settings = apiSettings.current
        val url = streamingUrl(settings.songGenBaseUrl)
        val body = JSONObject().apply {
            put("model", settings.songGenModel)
            put("prompt", params.descriptions ?: "")
            put("lyrics", params.lyric)
            put("audio_setting", JSONObject().apply {
                put("sample_rate", 44100)
                put("bitrate", 256000)
                put("format", "mp3")
            })
            if (params.auto_prompt_audio_type != null) put("auto_prompt_audio_type", params.auto_prompt_audio_type)
            put("generate_type", params.generate_type)
        }.toString()

        for (attempt in 1..MAX_ATTEMPTS) {
            Log.d(TAG, "$name → POST $url (attempt $attempt/$MAX_ATTEMPTS) descriptions=${params.descriptions}")
            val request = Request.Builder()
                .url(url)
                .apply { if (settings.songGenApiKey.isNotBlank()) header("Authorization", "Bearer ${settings.songGenApiKey}") }
                .post(body.toRequestBody(JSON))
                .build()

            var realBytesWritten = 0L
            var file: File? = null
            val errMsg: String? = try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string()?.take(300).orEmpty()
                        "HTTP ${response.code}${if (errBody.isNotEmpty()) ": $errBody" else ""}"
                    } else {
                        val out = File(cacheDir, "music_${System.currentTimeMillis()}.mp3")
                        file = out
                        val t0 = System.currentTimeMillis()
                        var firstByteMs = -1L
                        // Server sends 0x00 heartbeat bytes before chunk 0 to keep
                        // Cloudflare from 524'ing. Skip leading nulls until we see
                        // the start of MP3 data (ID3 tag or MPEG sync byte 0xFF).
                        var sawRealByte = false
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(out).use { sink ->
                                val buf = ByteArray(16 * 1024)
                                while (true) {
                                    val n = input.read(buf)
                                    if (n == -1) break
                                    if (firstByteMs < 0) {
                                        firstByteMs = System.currentTimeMillis() - t0
                                        Log.d(TAG, "$name: first bytes (heartbeat or audio) after ${firstByteMs}ms")
                                    }
                                    var start = 0
                                    if (!sawRealByte) {
                                        start = findMp3Start(buf, n)
                                        if (start < 0) continue
                                        sawRealByte = true
                                        val firstRealByteMs = System.currentTimeMillis() - t0
                                        Log.d(TAG, "$name: first real audio byte after ${firstRealByteMs}ms")
                                    }
                                    sink.write(buf, start, n - start)
                                    realBytesWritten += (n - start)
                                }
                            }
                        }
                        Log.d(TAG, "$name ← ${realBytesWritten / 1024}KB streamed in ${System.currentTimeMillis() - t0}ms")
                        null
                    }
                }
            } catch (e: IOException) {
                "IO error: ${e.message}"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }

            if (errMsg == null && realBytesWritten > 0 && file != null) {
                emit(StreamingChunk.Audio(file!!, 0, params))
                emit(StreamingChunk.Complete)
                return@flow
            }

            file?.takeIf { it.exists() }?.delete()
            val retriable = errMsg != null && isRetriableMessage(errMsg)
            if (!retriable || attempt == MAX_ATTEMPTS) {
                Log.w(TAG, "$name stream failed (attempt $attempt): $errMsg")
                emit(StreamingChunk.Error(errMsg ?: "Empty stream"))
                return@flow
            }
            val backoffMs = 3000L * (1 shl (attempt - 1))
            Log.w(TAG, "$name retrying stream after ${backoffMs}ms: $errMsg")
            delay(backoffMs)
        }
    }

    override suspend fun healthCheck(): Boolean = true

    private fun isRetriableMessage(msg: String): Boolean =
        msg.startsWith("HTTP 429") ||
            msg.startsWith("HTTP 500") ||
            msg.startsWith("HTTP 502") ||
            msg.startsWith("HTTP 503") ||
            msg.startsWith("HTTP 504") ||
            msg.startsWith("IO error")

    private fun findMp3Start(buffer: ByteArray, limit: Int): Int {
        for (i in 0 until limit) {
            val byte = buffer[i].toInt() and 0xFF
            if (byte == 0xFF) return i
            if (i <= limit - 3 &&
                buffer[i] == 'I'.code.toByte() &&
                buffer[i + 1] == 'D'.code.toByte() &&
                buffer[i + 2] == '3'.code.toByte()
            ) {
                return i
            }
        }
        return -1
    }

    companion object {
        private const val TAG = "SongGenerationBackend"
        private const val MAX_ATTEMPTS = 3
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

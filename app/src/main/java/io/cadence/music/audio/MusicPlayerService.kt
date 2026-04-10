package io.cadence.music.audio

import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject lateinit var bufferManager: AudioBufferManager
    @Inject lateinit var playerNotification: PlayerNotification

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var feedJob: Job? = null
    private var scheduleJob: Job? = null
    private var isPlaying = false

    // Files queued into ExoPlayer in order — head is the currently playing file
    private val enqueuedFiles = ArrayDeque<File>()

    override fun onCreate() {
        super.onCreate()

        playerNotification.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                PlayerNotification.NOTIFICATION_ID,
                playerNotification.buildMinimalNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(PlayerNotification.NOTIFICATION_ID, playerNotification.buildMinimalNotification(this))
        }

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Previous song finished — delete its file immediately to free space
                enqueuedFiles.removeFirstOrNull()?.let { played ->
                    if (played.delete()) Log.d(TAG, "Deleted: ${played.name}")
                }
                // Schedule next generation based on the now-current song's duration
                enqueuedFiles.firstOrNull()?.let { scheduleNextGeneration(it) }
                // Feed the already-generated clip (from the scheduled generation of the previous song)
                feedNextChunk()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} — ${error.message}")
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()

        val notification = playerNotification.buildNotification(this, mediaSession)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                PlayerNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(PlayerNotification.NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_PLAY && !isPlaying) {
            isPlaying = true
            startFeedLoop()
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        isPlaying = false
        feedJob?.cancel()
        scheduleJob?.cancel()
        enqueuedFiles.forEach { it.delete() }
        enqueuedFiles.clear()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun startFeedLoop() {
        feedJob?.cancel()
        feedJob = scope.launch {
            val first = bufferManager.takeNext() ?: run {
                Log.w(TAG, "Buffer returned null — no audio to play")
                return@launch
            }
            enqueueFile(first)
            player.prepare()
            player.play()
            Log.d(TAG, "Playback started: ${first.name}")
            // Schedule next generation based on this song's duration
            scheduleNextGeneration(first)
            // Pre-enqueue the already-buffered second clip for gapless transition
            feedNextChunk()
        }
    }

    /**
     * Reads [file]'s duration and triggers generation of the NEXT song
     * [GENERATION_LEAD_MS] before the current one ends, so it's ready in time.
     */
    private fun scheduleNextGeneration(file: File) {
        scheduleJob?.cancel()
        scheduleJob = scope.launch(Dispatchers.IO) {
            val durationMs = getAudioDurationMs(file)
            val delayMs = (durationMs - GENERATION_LEAD_MS).coerceAtLeast(0L)
            Log.d(TAG, "${file.name}: duration=${durationMs}ms → generating next in ${delayMs}ms")
            delay(delayMs)
            bufferManager.onChunkStarted()
        }
    }

    private fun feedNextChunk() {
        scope.launch {
            val next = bufferManager.takeNext() ?: return@launch
            enqueueFile(next)
        }
    }

    private fun enqueueFile(file: File) {
        val item = MediaItem.fromUri(Uri.fromFile(file))
        val wasEnded = player.playbackState == Player.STATE_ENDED
        player.addMediaItem(item)
        enqueuedFiles.addLast(file)
        Log.d(TAG, "Queued: ${file.name}")
        if (wasEnded) {
            // Playlist ran dry before this clip was ready — seek to it and resume.
            player.seekTo(player.mediaItemCount - 1, 0)
            player.prepare()
            player.play()
            scheduleNextGeneration(file)
            feedNextChunk()
            Log.d(TAG, "Resumed after gap: ${file.name}")
        }
    }

    private fun getAudioDurationMs(file: File): Long = try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: DEFAULT_DURATION_MS
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not read duration for ${file.name}: ${e.message}")
        DEFAULT_DURATION_MS
    }

    companion object {
        const val ACTION_PLAY = "io.cadence.music.action.PLAY"
        private const val TAG = "MusicPlayerService"
        /** Start generating the next song this many ms before the current one ends. */
        private const val GENERATION_LEAD_MS = 60_000L
        /** Fallback duration if metadata is unreadable. */
        private const val DEFAULT_DURATION_MS = 240_000L
    }
}

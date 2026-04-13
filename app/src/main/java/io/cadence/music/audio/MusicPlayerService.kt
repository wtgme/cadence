package io.cadence.music.audio

import android.content.Intent
import android.content.pm.ServiceInfo
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
    private var positionJob: Job? = null
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
                // Only react to auto-advance (previous chunk finished). Ignore
                // REASON_PLAYLIST_CHANGED (fires when we append the very first
                // item and would otherwise remove the currently-playing chunk
                // from enqueuedFiles, shifting file tracking off-by-one).
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    Log.d(TAG, "Transition reason=$reason ignored")
                    return
                }
                enqueuedFiles.removeFirstOrNull()?.let { played ->
                    if (played.delete()) Log.d(TAG, "Deleted: ${played.name}")
                }
                Log.d(TAG, "Chunk auto-advanced — pre-fetching next (queued=${enqueuedFiles.size})")
                // Feed the next buffered chunk into ExoPlayer immediately.
                // The buffer worker self-triggers new song generation after each
                // stream completes — no explicit onChunkStarted() needed here.
                feedNextChunk()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} — ${error.message}")
                // Don't let one bad clip stall the chain — skip past it and
                // keep the generation loop alive.
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
                feedNextChunk()
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
        when (intent?.action) {
            ACTION_PLAY -> if (!isPlaying) { isPlaying = true; startFeedLoop(); startPositionUpdates() }
            ACTION_SKIP_NEXT -> skipToNext()
            ACTION_SKIP_PREV -> {
                player.seekTo(0)
                if (player.playbackState == Player.STATE_ENDED) {
                    player.prepare()
                    player.play()
                }
            }
            ACTION_SEEK -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POSITION_MS, 0L)
                player.seekTo(pos)
            }
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        isPlaying = false
        feedJob?.cancel()
        positionJob?.cancel()
        bufferManager.updateProgress(0L, 0L)
        enqueuedFiles.forEach { it.delete() }
        enqueuedFiles.clear()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.let { if (it == C.TIME_UNSET) 0L else it }
                bufferManager.updateProgress(pos, dur)
                delay(500)
            }
        }
    }

    private fun skipToNext() {
        if (player.hasNextMediaItem()) {
            // Song already pre-loaded in ExoPlayer — instant skip
            player.seekToNextMediaItem()
        } else {
            // Next song is still being generated. Stop the current song immediately
            // so the user sees something happen, then let the already-running
            // feedNextChunk() coroutine deliver the next song when ready.
            // enqueueFile() handles STATE_IDLE and will auto-resume + pre-fetch.
            enqueuedFiles.forEach { it.delete() }
            enqueuedFiles.clear()
            player.clearMediaItems()
            player.stop()
            bufferManager.notifySkipToNext()
            // Do NOT call feedNextChunk() here — one is already pending from
            // startFeedLoop() or onMediaItemTransition. Adding another creates a
            // second competing consumer that consumes songs out of order.
        }
    }

    private fun startFeedLoop() {
        feedJob?.cancel()
        feedJob = scope.launch {
            val first = bufferManager.takeNext() ?: run {
                Log.w(TAG, "Buffer returned null — no audio to play")
                return@launch
            }
            // enqueueFile() detects STATE_IDLE and handles prepare/play/feedNextChunk
            enqueueFile(first)
            Log.d(TAG, "Feed loop started: ${first.name}")
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
        // Resume when: playlist ran dry naturally (ENDED) OR player was stopped by a
        // skip-while-generating call (IDLE after clearMediaItems + stop).
        val needsResume = player.playbackState == Player.STATE_ENDED ||
                          player.playbackState == Player.STATE_IDLE
        player.addMediaItem(item)
        enqueuedFiles.addLast(file)
        Log.d(TAG, "Queued: ${file.name}")
        if (needsResume) {
            player.seekTo(player.mediaItemCount - 1, 0)
            player.prepare()
            player.play()
            feedNextChunk()
            Log.d(TAG, "Resumed after gap: ${file.name}")
        }
    }

    companion object {
        const val ACTION_PLAY = "io.cadence.music.action.PLAY"
        const val ACTION_SKIP_NEXT = "io.cadence.music.action.SKIP_NEXT"
        const val ACTION_SKIP_PREV = "io.cadence.music.action.SKIP_PREV"
        const val ACTION_SEEK = "io.cadence.music.action.SEEK"
        const val EXTRA_SEEK_POSITION_MS = "seek_position_ms"
        private const val TAG = "MusicPlayerService"
    }
}

package io.cadence.music.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaStyleNotificationHelper
import io.cadence.music.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerNotification @Inject constructor() {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Now Playing",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Cadence music playback"
            setShowBadge(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    fun buildMinimalNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Cadence")
            .setContentText("Preparing music…")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun buildNotification(context: Context, mediaSession: androidx.media3.session.MediaSession): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Cadence")
            .setContentText("Playing your generated track")
            .setOngoing(true)
            .setSilent(true)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))
            .build()
    }

    companion object {
        const val CHANNEL_ID = "cadence_playback"
        const val NOTIFICATION_ID = 1002
    }
}

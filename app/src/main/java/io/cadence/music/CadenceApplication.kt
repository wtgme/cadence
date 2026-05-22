package io.cadence.music

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class CadenceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sweepStaleAudioCache()
    }

    /**
     * Delete any .mp3 files left in the audio cache from a previous run that didn't
     * exit gracefully (force-quit, crash, OS process kill). On fresh launch the buffer
     * queue is always empty, so anything still on disk is orphaned — safe to delete
     * unconditionally. Without this, force-quit users accumulate ~30–60 MB of audio
     * per long session that the next `prime()` would only clean if they start a new
     * playback session.
     */
    private fun sweepStaleAudioCache() {
        val audioDir = File(cacheDir, "audio")
        if (!audioDir.isDirectory) return
        var removed = 0
        audioDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".mp3") && file.delete()) removed++
        }
        if (removed > 0) Log.d(TAG, "Swept $removed stale audio file(s) from previous run")
    }

    private companion object {
        private const val TAG = "CadenceApplication"
    }
}

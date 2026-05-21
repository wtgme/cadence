package io.cadence.music.data.sensor

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.data.model.MotionActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the user's current on-device-classified physical activity.
 *
 * Bridges Google Play Services' [ActivityRecognitionClient.requestActivityUpdates],
 * which delivers an [ActivityRecognitionResult] via a [PendingIntent] every
 * [INTERVAL_MS] (or sooner when the classifier changes its decision). Only samples
 * with confidence ≥ [MIN_CONFIDENCE] are surfaced — lower confidence publishes
 * `null`, letting `SceneDetector` fall back to GPS/HR heuristics.
 *
 * Mirrors the iOS `MotionActivityRepository` (CMMotionActivityManager).
 */
@Singleton
class MotionActivityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: ActivityRecognitionClient,
) {
    private val _activity = MutableStateFlow<MotionActivity?>(null)
    val activity: StateFlow<MotionActivity?> = _activity

    private var receiver: BroadcastReceiver? = null
    private var pendingIntent: PendingIntent? = null

    fun start() {
        if (receiver != null) return
        if (!hasPermission()) {
            Log.d(TAG, "ACTIVITY_RECOGNITION not granted — motion classifier inactive")
            return
        }

        val filter = IntentFilter(ACTION_ACTIVITY_UPDATE)
        val rcvr = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (!ActivityRecognitionResult.hasResult(intent)) return
                val result = ActivityRecognitionResult.extractResult(intent) ?: return
                val mostProbable = result.mostProbableActivity ?: return
                if (mostProbable.confidence < MIN_CONFIDENCE) {
                    if (_activity.value != null) _activity.value = null
                    return
                }
                val mapped = map(mostProbable.type)
                if (mapped != _activity.value) {
                    Log.d(TAG, "Motion → $mapped (confidence=${mostProbable.confidence})")
                    _activity.value = mapped
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            rcvr,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiver = rcvr

        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(ACTION_ACTIVITY_UPDATE).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        pendingIntent = pi
        try {
            client.requestActivityUpdates(INTERVAL_MS, pi)
        } catch (e: SecurityException) {
            Log.w(TAG, "requestActivityUpdates failed", e)
        }
    }

    fun stop() {
        pendingIntent?.let { pi ->
            try { client.removeActivityUpdates(pi) } catch (_: Exception) {}
            pi.cancel()
        }
        receiver?.let {
            try { context.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        pendingIntent = null
        receiver = null
    }

    private fun hasPermission(): Boolean =
        // Android 10+ requires ACTIVITY_RECOGNITION at runtime; older versions grant implicitly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun map(detectedActivity: Int): MotionActivity = when (detectedActivity) {
        DetectedActivity.RUNNING -> MotionActivity.RUNNING
        DetectedActivity.ON_BICYCLE -> MotionActivity.CYCLING
        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> MotionActivity.WALKING
        DetectedActivity.IN_VEHICLE -> MotionActivity.AUTOMOTIVE
        DetectedActivity.STILL -> MotionActivity.STATIONARY
        else -> MotionActivity.UNKNOWN
    }

    private companion object {
        private const val TAG = "MotionActivityRepo"
        private const val ACTION_ACTIVITY_UPDATE = "io.cadence.music.ACTIVITY_RECOGNITION_UPDATE"
        private const val REQUEST_CODE = 0xCADE
        private const val INTERVAL_MS = 30_000L
        private const val MIN_CONFIDENCE = 60
    }
}

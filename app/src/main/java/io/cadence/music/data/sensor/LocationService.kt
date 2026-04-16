package io.cadence.music.data.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import io.cadence.music.R
import io.cadence.music.data.model.Scene
import javax.inject.Inject

data class LocationData(
    val speedKmh: Float,
    val latitude: Double,
    val longitude: Double,
)

@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var locationRepository: LocationRepository

    private var lastLocation: Location? = null
    private var currentPriority: Int = Priority.PRIORITY_BALANCED_POWER_ACCURACY

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val speedKmh = computeSpeed(location)
            locationRepository.update(
                LocationData(
                    speedKmh = speedKmh,
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            )
            lastLocation = location
        }
    }

    /**
     * Prefer the GPS-reported speed (m/s → km/h) when the device reports it as accurate.
     * Fall back to computing speed from the distance between the last two positions,
     * which is more reliable at low speeds and during GPS warm-up.
     */
    private fun computeSpeed(location: Location): Float {
        val prev = lastLocation
        val gpsSpeed = location.speed * 3.6f  // m/s → km/h

        // Use GPS speed if it's available and plausible (>0.5 km/h)
        if (location.hasSpeed() && gpsSpeed > 0.5f) {
            return gpsSpeed
        }

        // Compute from position delta
        if (prev != null && location.time > prev.time) {
            val distanceM = prev.distanceTo(location)
            val elapsedSec = (location.time - prev.time) / 1000f
            if (elapsedSec > 0) {
                val derivedKmh = (distanceM / elapsedSec) * 3.6f
                // Sanity check: ignore implausibly high values (GPS jump)
                if (derivedKmh < 200f) return derivedKmh
            }
        }
        return gpsSpeed
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        requestLocationUpdates()
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_SCENE -> {
                val sceneName = intent.getStringExtra(EXTRA_SCENE)
                val scene = sceneName?.let { runCatching { Scene.valueOf(it) }.getOrNull() }
                updateLocationPriority(scene)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Switch GPS accuracy based on detected scene. Active scenes (running, cycling,
     * commuting) use high-accuracy at 5s intervals; sedentary scenes use balanced-power
     * at 10s intervals — significantly reducing GPS radio usage and heat.
     */
    fun updateLocationPriority(scene: Scene?) {
        val needsHighAccuracy = scene in HIGH_ACCURACY_SCENES
        val targetPriority = if (needsHighAccuracy)
            Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        if (targetPriority == currentPriority) return
        currentPriority = targetPriority
        Log.d(TAG, "Switching GPS priority: ${if (needsHighAccuracy) "HIGH_ACCURACY (5s)" else "BALANCED_POWER (10s)"} for scene=$scene")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        val fineGranted = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarseGranted = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            Log.e(TAG, "Location permission not granted — cannot start updates")
            stopSelf()
            return
        }
        val intervalMs = if (currentPriority == Priority.PRIORITY_HIGH_ACCURACY) 5_000L else 10_000L
        val minIntervalMs = if (currentPriority == Priority.PRIORITY_HIGH_ACCURACY) 3_000L else 5_000L
        val request = LocationRequest.Builder(currentPriority, intervalMs)
            .setMinUpdateIntervalMillis(minIntervalMs)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        Log.d(TAG, "Location updates requested: interval=${intervalMs}ms, priority=$currentPriority")
    }

    private fun buildNotification(): Notification {
        val channelId = "cadence_location"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Location tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Tracking location for scene detection")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE_SCENE = "io.cadence.music.action.UPDATE_SCENE"
        const val EXTRA_SCENE = "scene"
        private const val TAG = "LocationService"
        /** Scenes that need fast, precise GPS for speed-based detection. */
        private val HIGH_ACCURACY_SCENES = setOf(Scene.RUNNING, Scene.CYCLING, Scene.COMMUTING)
    }
}

package io.cadence.music.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Single source of truth for location data.
 * LocationService writes into it; SensorStateCollector reads from it.
 * Eliminates the lateinit wiring that caused the crash.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
) {
    private val _locationData = MutableStateFlow(LocationData(0f, 0.0, 0.0))
    val locationData: StateFlow<LocationData> = _locationData

    fun update(data: LocationData) {
        _locationData.value = data
    }

    /**
     * Cold-start fast path. Returns the current cached location if we already have a real
     * fix (non-zero coords); otherwise asks FusedLocationProviderClient for the system's
     * last-known location, which is served from cache and typically returns within ~100ms.
     *
     * Avoids blocking Step 1 / weather refresh on a fresh GPS fix (can take 5–15s).
     */
    suspend fun currentOrLastKnown(): LocationData {
        val current = _locationData.value
        if (current.latitude != 0.0 || current.longitude != 0.0) return current

        if (!hasLocationPermission()) return current

        val last = withTimeoutOrNull(LAST_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        return last?.let {
            LocationData(speedKmh = 0f, latitude = it.latitude, longitude = it.longitude).also { d ->
                _locationData.value = d
            }
        } ?: current
    }

    private fun hasLocationPermission(): Boolean {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private companion object {
        const val LAST_LOCATION_TIMEOUT_MS = 1_500L
    }
}

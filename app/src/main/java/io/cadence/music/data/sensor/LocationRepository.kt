package io.cadence.music.data.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for location data.
 * LocationService writes into it; SensorStateCollector reads from it.
 * Eliminates the lateinit wiring that caused the crash.
 */
@Singleton
class LocationRepository @Inject constructor() {
    private val _locationData = MutableStateFlow(LocationData(0f, 0.0, 0.0))
    val locationData: StateFlow<LocationData> = _locationData

    fun update(data: LocationData) {
        _locationData.value = data
    }
}

package io.cadence.music.data.sensor

import io.cadence.music.data.model.SensorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorStateCollector @Inject constructor(
    private val locationRepository: LocationRepository,
    private val healthDataManager: HealthDataManager,
    private val sleepRepository: SleepRepository,
    private val healthExtrasRepository: HealthExtrasRepository,
    private val weatherRepository: WeatherRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sensorState: Flow<SensorState> = combine(
        locationRepository.locationData,
        healthDataManager.heartRate,
        healthDataManager.activityMinutesToday,
        sleepRepository.sleepScore,
        healthExtrasRepository.extras,
        weatherRepository.weather,
    ) { values ->
        val location = values[0] as LocationData
        val heartRate = values[1] as Int
        val activityMins = values[2] as Int
        val sleep = values[3] as SleepScore
        val extras = values[4] as HealthExtras
        val weather = values[5] as String

        // Only compute energy when we have at least one real signal.
        // 0 means "no data" — the UI shows "—" in that case.
        val hasHr       = heartRate > 0
        val hasActivity = activityMins > 0
        val hasSleep    = sleep.durationHours > 0f
        val energyScore = if (!hasHr && !hasActivity && !hasSleep) {
            0
        } else {
            var score = 50
            if (hasHr)       score += ((heartRate - 70) / 5).coerceIn(-15, 20)
            if (hasActivity) score += (activityMins / 3).coerceAtMost(20)
            if (hasSleep)    score += ((sleep.score - 50) / 4).coerceIn(-15, 15)
            score.coerceIn(1, 100) // 0 is reserved for "no data"
        }

        SensorState(
            speedKmh = location.speedKmh,
            heartRate = heartRate,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            weather = weather,
            latitude = location.latitude,
            longitude = location.longitude,
            energyScore = energyScore,
            sleepScore = if (hasSleep) sleep.score else 0,
            sleepHours = sleep.durationHours,
            sleepDeepPct = sleep.deepSleepPct,
            sleepRemPct = sleep.remSleepPct,
            activityMinutesToday = activityMins,
            spo2 = extras.spo2,
            bloodPressureSystolic = extras.bloodPressureSystolic,
            bloodPressureDiastolic = extras.bloodPressureDiastolic,
            bodyTemperature = extras.bodyTemperature,
            floorsClimbed = extras.floorsClimbed,
            caloriesBurned = extras.caloriesBurned,
            stepsToday = extras.stepsToday,
            distanceKm = extras.distanceKm,
        )
    }

    fun start() {
        healthDataManager.start()
        healthExtrasRepository.start()
        scope.launch { sleepRepository.refresh() }
        
        // Refresh weather when location changes
        scope.launch {
            locationRepository.locationData.collect { location ->
                weatherRepository.refresh(location.latitude, location.longitude)
            }
        }
    }

    fun stop() {
        healthDataManager.stop()
        healthExtrasRepository.stop()
    }

    suspend fun refreshAll() {
        healthDataManager.refresh()
        healthExtrasRepository.refresh()
        sleepRepository.refresh()
        val loc = locationRepository.locationData.first()
        weatherRepository.refresh(loc.latitude, loc.longitude, force = true)
    }
}

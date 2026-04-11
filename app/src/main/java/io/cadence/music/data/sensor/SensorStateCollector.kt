package io.cadence.music.data.sensor

import io.cadence.music.data.model.SensorState
import io.cadence.music.domain.ReadinessCalculator
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
    private val readinessCalculator: ReadinessCalculator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val healthDiagnostic = healthDataManager.diagnostic

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

        val hasSleep = sleep.durationHours > 0f

        val readiness = readinessCalculator.compute(
            ReadinessCalculator.Inputs(
                sleepScore = if (hasSleep) sleep.score else 0,
                hrvToday = extras.hrvRmssd,
                hrvBaseline = extras.hrvBaseline,
                restingHrToday = extras.restingHr,
                restingHrBaseline = extras.restingHrBaseline,
                yesterdayActiveKcal = extras.yesterdayActiveKcal,
                activeKcalBaseline = extras.activeKcalBaseline,
            )
        )

        SensorState(
            speedKmh = location.speedKmh,
            heartRate = heartRate,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            weather = weather,
            latitude = location.latitude,
            longitude = location.longitude,
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
            readinessScore = readiness.score,
            readinessBreakdown = readiness.breakdown,
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

    suspend fun hasHeartRatePermission(): Boolean = healthDataManager.hasHeartRatePermission()
}

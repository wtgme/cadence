package io.cadence.music.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class HealthExtras(
    val spo2: Int = 0,
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val bodyTemperature: Float = 0f,
    val floorsClimbed: Int = 0,
    val caloriesBurned: Float = 0f,
    val stepsToday: Long = 0,
    val distanceKm: Float = 0f,
)

@Singleton
class HealthExtrasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _extras = MutableStateFlow(HealthExtras())
    val extras: StateFlow<HealthExtras> = _extras

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    fun start() {
        pollJob?.cancel()
        pollJob = scope.launch {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                Log.w(TAG, "Health Connect not available — extras will remain empty")
                return@launch
            }
            val client = HealthConnectClient.getOrCreate(context)
            Log.d(TAG, "Health Connect extras polling started (every ${POLL_INTERVAL_MS / 1000}s)")

            var granted = emptySet<String>()
            while (isActive) {
                try {
                    granted = client.permissionController.getGrantedPermissions()
                    readExtras(client, granted)
                } catch (e: Exception) {
                    if (isQuotaExceeded(e)) {
                        Log.w(TAG, "Quota exceeded during extras poll, backing off")
                    } else {
                        Log.e(TAG, "Extras polling failed", e)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
    }

    suspend fun refresh() {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            readExtras(client, granted)
            Log.d(TAG, "Extras refreshed")
        } catch (e: Exception) {
            if (isQuotaExceeded(e)) {
                Log.w(TAG, "Extras refresh failed: quota exceeded")
            } else {
                Log.w(TAG, "Extras refresh failed", e)
            }
        }
    }

    private suspend fun readExtras(client: HealthConnectClient, granted: Set<String>) {
        val now = Instant.now()
        val twentyFourHoursAgo = now.minusSeconds(86_400)
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

        // Point-in-time metrics: Read latest records
        if (HealthPermission.getReadPermission(OxygenSaturationRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(twentyFourHoursAgo, now),
                    ascendingOrder = false,
                    pageSize = 1
                ))
                val latest = response.records.firstOrNull()
                if (latest != null) {
                    _extras.value = _extras.value.copy(spo2 = latest.percentage.value.toInt())
                }
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "SpO2: Quota exceeded")
                else Log.w(TAG, "SpO2 read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        if (HealthPermission.getReadPermission(BloodPressureRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(twentyFourHoursAgo, now),
                    ascendingOrder = false,
                    pageSize = 1
                ))
                val latest = response.records.firstOrNull()
                if (latest != null) {
                    _extras.value = _extras.value.copy(
                        bloodPressureSystolic = latest.systolic.inMillimetersOfMercury.toInt(),
                        bloodPressureDiastolic = latest.diastolic.inMillimetersOfMercury.toInt(),
                    )
                }
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "BP: Quota exceeded")
                else Log.w(TAG, "BP read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        if (HealthPermission.getReadPermission(BodyTemperatureRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(twentyFourHoursAgo, now),
                    ascendingOrder = false,
                    pageSize = 1
                ))
                val latest = response.records.firstOrNull()
                if (latest != null) {
                    _extras.value = _extras.value.copy(
                        bodyTemperature = latest.temperature.inCelsius.toFloat(),
                    )
                }
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "BodyTemp: Quota exceeded")
                else Log.w(TAG, "Body temp read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        // Manual summation for cumulative metrics to avoid unresolved aggregate constant issues
        // while still providing deduplication by using the latest records per time window.
        
        if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ))
                _extras.value = _extras.value.copy(stepsToday = response.records.sumOf { it.count })
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "Steps: Quota exceeded")
                else Log.w(TAG, "Steps read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ))
                val totalM = response.records.sumOf { it.distance.inMeters }
                _extras.value = _extras.value.copy(distanceKm = (totalM / 1000.0).toFloat())
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "Distance: Quota exceeded")
                else Log.w(TAG, "Distance read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ))
                val totalKcal = response.records.sumOf { it.energy.inKilocalories }
                _extras.value = _extras.value.copy(caloriesBurned = totalKcal.toFloat())
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "Calories: Quota exceeded")
                else Log.w(TAG, "Calories read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        // Fallback: many devices/apps (Google Fit, Samsung Health) only write
        // TotalCaloriesBurnedRecord, not ActiveCaloriesBurnedRecord.
        if (_extras.value.caloriesBurned == 0f &&
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ))
                val totalKcal = response.records.sumOf { it.energy.inKilocalories }
                if (totalKcal > 0) {
                    _extras.value = _extras.value.copy(caloriesBurned = totalKcal.toFloat())
                }
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "TotalCalories: Quota exceeded")
                else Log.w(TAG, "Total calories read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        if (HealthPermission.getReadPermission(FloorsClimbedRecord::class) in granted) {
            try {
                val response = client.readRecords(ReadRecordsRequest(
                    recordType = FloorsClimbedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ))
                val total = response.records.sumOf { it.floors }.toInt()
                _extras.value = _extras.value.copy(floorsClimbed = total)
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "Floors: Quota exceeded")
                else Log.w(TAG, "Floors read failed", e)
            }
        }
    }

    private fun isQuotaExceeded(e: Exception): Boolean {
        return e.message?.contains("quota exceeded", ignoreCase = true) == true ||
                e.cause?.message?.contains("quota exceeded", ignoreCase = true) == true
    }

    companion object {
        private const val TAG = "HealthExtrasRepo"
        private const val POLL_INTERVAL_MS = 300_000L
        private const val EXTRA_REQUEST_DELAY_MS = 500L
    }
}

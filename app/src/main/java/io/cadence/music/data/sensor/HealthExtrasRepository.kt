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
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
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
    // Readiness inputs
    val restingHr: Int = 0,               // bpm, 0 = unknown
    val restingHrBaseline: Float = 0f,    // 14-day mean, 0 = unknown
    val hrvRmssd: Float = 0f,             // ms RMSSD, 0 = unknown
    val hrvBaseline: Float = 0f,          // 14-day mean, 0 = unknown
    val yesterdayActiveKcal: Float = 0f,  // 0 = unknown
    val activeKcalBaseline: Float = 0f,   // 14-day mean, 0 = unknown
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
        // Use today's totals once we're a few hours past midnight; otherwise fall back to a
        // rolling 24h window so cumulative metrics (steps/calories/distance) aren't empty
        // right after midnight. Threshold: at least 3h of "today" must have elapsed.
        val threeHoursAfterStartOfDay = startOfDay.plusSeconds(3 * 3600)
        val cumulativeStart = if (now.isAfter(threeHoursAfterStartOfDay)) startOfDay else twentyFourHoursAgo

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
                    timeRangeFilter = TimeRangeFilter.between(cumulativeStart, now),
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
                    timeRangeFilter = TimeRangeFilter.between(cumulativeStart, now),
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
                    timeRangeFilter = TimeRangeFilter.between(cumulativeStart, now),
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
                    timeRangeFilter = TimeRangeFilter.between(cumulativeStart, now),
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
                    timeRangeFilter = TimeRangeFilter.between(cumulativeStart, now),
                ))
                val total = response.records.sumOf { it.floors }.toInt()
                _extras.value = _extras.value.copy(floorsClimbed = total)
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) Log.w(TAG, "Floors: Quota exceeded")
                else Log.w(TAG, "Floors read failed", e)
            }
            delay(EXTRA_REQUEST_DELAY_MS)
        }

        // ——— Readiness inputs ———
        val baselineStart = now.minusSeconds(BASELINE_DAYS * 86_400L)
        val yesterdayStart = LocalDate.now().minusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        val yesterdayEnd = startOfDay

        readRestingHeartRate(client, granted, baselineStart, now)
        delay(EXTRA_REQUEST_DELAY_MS)
        readHrv(client, granted, baselineStart, now)
        delay(EXTRA_REQUEST_DELAY_MS)
        readActiveKcalBaseline(client, granted, baselineStart, yesterdayStart, yesterdayEnd)
    }

    private suspend fun readRestingHeartRate(
        client: HealthConnectClient,
        granted: Set<String>,
        baselineStart: Instant,
        now: Instant,
    ) {
        if (HealthPermission.getReadPermission(RestingHeartRateRecord::class) !in granted) return
        try {
            val response = client.readRecords(ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(baselineStart, now),
            ))
            val records = response.records
            if (records.isEmpty()) return
            val latest = records.maxByOrNull { it.time }
            val todayBpm = latest?.beatsPerMinute?.toInt() ?: 0
            // Baseline = mean of all readings EXCLUDING the latest (so "today vs history")
            val historyBpms = records
                .filter { it != latest }
                .map { it.beatsPerMinute.toInt() }
            val baseline = if (historyBpms.isNotEmpty()) historyBpms.average().toFloat() else 0f
            _extras.value = _extras.value.copy(
                restingHr = todayBpm,
                restingHrBaseline = baseline,
            )
            Log.d(TAG, "Resting HR: today=$todayBpm, baseline=${"%.1f".format(baseline)} (${historyBpms.size} samples)")
        } catch (e: Exception) {
            if (isQuotaExceeded(e)) Log.w(TAG, "RestingHR: Quota exceeded")
            else Log.w(TAG, "RestingHR read failed", e)
        }
    }

    private suspend fun readHrv(
        client: HealthConnectClient,
        granted: Set<String>,
        baselineStart: Instant,
        now: Instant,
    ) {
        if (HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) !in granted) return
        try {
            val response = client.readRecords(ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(baselineStart, now),
            ))
            val records = response.records
            if (records.isEmpty()) return
            val latest = records.maxByOrNull { it.time }
            val todayMs = latest?.heartRateVariabilityMillis?.toFloat() ?: 0f
            val history = records
                .filter { it != latest }
                .map { it.heartRateVariabilityMillis.toFloat() }
            val baseline = if (history.isNotEmpty()) history.average().toFloat() else 0f
            _extras.value = _extras.value.copy(
                hrvRmssd = todayMs,
                hrvBaseline = baseline,
            )
            Log.d(TAG, "HRV: today=${"%.1f".format(todayMs)}ms, baseline=${"%.1f".format(baseline)}ms (${history.size} samples)")
        } catch (e: Exception) {
            if (isQuotaExceeded(e)) Log.w(TAG, "HRV: Quota exceeded")
            else Log.w(TAG, "HRV read failed", e)
        }
    }

    /** Reads yesterday's active kcal and computes a 14-day daily mean. */
    private suspend fun readActiveKcalBaseline(
        client: HealthConnectClient,
        granted: Set<String>,
        baselineStart: Instant,
        yesterdayStart: Instant,
        yesterdayEnd: Instant,
    ) {
        if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) !in granted) return
        try {
            val response = client.readRecords(ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(baselineStart, yesterdayEnd),
            ))
            if (response.records.isEmpty()) return
            val yesterdayKcal = response.records
                .filter { it.startTime >= yesterdayStart && it.endTime <= yesterdayEnd }
                .sumOf { it.energy.inKilocalories }
                .toFloat()
            // Daily mean across the baseline window (excluding today so "yesterday vs normal day")
            val days = BASELINE_DAYS.toFloat().coerceAtLeast(1f)
            val totalKcal = response.records.sumOf { it.energy.inKilocalories }.toFloat()
            val dailyMean = totalKcal / days
            _extras.value = _extras.value.copy(
                yesterdayActiveKcal = yesterdayKcal,
                activeKcalBaseline = dailyMean,
            )
            Log.d(TAG, "Active kcal: yesterday=${"%.0f".format(yesterdayKcal)}, dailyBaseline=${"%.0f".format(dailyMean)}")
        } catch (e: Exception) {
            if (isQuotaExceeded(e)) Log.w(TAG, "Kcal baseline: Quota exceeded")
            else Log.w(TAG, "Kcal baseline read failed", e)
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
        /** Window size for RHR/HRV/kcal baselines. */
        private const val BASELINE_DAYS = 14L
    }
}

package io.cadence.music.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
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

@Singleton
class HealthDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate

    private val _activityMinutesToday = MutableStateFlow(0)
    val activityMinutesToday: StateFlow<Int> = _activityMinutesToday

    /** Human-readable diagnostic from the most recent refresh: record counts by type. */
    private val _diagnostic = MutableStateFlow<String?>(null)
    val diagnostic: StateFlow<String?> = _diagnostic

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    fun start() {
        pollJob?.cancel()
        pollJob = scope.launch {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                Log.w(TAG, "Health Connect not available")
                return@launch
            }
            val client = HealthConnectClient.getOrCreate(context)
            Log.d(TAG, "Health Connect biometrics polling started")

            var slowTick = 0
            var granted = emptySet<String>()

            while (isActive) {
                try {
                    // Refresh permissions occasionally
                    if (slowTick % SLOW_POLL_EVERY_N == 0) {
                        granted = client.permissionController.getGrantedPermissions()
                    }

                    readHeartRate(client, granted)

                    // Exercise sessions change at most once per workout — refresh every 5 minutes
                    if (slowTick % SLOW_POLL_EVERY_N == 0) {
                        syncActivityAndEnergy(client, granted)
                    }
                    slowTick++
                } catch (e: Exception) {
                    if (e.message?.contains("quota", ignoreCase = true) == true) {
                        Log.w(TAG, "Health Connect quota exceeded, backing off")
                        delay(POLL_INTERVAL_MS) // Additional backoff delay
                    } else {
                        Log.e(TAG, "Health Connect polling failed", e)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun refresh() {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            Log.w(TAG, "Health Connect not available (status=$sdkStatus)")
            _diagnostic.value = "Health Connect not available on this device (status=$sdkStatus)"
            return
        }
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            Log.d(TAG, "Granted HC permissions (${granted.size}): $granted")
            val hrPerm = HealthPermission.getReadPermission(HeartRateRecord::class)
            val exercisePerm = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
            if (hrPerm !in granted) Log.w(TAG, "Heart rate permission NOT granted")
            if (exercisePerm !in granted) Log.w(TAG, "Exercise session permission NOT granted")
            val hrCount = readHeartRate(client, granted)
            val exerciseCount = syncActivityAndEnergy(client, granted)
            _diagnostic.value = "HR records: ${fmtCount(hrCount)} · Exercise sessions: ${fmtCount(exerciseCount)}"
            Log.d(TAG, "Biometrics refreshed — HR=${_heartRate.value}, activityMin=${_activityMinutesToday.value}")
        } catch (e: Exception) {
            if (e.message?.contains("quota", ignoreCase = true) == true) {
                Log.w(TAG, "Biometrics refresh failed: quota exceeded")
                _diagnostic.value = "Health Connect quota exceeded — try again in a minute"
            } else {
                Log.w(TAG, "Biometrics refresh failed", e)
                _diagnostic.value = "Read failed: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    private fun fmtCount(n: Int): String = when (n) {
        -1 -> "no permission"
        0 -> "0 (no data)"
        else -> n.toString()
    }

    /** Returns whether read permission for HeartRateRecord is currently granted. */
    suspend fun hasHeartRatePermission(): Boolean {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            HealthPermission.getReadPermission(HeartRateRecord::class) in granted
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check HC permissions", e)
            false
        }
    }

    /** Returns the number of HeartRateRecord rows found in the queried window. */
    private suspend fun readHeartRate(client: HealthConnectClient, granted: Set<String>): Int {
        if (HealthPermission.getReadPermission(HeartRateRecord::class) !in granted) return -1

        // Start with a 60-min window, then fall back to 24h if empty (watch sync can be very slow)
        var windowSeconds = 3600L
        var response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().minusSeconds(windowSeconds), Instant.now()
                ),
                ascendingOrder = false,
                pageSize = 10
            )
        )
        if (response.records.isEmpty()) {
            windowSeconds = 86_400L
            response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.now().minusSeconds(windowSeconds), Instant.now()
                    ),
                    ascendingOrder = false,
                    pageSize = 10
                )
            )
        }
        Log.d(TAG, "HR query: ${response.records.size} records found in last ${windowSeconds}s")

        val latestSample = response.records
            .flatMap { it.samples }
            .maxByOrNull { it.time }

        if (latestSample != null && latestSample.beatsPerMinute > 0) {
            _heartRate.value = latestSample.beatsPerMinute.toInt()
            Log.d(TAG, "Heart rate: ${_heartRate.value} bpm (sample time=${latestSample.time})")
        } else {
            Log.w(TAG, "No heart rate sample found in the last ${windowSeconds}s")
        }
        return response.records.size
    }

    /** Returns number of exercise session records found today. */
    private suspend fun syncActivityAndEnergy(client: HealthConnectClient, granted: Set<String>): Int {
        if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) return -1

        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            )
        )
        val totalActivityMins = response.records.sumOf { record ->
            java.time.Duration.between(record.startTime, record.endTime).toMinutes()
        }.toInt()
        _activityMinutesToday.value = totalActivityMins
        Log.d(TAG, "Exercise sessions today: ${response.records.size} records, ${totalActivityMins} mins")
        return response.records.size

        // Energy score is now computed in SensorStateCollector where sleep data is also available.
    }

    companion object {
        private const val TAG = "HealthDataManager"
        private const val POLL_INTERVAL_MS = 180_000L // 3 min — watch HR syncs infrequently
        private const val SLOW_POLL_EVERY_N = 2 // sync exercise sessions every ~6 min
    }
}

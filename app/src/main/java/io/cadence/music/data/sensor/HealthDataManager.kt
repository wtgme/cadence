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
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            readHeartRate(client, granted)
            syncActivityAndEnergy(client, granted)
            Log.d(TAG, "Biometrics refreshed")
        } catch (e: Exception) {
            if (e.message?.contains("quota", ignoreCase = true) == true) {
                Log.w(TAG, "Biometrics refresh failed: quota exceeded")
            } else {
                Log.w(TAG, "Biometrics refresh failed", e)
            }
        }
    }

    private suspend fun readHeartRate(client: HealthConnectClient, granted: Set<String>) {
        if (HealthPermission.getReadPermission(HeartRateRecord::class) !in granted) return

        val end = Instant.now()
        val start = end.minusSeconds(900) // 15 min window
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1
            )
        )
        val latestSample = response.records.firstOrNull()?.samples?.maxByOrNull { it.time }

        if (latestSample != null && latestSample.beatsPerMinute > 0) {
            _heartRate.value = latestSample.beatsPerMinute.toInt()
        }
    }

    private suspend fun syncActivityAndEnergy(client: HealthConnectClient, granted: Set<String>) {
        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

        var totalActivityMins = 0
        
        if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                )
            )
            totalActivityMins = response.records.sumOf { record ->
                java.time.Duration.between(record.startTime, record.endTime).toMinutes()
            }.toInt()
            _activityMinutesToday.value = totalActivityMins
        }

        // Energy score is now computed in SensorStateCollector where sleep data is also available.
    }

    companion object {
        private const val TAG = "HealthDataManager"
        private const val POLL_INTERVAL_MS = 60_000L
        private const val SLOW_POLL_EVERY_N = 5 // sync exercise sessions every 5 min
    }
}

package io.cadence.music.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.data.model.ActiveWorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the activity type of any in-progress or just-finished workout reported
 * to Health Connect by a paired watch. When a user starts a "Running" workout on
 * Wear OS / Fitbit / etc., the resulting [ExerciseSessionRecord] surfaces here; we
 * expose its [ExerciseSessionRecord.exerciseType] so that `SceneDetector` can prefer
 * it over GPS/HR heuristics.
 *
 * Mirrors the iOS `WorkoutSessionRepository` (HealthKit `HKWorkout`).
 */
@Singleton
class WorkoutSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _activeType = MutableStateFlow<ActiveWorkoutType?>(null)
    val activeType: StateFlow<ActiveWorkoutType?> = _activeType

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    fun start() {
        pollJob?.cancel()
        pollJob = scope.launch {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable — workout session inactive")
                return@launch
            }
            val client = HealthConnectClient.getOrCreate(context)
            while (isActive) {
                refresh(client)
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
        refresh(HealthConnectClient.getOrCreate(context))
    }

    private suspend fun refresh(client: HealthConnectClient) {
        try {
            val granted = client.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) {
                if (_activeType.value != null) _activeType.value = null
                return
            }
            val now = Instant.now()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(now.minusSeconds(LOOKBACK_SECONDS), now),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            )
            val latest = response.records.maxByOrNull { it.endTime }
            val next: ActiveWorkoutType? = if (latest == null) {
                null
            } else {
                val ongoing = latest.endTime.isAfter(now)
                val endedRecently = Duration.between(latest.endTime, now).seconds < RECENT_WINDOW_SECONDS
                if (ongoing || endedRecently) map(latest.exerciseType) else null
            }
            if (next != _activeType.value) {
                Log.d(TAG, "Workout → $next")
                _activeType.value = next
            }
        } catch (e: Exception) {
            Log.w(TAG, "Workout refresh failed: ${e.message}")
        }
    }

    private fun map(type: Int): ActiveWorkoutType = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActiveWorkoutType.RUNNING
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ActiveWorkoutType.CYCLING
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActiveWorkoutType.WALKING
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> ActiveWorkoutType.ROWING
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> ActiveWorkoutType.ELLIPTICAL
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> ActiveWorkoutType.HIIT
        else -> ActiveWorkoutType.OTHER
    }

    private companion object {
        private const val TAG = "WorkoutSessionRepo"
        private const val POLL_INTERVAL_MS = 60_000L
        private const val LOOKBACK_SECONDS = 2 * 3600L  // 2 hours
        private const val RECENT_WINDOW_SECONDS = 5 * 60L  // 5 minutes
    }
}

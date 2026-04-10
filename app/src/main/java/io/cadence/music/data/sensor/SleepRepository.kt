package io.cadence.music.data.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep quality score: 0–100.
 *   ≥ 80  → good night (energise music slightly)
 *   50–79 → average
 *   < 50  → poor sleep (use calmer music to compensate)
 */
data class SleepScore(
    val score: Int,          // 0-100
    val durationHours: Float,
    val deepSleepPct: Float,
    val remSleepPct: Float,
)

@Singleton
class SleepRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _sleepScore = MutableStateFlow(SleepScore(score = 75, durationHours = 0f, deepSleepPct = 0f, remSleepPct = 0f))
    val sleepScore: StateFlow<SleepScore> = _sleepScore

    suspend fun refresh() {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            Log.w(TAG, "Health Connect not available")
            return
        }
        val client = HealthConnectClient.getOrCreate(context)
        try {
            // Check permission first
            val granted = client.permissionController.getGrantedPermissions()
            val hasSleep = HealthPermission.getReadPermission(SleepSessionRecord::class) in granted
            Log.d(TAG, "Sleep permission granted: $hasSleep (all granted: $granted)")
            if (!hasSleep) {
                Log.w(TAG, "Sleep permission not granted — using default score 75")
                return
            }

            // Look for last 48 hours to catch the most recent sleep session
            val end = Instant.now()
            val start = end.minusSeconds(172_800)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            Log.d(TAG, "Found ${response.records.size} sleep sessions in last 24h")

            val session = response.records
                .filter { it.endTime > it.startTime }
                .maxByOrNull { it.endTime }
                ?: run {
                    Log.d(TAG, "No sleep session found in last 24h — keeping default score")
                    return
                }

            val totalMs = (session.endTime.toEpochMilli() - session.startTime.toEpochMilli()).toFloat()
            val durationHours = totalMs / 3_600_000f

            var deepMs = 0L
            var remMs = 0L
            session.stages.forEach { stage ->
                val stageMs = stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMs += stageMs
                    SleepSessionRecord.STAGE_TYPE_REM  -> remMs += stageMs
                    else -> {}
                }
            }

            val deepPct = if (totalMs > 0) deepMs / totalMs * 100f else 0f
            val remPct  = if (totalMs > 0) remMs  / totalMs * 100f else 0f

            // Score: 40 pts for duration (7h target), 30 pts for deep ≥20%, 30 pts for REM ≥20%
            val durationScore = (durationHours / 7f * 40).coerceAtMost(40f).toInt()
            val deepScore     = (deepPct / 20f * 30).coerceAtMost(30f).toInt()
            val remScore      = (remPct  / 20f * 30).coerceAtMost(30f).toInt()
            val total         = durationScore + deepScore + remScore

            _sleepScore.value = SleepScore(
                score = total,
                durationHours = durationHours,
                deepSleepPct = deepPct,
                remSleepPct = remPct,
            )
            Log.d(TAG, "Sleep: ${durationHours}h, deep=${deepPct}%, rem=${remPct}%, score=$total")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sleep permission denied", e)
        } catch (e: Exception) {
            if (e.message?.contains("quota", ignoreCase = true) == true || 
                e.cause?.message?.contains("quota", ignoreCase = true) == true) {
                Log.w(TAG, "Health Connect quota exceeded for sleep data")
            } else {
                Log.e(TAG, "Failed to read sleep data", e)
            }
        }
    }

    companion object {
        private const val TAG = "SleepRepository"
    }
}

package io.cadence.music.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.data.model.SensorState
import io.cadence.music.ui.theme.GlowDefault
import io.cadence.music.ui.theme.Surface0
import io.cadence.music.ui.theme.TextPrimary
import io.cadence.music.ui.theme.TextSecondary
import io.cadence.music.ui.theme.TextTertiary

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val state      by viewModel.sensorState.collectAsState(initial = SensorState())
    val scene      by viewModel.scene.collectAsState(initial = null)
    val candidate  by viewModel.candidateScene.collectAsState(initial = null)
    val mentalState by viewModel.mentalState.collectAsState(initial = null)

    Surface(modifier = Modifier.fillMaxSize(), color = Surface0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("DEBUG", style = MaterialTheme.typography.titleLarge, color = GlowDefault)

            SectionHeader("Live Sensors")
            DebugRow("Heart rate", if (state.heartRate > 0) "${state.heartRate} bpm" else "—")
            DebugRow("Speed", "${"%.1f".format(state.speedKmh)} km/h")

            SectionHeader("Health")
            DebugRow("SpO2",  if (state.spo2 > 0) "${state.spo2}%" else "—")
            DebugRow("BP",    if (state.bloodPressureSystolic > 0) "${state.bloodPressureSystolic}/${state.bloodPressureDiastolic} mmHg" else "—")
            DebugRow("Temp",  if (state.bodyTemperature > 0f) "${"%.1f".format(state.bodyTemperature)}°C" else "—")

            SectionHeader("Today")
            DebugRow("Activity", "${state.activityMinutesToday} mins")
            DebugRow("Steps",    if (state.stepsToday > 0) "${state.stepsToday}" else "—")
            DebugRow("Distance", if (state.distanceKm > 0f) "${"%.2f".format(state.distanceKm)} km" else "—")
            DebugRow("Floors",   if (state.floorsClimbed > 0) "${state.floorsClimbed}" else "—")
            DebugRow("Calories", if (state.caloriesBurned > 0f) "${"%.0f".format(state.caloriesBurned)} kcal" else "—")

            SectionHeader("Sleep")
            DebugRow("Duration", if (state.sleepHours > 0f) "${"%.1f".format(state.sleepHours)}h" else "—")
            DebugRow("Deep / REM", "${"%.0f".format(state.sleepDeepPct)}% / ${"%.0f".format(state.sleepRemPct)}%")

            SectionHeader("Scene")
            DebugRow("Raw",       candidate?.displayName() ?: "—")
            DebugRow("Confirmed", scene?.displayName() ?: "—")
            DebugRow("Hour",      "${state.hourOfDay}:00")

            SectionHeader("Mental State")
            if (mentalState != null) {
                val ms         = mentalState!!
                val valenceStr = ms.valence?.let { v -> if (v >= 0) "+$v" else "$v" } ?: "—"
                DebugRow("Arousal / Valence", "${ms.arousal ?: "—"}/10   $valenceStr/5")
                DebugRow("Stress / Energy / Focus", "${ms.stress ?: "—"}/10   ${ms.energy ?: "—"}/10   ${ms.focus ?: "—"}/10")
                DebugRow("Mood", ms.mood ?: "—")
            } else {
                Text("(awaiting LLM)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text  = "— $title —",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
    )
}

@Composable
private fun DebugRow(label: String, value: String) {
    Text(
        text  = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = TextPrimary,
    )
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.SensorState

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val state by viewModel.sensorState.collectAsState(initial = SensorState())
    val scene by viewModel.scene.collectAsState(initial = null)
    val candidate by viewModel.candidateScene.collectAsState(initial = null)
    val mentalState by viewModel.mentalState.collectAsState(initial = null)

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("DEBUG", style = MaterialTheme.typography.titleLarge, color = Color.White)

            Text("— Live Sensors —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text("Heart rate: ${if (state.heartRate > 0) "${state.heartRate} bpm" else "—"}", color = Color.White)
            Text("Speed: ${"%.1f".format(state.speedKmh)} km/h", color = Color.White)

            Text("— Health —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text("SpO2: ${if (state.spo2 > 0) "${state.spo2}%" else "—"}", color = Color.White)
            Text("BP: ${if (state.bloodPressureSystolic > 0) "${state.bloodPressureSystolic}/${state.bloodPressureDiastolic} mmHg" else "—"}", color = Color.White)
            Text("Temp: ${if (state.bodyTemperature > 0f) "${"%.1f".format(state.bodyTemperature)}°C" else "—"}", color = Color.White)

            Text("— Today —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text("Activity: ${state.activityMinutesToday} mins", color = Color.White)
            Text("Steps: ${if (state.stepsToday > 0) "${state.stepsToday}" else "—"}", color = Color.White)
            Text("Distance: ${if (state.distanceKm > 0f) "${"%.2f".format(state.distanceKm)} km" else "—"}", color = Color.White)
            Text("Floors: ${if (state.floorsClimbed > 0) "${state.floorsClimbed}" else "—"}", color = Color.White)
            Text("Calories: ${if (state.caloriesBurned > 0f) "${"%.0f".format(state.caloriesBurned)} kcal" else "—"}", color = Color.White)

            Text("— Sleep —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text("Duration: ${if (state.sleepHours > 0f) "${"%.1f".format(state.sleepHours)}h" else "—"}", color = Color.White)
            Text("Deep: ${"%.0f".format(state.sleepDeepPct)}%  REM: ${"%.0f".format(state.sleepRemPct)}%", color = Color.White)

            Text("— Scene —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text("Raw: ${candidate?.displayName() ?: "—"}", color = Color.White.copy(alpha = 0.7f))
            Text("Confirmed: ${scene?.displayName() ?: "—"}", color = Color.White)
            Text("Hour: ${state.hourOfDay}:00", color = Color.White.copy(alpha = 0.7f))

            Text("— Mental State —", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            if (mentalState != null) {
                val ms = mentalState!!
                val valenceStr = ms.valence?.let { v -> if (v >= 0) "+$v" else "$v" } ?: "—"
                Text(
                    "Arousal: ${ms.arousal ?: "—"}/10   Valence: $valenceStr/5",
                    color = Color.White,
                )
                Text(
                    "Stress: ${ms.stress ?: "—"}/10   Energy: ${ms.energy ?: "—"}/10   Focus: ${ms.focus ?: "—"}/10",
                    color = Color.White,
                )
                Text("Mood: ${ms.mood ?: "—"}", color = Color.White)
            } else {
                Text("(awaiting LLM)", color = Color.White.copy(alpha = 0.3f))
            }
        }
    }
}

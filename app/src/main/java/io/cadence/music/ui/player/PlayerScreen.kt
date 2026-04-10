package io.cadence.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.model.Scene
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val confirmedScene by viewModel.currentScene.collectAsState()
    val candidateScene by viewModel.candidateScene.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val chunksReady by viewModel.chunksReady.collectAsState()
    val metricsContext by viewModel.currentMetricsContext.collectAsState()
    val songParams by viewModel.currentSongParams.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    val scene = candidateScene
    val isConfirming = candidateScene != null && candidateScene != confirmedScene
    val isBuffering = playbackState == PlaybackState.BUFFERING
    val isPlaying = playbackState == PlaybackState.PLAYING
    val isActive = playbackState != PlaybackState.IDLE
    var showSceneOverride by remember { mutableStateOf(false) }
    var showSwipeHint by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5_000)
        showSwipeHint = false
    }

    val mainGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A1A),
            Color(0xFF0A0A0A),
            Color(0xFF050505)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mainGradient)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 50) showSceneOverride = true
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))

                // Header
                Text(
                    text = "CADENCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4CAF50),
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(40.dp))

                // Now Playing Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scene?.displayName() ?: "Detecting Environment...",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    if (isConfirming) {
                        Text(
                            text = "Switching mode...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(200.dp)
                            .semantics { contentDescription = if (isBuffering) "Generating music" else if (isPlaying) "Music playing" else "Music paused" }
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = Color(0xFF4CAF50),
                                strokeWidth = 4.dp,
                            )
                        } else {
                            WaveformVisualizer(
                                isPlaying = isPlaying,
                                bpm = sensorState.heartRate,
                            )
                        }
                    }

                    if (isBuffering) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Synthesizing Music ($chunksReady/2)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Play / Stop control
                Button(
                    onClick = {
                        if (isActive) viewModel.stop() else viewModel.startPlayback()
                    },
                    enabled = !isBuffering,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isBuffering -> Color.Gray
                            isActive -> Color(0xFFE53935)
                            else -> Color(0xFF4CAF50)
                        }
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "Stop" else "Play",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Swipe hint
                if (showSwipeHint) {
                    Text(
                        text = "Swipe right to override scene",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }

                // Error banner
                if (lastError != null && isActive) {
                    Spacer(Modifier.height(12.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Generation failed — tap retry",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE53935),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.retryGeneration() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // AI Reasoning Chain
                if (metricsContext.isNotEmpty() && isActive) {
                    Text(
                        text = "AI REASONING CHAIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "INPUT: BIOMETRIC CONTEXT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = metricsContext,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 16.sp
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                            
                            Text(
                                text = "OUTPUT: GENERATED PROMPT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            if (songParams != null) {
                                Text(
                                    text = "Tags: ${songParams?.tags ?: "none"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = songParams?.lyric?.take(150)?.plus("...") ?: "Generating lyrics...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Stats Dashboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BIOMETRICS & ENVIRONMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    IconButton(
                        onClick = { viewModel.refreshBiometrics() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color(0xFF4CAF50).copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Vitals Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "HEART RATE",
                        value = if (sensorState.heartRate > 0) "${sensorState.heartRate}" else "—",
                        unit = "BPM",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "ENERGY",
                        value = if (sensorState.energyScore > 0) "${sensorState.energyScore}" else "—",
                        unit = if (sensorState.energyScore > 0) "/100" else "",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))

                // Activity Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "STEPS",
                        value = if (sensorState.stepsToday > 0) "${sensorState.stepsToday}" else "—",
                        unit = "Today",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "ACTIVITY",
                        value = if (sensorState.activityMinutesToday > 0) "${sensorState.activityMinutesToday}" else "—",
                        unit = "MINS",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Environment Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "KCAL",
                        value = if (sensorState.caloriesBurned > 0f) "${"%.0f".format(sensorState.caloriesBurned)}" else "—",
                        unit = "BURNED",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "SLEEP",
                        value = if (sensorState.sleepScore > 0) "${sensorState.sleepScore}" else "—",
                        unit = if (sensorState.sleepScore > 0) "/100" else "",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // GPS & Weather Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "WEATHER",
                        value = sensorState.weather,
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "GPS SPEED",
                        value = "${"%.1f".format(sensorState.speedKmh)}",
                        unit = "KM/H",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(48.dp))
            }

            // Scene override dropdown
            DropdownMenu(
                expanded = showSceneOverride,
                onDismissRequest = { showSceneOverride = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                Scene.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.displayName(), color = Color.White) },
                        onClick = {
                            viewModel.overrideScene(s)
                            showSceneOverride = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    val description = if (unit.isNotEmpty()) "$label: $value $unit" else "$label: $value"
    GlassCard(modifier = modifier.semantics { contentDescription = description }) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

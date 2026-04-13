package io.cadence.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
import io.cadence.music.audio.PlaybackProgress
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val HEALTH_CONNECT_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(BloodPressureRecord::class),
    HealthPermission.getReadPermission(BodyTemperatureRecord::class),
    HealthPermission.getReadPermission(FloorsClimbedRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
)

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val confirmedScene by viewModel.currentScene.collectAsState()
    val candidateScene by viewModel.candidateScene.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val chunksReady by viewModel.chunksReady.collectAsState()
    val metricsContext by viewModel.currentMetricsContext.collectAsState()
    val songParams by viewModel.currentSongParams.collectAsState()
    val mentalState by viewModel.currentMentalState.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val songHistory by viewModel.songHistory.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val isRefreshingBiometrics by viewModel.isRefreshingBiometrics.collectAsState()
    val hasHealthPermissions by viewModel.hasHealthPermissions.collectAsState()
    val healthDiagnostic by viewModel.healthDiagnostic.collectAsState()

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.recheckHealthPermissions()
        viewModel.refreshBiometrics()
    }

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

                Spacer(Modifier.height(24.dp))

                // Timeline
                SongTimeline(
                    progress = playbackProgress,
                    isEnabled = isPlaying,
                    onSeek = { viewModel.seekTo(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Playback controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Previous (restart current track)
                    IconButton(
                        onClick = { viewModel.skipToPrevious() },
                        enabled = isPlaying,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isPlaying) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Stop
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

                    // Next
                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        enabled = isPlaying,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isPlaying) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
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

                // Generated Tracks History
                if (songHistory.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    var tracksExpanded by remember { mutableStateOf(false) }
                    val visibleTracks = if (tracksExpanded) songHistory else songHistory.take(3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERATED TRACKS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                        if (songHistory.size > 3) {
                            Text(
                                text = if (tracksExpanded) "show less" else "+${songHistory.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                modifier = Modifier.clickable { tracksExpanded = !tracksExpanded }
                            )
                        }
                    }
                    visibleTracks.forEach { song ->
                        GeneratedTrackCard(song = song, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
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

                    var reasoningExpanded by remember { mutableStateOf(false) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reasoningExpanded = !reasoningExpanded }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INPUT: BIOMETRIC CONTEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (reasoningExpanded) "tap to collapse" else "tap to expand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = metricsContext,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 16.sp,
                                softWrap = true,
                                maxLines = if (reasoningExpanded) Int.MAX_VALUE else 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            Text(
                                text = "STEP 1A: MENTAL STATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            if (mentalState != null) {
                                MentalStateRow(mentalState!!)
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            Text(
                                text = "STEP 1B: GENERATED PROMPT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))

                            val params = songParams
                            if (params != null) {
                                Text(
                                    text = "Style: ${params.descriptions ?: "none"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    softWrap = true,
                                    maxLines = if (reasoningExpanded) Int.MAX_VALUE else 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Lyrics:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = params.lyric,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    softWrap = true,
                                    maxLines = if (reasoningExpanded) Int.MAX_VALUE else 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                if (reasoningExpanded) {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    )
                                    Text(
                                        text = "Generation parameters",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "type = ${params.generate_type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        text = "auto_prompt = ${params.auto_prompt_audio_type ?: "none"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasHealthPermissions) {
                            Text(
                                text = "Grant Health Access",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA726),
                                modifier = Modifier
                                    .clickable { healthPermissionLauncher.launch(HEALTH_CONNECT_PERMISSIONS) }
                                    .padding(end = 8.dp),
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshBiometrics() },
                            enabled = !isRefreshingBiometrics,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isRefreshingBiometrics) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Health Connect diagnostic
                healthDiagnostic?.let { diag ->
                    Text(
                        text = diag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                    val looksEmpty = diag.contains("0 (no data)") ||
                        sensorState.heartRate == 0 && sensorState.stepsToday == 0L && sensorState.caloriesBurned == 0f
                    if (looksEmpty && hasHealthPermissions) {
                        Text(
                            text = "If you use Samsung Health, open it → Settings → Health Connect → enable sharing for Heart Rate, Steps, Exercise, and Calories. Data may take a few minutes to sync after enabling.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFA726).copy(alpha = 0.7f),
                            lineHeight = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
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
                        label = "READINESS",
                        value = if (sensorState.readinessScore > 0) "${sensorState.readinessScore}" else "—",
                        unit = if (sensorState.readinessScore > 0) "/100" else "",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (sensorState.readinessBreakdown.isNotEmpty() && sensorState.readinessScore > 0) {
                    Text(
                        text = "Readiness: ${sensorState.readinessBreakdown}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
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
fun GeneratedTrackCard(song: GeneratedSong, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(
        modifier = modifier.clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            // Collapsed header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = song.scene?.displayName() ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp),
                    maxLines = 2,
                )
                Text(
                    text = song.params.descriptions ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = timeAgo(song.generatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                )
            }

            // Collapsed lyric preview
            if (!expanded) {
                val preview = song.params.lyric.lines().firstOrNull { it.isNotBlank() }?.trim()
                if (!preview.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 84.dp),
                    )
                }
            }

            // Expanded full content
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song.params.descriptions ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song.params.lyric.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 16.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
fun SongTimeline(
    progress: PlaybackProgress,
    isEnabled: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val fraction = when {
        progress.durationMs <= 0 -> 0f
        isDragging -> dragFraction
        else -> (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val displayPositionMs = if (isDragging) (dragFraction * progress.durationMs).toLong() else progress.positionMs

    Column(modifier = modifier) {
        Slider(
            value = fraction,
            onValueChange = { v ->
                isDragging = true
                dragFraction = v
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((dragFraction * progress.durationMs).toLong())
            },
            enabled = isEnabled && progress.durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4CAF50),
                activeTrackColor = Color(0xFF4CAF50),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                disabledThumbColor = Color.White.copy(alpha = 0.2f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.15f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f),
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                text = formatDuration(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun timeAgo(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
    }
}

@Composable
fun MentalStateRow(ms: MentalState) {
    val valenceStr = ms.valence?.let { v -> if (v >= 0) "+$v" else "$v" } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Arousal: ${ms.arousal ?: "—"}/10   Valence: $valenceStr/5   Stress: ${ms.stress ?: "—"}/10",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
        )
        Text(
            text = "Energy: ${ms.energy ?: "—"}/10   Focus: ${ms.focus ?: "—"}/10",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
        )
        ms.mood?.let { mood ->
            Text(
                text = "Mood: $mood",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        }
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

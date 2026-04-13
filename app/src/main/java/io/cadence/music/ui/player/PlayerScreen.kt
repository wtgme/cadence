package io.cadence.music.ui.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.audio.PlaybackProgress
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.UserTasteMemory
import io.cadence.music.ui.theme.FeedbackDislike
import io.cadence.music.ui.theme.FeedbackLike
import io.cadence.music.ui.theme.FeedbackNeutral
import io.cadence.music.ui.theme.GlowCommuting
import io.cadence.music.ui.theme.GlowDefault
import io.cadence.music.ui.theme.GlowResting
import io.cadence.music.ui.theme.GlowRunning
import io.cadence.music.ui.theme.GlowTraffic
import io.cadence.music.ui.theme.GlowWalking
import io.cadence.music.ui.theme.SceneCommuting
import io.cadence.music.ui.theme.SceneDefault
import io.cadence.music.ui.theme.SceneResting
import io.cadence.music.ui.theme.SceneRunning
import io.cadence.music.ui.theme.SceneTraffic
import io.cadence.music.ui.theme.SceneWalking
import io.cadence.music.ui.theme.Surface0
import io.cadence.music.ui.theme.Surface1
import io.cadence.music.ui.theme.Surface2
import io.cadence.music.ui.theme.TextPrimary
import io.cadence.music.ui.theme.TextSecondary
import io.cadence.music.ui.theme.TextTertiary
import io.cadence.music.ui.theme.WarningAmber
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

// ── Scene helpers ──────────────────────────────────────────────────────────

private fun Scene?.glowColor() = when (this) {
    Scene.RUNNING          -> GlowRunning
    Scene.WALKING          -> GlowWalking
    Scene.COMMUTING        -> GlowCommuting
    Scene.STUCK_IN_TRAFFIC -> GlowTraffic
    Scene.RESTING          -> GlowResting
    null                   -> GlowDefault
}

private fun Scene?.tintColor() = when (this) {
    Scene.RUNNING          -> SceneRunning
    Scene.WALKING          -> SceneWalking
    Scene.COMMUTING        -> SceneCommuting
    Scene.STUCK_IN_TRAFFIC -> SceneTraffic
    Scene.RESTING          -> SceneResting
    null                   -> SceneDefault
}

// ── Permissions (kept here to avoid duplication) ─────────────────────────

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

// ═══════════════════════════════════════════════════════════════════════════
// PlayerScreen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val confirmedScene        by viewModel.currentScene.collectAsState()
    val candidateScene        by viewModel.candidateScene.collectAsState()
    val sensorState           by viewModel.sensorState.collectAsState()
    val playbackState         by viewModel.playbackState.collectAsState()
    val chunksReady           by viewModel.chunksReady.collectAsState()
    val metricsContext        by viewModel.currentMetricsContext.collectAsState()
    val songParams            by viewModel.currentSongParams.collectAsState()
    val mentalState           by viewModel.currentMentalState.collectAsState()
    val lastError             by viewModel.lastError.collectAsState()
    val songHistory           by viewModel.songHistory.collectAsState()
    val playbackProgress      by viewModel.playbackProgress.collectAsState()
    val isRefreshingBiometrics by viewModel.isRefreshingBiometrics.collectAsState()
    val hasHealthPermissions  by viewModel.hasHealthPermissions.collectAsState()
    val healthDiagnostic      by viewModel.healthDiagnostic.collectAsState()
    val tasteMemory           by viewModel.tasteMemory.collectAsState()

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.recheckHealthPermissions()
        viewModel.refreshBiometrics()
    }

    val isBuffering = playbackState == PlaybackState.BUFFERING
    val isPlaying   = playbackState == PlaybackState.PLAYING
    val isActive    = playbackState != PlaybackState.IDLE

    var showSceneOverride by remember { mutableStateOf(false) }

    // Scene-tinted background
    val targetTint by animateColorAsState(
        targetValue = confirmedScene.tintColor(),
        animationSpec = tween(durationMillis = 1200),
        label = "sceneTint",
    )
    val targetGlow by animateColorAsState(
        targetValue = confirmedScene.glowColor(),
        animationSpec = tween(durationMillis = 1200),
        label = "sceneGlow",
    )

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 72.dp,
        sheetContainerColor = Surface1,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        },
        sheetContent = {
            BottomSheetContent(
                sensorState          = sensorState,
                metricsContext       = metricsContext,
                mentalState          = mentalState,
                songParams           = songParams,
                songHistory          = songHistory,
                isActive             = isActive,
                hasHealthPermissions = hasHealthPermissions,
                healthDiagnostic     = healthDiagnostic,
                isRefreshingBiometrics = isRefreshingBiometrics,
                onRefreshBiometrics  = { viewModel.refreshBiometrics() },
                onGrantHealth        = { healthPermissionLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to targetTint.copy(alpha = 0.85f),
                        0.5f to Surface0,
                        1f to Surface0,
                    )
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 50) showSceneOverride = true
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Header ────────────────────────────────────────────────
                Text(
                    text  = "CADENCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = targetGlow,
                )

                Spacer(Modifier.height(28.dp))

                // ── Scene name ────────────────────────────────────────────
                Text(
                    text      = confirmedScene?.displayName() ?: "Detecting…",
                    style     = MaterialTheme.typography.headlineLarge,
                    color     = TextPrimary,
                    textAlign = TextAlign.Center,
                )

                candidateScene?.let { candidate ->
                    if (candidate != confirmedScene) {
                        Text(
                            text  = "Switching to ${candidate.displayName()}…",
                            style = MaterialTheme.typography.labelMedium,
                            color = targetGlow.copy(alpha = 0.7f),
                        )
                    }
                }

                // HR badge
                if (sensorState.heartRate > 0) {
                    Spacer(Modifier.height(6.dp))
                    HrBadge(bpm = sensorState.heartRate, glowColor = targetGlow)
                }

                Spacer(Modifier.height(24.dp))

                // ── Hero waveform ─────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .semantics {
                            contentDescription = when {
                                isBuffering -> "Generating music"
                                isPlaying   -> "Music playing"
                                else        -> "Music paused"
                            }
                        },
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = targetGlow,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text  = "Synthesising\n($chunksReady/2)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 96.dp),
                        )
                    } else {
                        CircularWaveformVisualizer(
                            isPlaying = isPlaying,
                            bpm       = sensorState.heartRate,
                            glowColor = targetGlow,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Style tags ────────────────────────────────────────────
                songParams?.descriptions?.let { desc ->
                    if (desc.isNotBlank()) {
                        StyleTagRow(descriptions = desc, glowColor = targetGlow)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Timeline ──────────────────────────────────────────────
                SongTimeline(
                    progress  = playbackProgress,
                    isEnabled = isPlaying,
                    onSeek    = { viewModel.seekTo(it) },
                    glowColor = targetGlow,
                    modifier  = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                // ── Controls ──────────────────────────────────────────────
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(28.dp),
                ) {
                    IconButton(
                        onClick  = { viewModel.skipToPrevious() },
                        enabled  = isPlaying,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isPlaying) TextPrimary else TextTertiary,
                            modifier           = Modifier.size(30.dp),
                        )
                    }

                    // Play / Stop
                    Button(
                        onClick = { if (isActive) viewModel.stop() else viewModel.startPlayback() },
                        enabled = !isBuffering,
                        modifier = Modifier.size(72.dp),
                        shape    = CircleShape,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isBuffering -> Surface2
                                isActive    -> Color(0xFFBF3030)
                                else        -> targetGlow
                            },
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            imageVector        = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isActive) "Stop" else "Play",
                            modifier           = Modifier.size(32.dp),
                            tint               = Color.White,
                        )
                    }

                    IconButton(
                        onClick  = { viewModel.skipToNext() },
                        enabled  = isPlaying,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isPlaying) TextPrimary else TextTertiary,
                            modifier           = Modifier.size(30.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Feedback ──────────────────────────────────────────────
                if (isPlaying && songParams != null) {
                    TrackFeedbackRow(
                        tasteMemory   = tasteMemory,
                        onThumbsUp    = { viewModel.thumbsUp() },
                        onThumbsDown  = { viewModel.thumbsDown() },
                        glowColor     = targetGlow,
                    )
                }

                // ── Error banner ──────────────────────────────────────────
                if (lastError != null && isActive) {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(onRetry = { viewModel.retryGeneration() })
                }
            }

            // Scene override menu
            DropdownMenu(
                expanded          = showSceneOverride,
                onDismissRequest  = { showSceneOverride = false },
                modifier          = Modifier.background(Surface2),
            ) {
                Scene.entries.forEach { s ->
                    DropdownMenuItem(
                        text    = { Text(s.displayName(), color = TextPrimary) },
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

// ═══════════════════════════════════════════════════════════════════════════
// Bottom Sheet Content
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomSheetContent(
    sensorState: io.cadence.music.data.model.SensorState,
    metricsContext: String,
    mentalState: MentalState?,
    songParams: SongParams?,
    songHistory: List<GeneratedSong>,
    isActive: Boolean,
    hasHealthPermissions: Boolean,
    healthDiagnostic: String?,
    isRefreshingBiometrics: Boolean,
    onRefreshBiometrics: () -> Unit,
    onGrantHealth: () -> Unit,
) {
    // Peek label (always visible at 72dp sheet height)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = "BIOMETRICS & INSIGHTS",
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!hasHealthPermissions) {
                Text(
                    text     = "Grant Health Access",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = WarningAmber,
                    modifier = Modifier
                        .clickable(onClick = onGrantHealth)
                        .padding(end = 8.dp),
                )
            }
            IconButton(
                onClick  = onRefreshBiometrics,
                enabled  = !isRefreshingBiometrics,
                modifier = Modifier.size(24.dp),
            ) {
                if (isRefreshingBiometrics) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = GlowDefault.copy(alpha = 0.6f),
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = "Refresh biometrics",
                        tint               = GlowDefault.copy(alpha = 0.6f),
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // Health diagnostic
        healthDiagnostic?.let { diag ->
            Text(
                text  = diag,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            val looksEmpty = diag.contains("0 (no data)") ||
                (sensorState.heartRate == 0 && sensorState.stepsToday == 0L && sensorState.caloriesBurned == 0f)
            if (looksEmpty && hasHealthPermissions) {
                Text(
                    text      = "If you use Samsung Health, open it → Settings → Health Connect → enable sharing for Heart Rate, Steps, Exercise, and Calories.",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = WarningAmber.copy(alpha = 0.7f),
                    lineHeight = 14.sp,
                )
            }
        }

        // ── Biometrics grid ─────────────────────────────────────────────
        Text(
            text  = "VITALS",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "HEART RATE",
                value = if (sensorState.heartRate > 0) "${sensorState.heartRate}" else "—",
                unit  = "BPM",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "READINESS",
                value = if (sensorState.readinessScore > 0) "${sensorState.readinessScore}" else "—",
                unit  = if (sensorState.readinessScore > 0) "/100" else "",
                modifier = Modifier.weight(1f),
            )
        }
        if (sensorState.readinessBreakdown.isNotEmpty() && sensorState.readinessScore > 0) {
            Text(
                text  = "Readiness: ${sensorState.readinessBreakdown}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "STEPS",
                value = if (sensorState.stepsToday > 0) "${sensorState.stepsToday}" else "—",
                unit  = "Today",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "ACTIVE",
                value = if (sensorState.activityMinutesToday > 0) "${sensorState.activityMinutesToday}" else "—",
                unit  = "MINS",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "KCAL",
                value = if (sensorState.caloriesBurned > 0f) "${"%.0f".format(sensorState.caloriesBurned)}" else "—",
                unit  = "BURNED",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "SLEEP",
                value = if (sensorState.sleepScore > 0) "${sensorState.sleepScore}" else "—",
                unit  = if (sensorState.sleepScore > 0) "/100" else "",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "WEATHER",
                value = sensorState.weather,
                unit  = "",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "GPS",
                value = "${"%.1f".format(sensorState.speedKmh)}",
                unit  = "KM/H",
                modifier = Modifier.weight(1f),
            )
        }

        // ── AI Reasoning ────────────────────────────────────────────────
        if (metricsContext.isNotEmpty() && isActive) {
            HorizontalDivider(
                color    = Color.White.copy(alpha = 0.07f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
            AIReasoningPanel(
                metricsContext = metricsContext,
                mentalState    = mentalState,
                songParams     = songParams,
            )
        }

        // ── Song history ────────────────────────────────────────────────
        if (songHistory.isNotEmpty()) {
            HorizontalDivider(
                color    = Color.White.copy(alpha = 0.07f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
            var tracksExpanded by remember { mutableStateOf(false) }
            val visibleTracks  = if (tracksExpanded) songHistory else songHistory.take(3)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "GENERATED TRACKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                if (songHistory.size > 3) {
                    Text(
                        text     = if (tracksExpanded) "show less" else "+${songHistory.size - 3} more",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = GlowDefault.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { tracksExpanded = !tracksExpanded },
                    )
                }
            }

            visibleTracks.forEach { song ->
                GeneratedTrackCard(song = song, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Sub-composables
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HrBadge(bpm: Int, glowColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(glowColor.copy(alpha = 0.15f))
            .border(1.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text  = "♥ $bpm BPM",
            style = MaterialTheme.typography.labelMedium,
            color = glowColor,
        )
    }
}

@Composable
private fun StyleTagRow(descriptions: String, glowColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        descriptions.split(",", ";", " ").map { it.trim() }.filter { it.isNotBlank() }.take(5).forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(glowColor.copy(alpha = 0.12f))
                    .border(1.dp, glowColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text  = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = glowColor.copy(alpha = 0.85f),
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
    modifier: Modifier = Modifier,
    glowColor: Color = GlowDefault,
) {
    var isDragging   by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val fraction = when {
        progress.durationMs <= 0 -> 0f
        isDragging               -> dragFraction
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
            colors  = SliderDefaults.colors(
                thumbColor             = glowColor,
                activeTrackColor       = glowColor,
                inactiveTrackColor     = Color.White.copy(alpha = 0.2f),
                disabledThumbColor     = Color.White.copy(alpha = 0.2f),
                disabledActiveTrackColor   = Color.White.copy(alpha = 0.15f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = formatDuration(displayPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            Text(
                text  = formatDuration(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}

@Composable
fun TrackFeedbackRow(
    tasteMemory: UserTasteMemory,
    onThumbsUp: () -> Unit,
    onThumbsDown: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = GlowDefault,
) {
    var flash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(flash) {
        if (flash != null) {
            delay(900)
            flash = null
        }
    }

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick  = { flash = "down"; onThumbsDown() },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.ThumbDown,
                contentDescription = "Dislike",
                tint = if (flash == "down") FeedbackDislike else FeedbackNeutral,
                modifier           = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "RATE THIS TRACK",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            if (tasteMemory.feedbackCount > 0) {
                Text(
                    text  = "${tasteMemory.feedbackCount} signal${if (tasteMemory.feedbackCount == 1) "" else "s"} learned",
                    style = MaterialTheme.typography.labelSmall,
                    color = glowColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        IconButton(
            onClick  = { flash = "up"; onThumbsUp() },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.ThumbUp,
                contentDescription = "Like",
                tint = if (flash == "up") FeedbackLike else FeedbackNeutral,
                modifier           = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ErrorBanner(onRetry: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = io.cadence.music.ui.theme.ErrorRed,
                modifier           = Modifier.size(18.dp),
            )
            Text(
                text     = "Generation failed — tap retry",
                style    = MaterialTheme.typography.bodySmall,
                color    = io.cadence.music.ui.theme.ErrorRed,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
                colors  = ButtonDefaults.textButtonColors(contentColor = GlowDefault),
            ) {
                Text("Retry", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AIReasoningPanel(
    metricsContext: String,
    mentalState: MentalState?,
    songParams: SongParams?,
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text  = "AI REASONING CHAIN",
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "INPUT: BIOMETRIC CONTEXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlowDefault,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = if (expanded) "collapse" else "expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlowDefault.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text     = metricsContext,
                style    = MaterialTheme.typography.bodySmall,
                color    = TextSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = Color.White.copy(alpha = 0.08f),
            )

            Text(
                text  = "STEP 1A: MENTAL STATE",
                style = MaterialTheme.typography.labelSmall,
                color = GlowDefault,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (mentalState != null) {
                MentalStateRow(mentalState)
            } else {
                CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color       = GlowDefault,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = Color.White.copy(alpha = 0.08f),
            )

            Text(
                text  = "STEP 1B: GENERATED PROMPT",
                style = MaterialTheme.typography.labelSmall,
                color = GlowDefault,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (songParams != null) {
                Text(
                    text     = "Style: ${songParams.descriptions ?: "none"}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "Lyrics:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                Text(
                    text      = songParams.lyric,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = TextSecondary,
                    fontStyle = FontStyle.Italic,
                    maxLines  = if (expanded) Int.MAX_VALUE else 3,
                    overflow  = TextOverflow.Ellipsis,
                )
                if (expanded) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "type = ${songParams.generate_type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        text  = "auto_prompt = ${songParams.auto_prompt_audio_type ?: "none"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color       = GlowDefault,
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(
            width  = 1.dp,
            color  = Color.White.copy(alpha = 0.07f),
            shape  = RoundedCornerShape(14.dp),
        ),
        color  = Surface1,
        shape  = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = value,
                    style      = MaterialTheme.typography.titleLarge,
                    color      = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text     = unit,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
            ),
            shape = RoundedCornerShape(16.dp),
        ),
        color          = Color.White.copy(alpha = 0.05f),
        shape          = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
    ) { content() }
}

@Composable
fun GeneratedTrackCard(song: GeneratedSong, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = modifier.clickable { expanded = !expanded }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text       = song.scene?.displayName() ?: "—",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = GlowDefault,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.width(72.dp),
                    maxLines   = 2,
                )
                Text(
                    text      = song.params.descriptions ?: "—",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.weight(1f),
                )
                Text(
                    text  = timeAgo(song.generatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }

            if (!expanded) {
                val preview = song.params.lyric.lines().firstOrNull { it.isNotBlank() }?.trim()
                if (!preview.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text      = preview,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = TextTertiary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic,
                        modifier  = Modifier.padding(start = 82.dp),
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(10.dp))
                Text("Tags", style = MaterialTheme.typography.labelSmall, color = GlowDefault, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(song.params.descriptions ?: "—", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Text("Lyrics", style = MaterialTheme.typography.labelSmall, color = GlowDefault, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = song.params.lyric.ifBlank { "—" },
                    style     = MaterialTheme.typography.bodySmall,
                    color     = TextSecondary,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
fun MentalStateRow(ms: MentalState) {
    val valenceStr = ms.valence?.let { v -> if (v >= 0) "+$v" else "$v" } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text  = "Arousal: ${ms.arousal ?: "—"}/10   Valence: $valenceStr/5   Stress: ${ms.stress ?: "—"}/10",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
        )
        Text(
            text  = "Energy: ${ms.energy ?: "—"}/10   Focus: ${ms.focus ?: "—"}/10",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
        )
        ms.mood?.let { mood ->
            Text(
                text      = "Mood: $mood",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextSecondary,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

// ── Utilities ────────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun timeAgo(millis: Long): String {
    val diff    = System.currentTimeMillis() - millis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1  -> "just now"
        minutes < 60 -> "${minutes}m ago"
        else         -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
    }
}

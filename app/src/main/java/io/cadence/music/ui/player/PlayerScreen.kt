package io.cadence.music.ui.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.audio.PlaybackProgress
import io.cadence.music.audio.PlaybackState
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.GeneratedSong
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.data.model.UserMusicAdjustment
import io.cadence.music.data.model.UserTasteMemory
import io.cadence.music.ui.permissions.HEALTH_CONNECT_PERMISSIONS
import io.cadence.music.ui.theme.FeedbackDislike
import io.cadence.music.ui.theme.FeedbackLike
import io.cadence.music.ui.theme.FeedbackNeutral
import io.cadence.music.ui.theme.GlowCommuting
import io.cadence.music.ui.theme.GlowCycling
import io.cadence.music.ui.theme.GlowDefault
import io.cadence.music.ui.theme.GlowFocus
import io.cadence.music.ui.theme.GlowResting
import io.cadence.music.ui.theme.GlowRunning
import io.cadence.music.ui.theme.GlowWalking
import io.cadence.music.ui.theme.GlowWorkout
import io.cadence.music.ui.theme.SceneCommuting
import io.cadence.music.ui.theme.SceneCycling
import io.cadence.music.ui.theme.SceneDefault
import io.cadence.music.ui.theme.SceneFocus
import io.cadence.music.ui.theme.SceneResting
import io.cadence.music.ui.theme.SceneRunning
import io.cadence.music.ui.theme.SceneWalking
import io.cadence.music.ui.theme.SceneWorkout
import io.cadence.music.ui.theme.Surface0
import io.cadence.music.ui.theme.Surface1
import io.cadence.music.ui.theme.Surface2
import io.cadence.music.ui.theme.TextPrimary
import io.cadence.music.ui.theme.TextSecondary
import io.cadence.music.ui.theme.TextTertiary
import io.cadence.music.ui.theme.WarningAmber
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Scene helpers ──────────────────────────────────────────────────────────

private fun Scene?.glowColor() = when (this) {
    Scene.RUNNING   -> GlowRunning
    Scene.CYCLING   -> GlowCycling
    Scene.WALKING   -> GlowWalking
    Scene.COMMUTING -> GlowCommuting
    Scene.WORKOUT   -> GlowWorkout
    Scene.FOCUS     -> GlowFocus
    Scene.RESTING   -> GlowResting
    null            -> GlowDefault
}

private fun Scene?.tintColor() = when (this) {
    Scene.RUNNING   -> SceneRunning
    Scene.CYCLING   -> SceneCycling
    Scene.WALKING   -> SceneWalking
    Scene.COMMUTING -> SceneCommuting
    Scene.WORKOUT   -> SceneWorkout
    Scene.FOCUS     -> SceneFocus
    Scene.RESTING   -> SceneResting
    null            -> SceneDefault
}

// ═══════════════════════════════════════════════════════════════════════════
// PlayerScreen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val confirmedScene          by viewModel.currentScene.collectAsState()
    val candidateScene          by viewModel.candidateScene.collectAsState()
    val sensorState             by viewModel.sensorState.collectAsState()
    val playbackState           by viewModel.playbackState.collectAsState()
    val chunksReady             by viewModel.chunksReady.collectAsState()
    val metricsContext          by viewModel.currentMetricsContext.collectAsState()
    val songParams              by viewModel.currentSongParams.collectAsState()
    val mentalState             by viewModel.currentMentalState.collectAsState()
    val lastError               by viewModel.lastError.collectAsState()
    val songHistory             by viewModel.songHistory.collectAsState()
    val playbackProgress        by viewModel.playbackProgress.collectAsState()
    val isRefreshingBiometrics  by viewModel.isRefreshingBiometrics.collectAsState()
    val hasHealthPermissions    by viewModel.hasHealthPermissions.collectAsState()
    val healthDiagnostic        by viewModel.healthDiagnostic.collectAsState()
    val tasteMemory             by viewModel.tasteMemory.collectAsState()
    val currentAdjustment       by viewModel.currentAdjustment.collectAsState()
    val generationStartMs       by viewModel.generationStartMs.collectAsState()
    val isAdaptingToHrDrift     by viewModel.isAdaptingToHrDrift.collectAsState()

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.recheckHealthPermissions()
        viewModel.refreshBiometrics()
    }

    val isBuffering = playbackState == PlaybackState.BUFFERING
    val isPlaying   = playbackState == PlaybackState.PLAYING
    val isActive    = playbackState != PlaybackState.IDLE

    var showSceneOverride     by remember { mutableStateOf(false) }
    var showReasoningModal    by remember { mutableStateOf(false) }
    var healthBannerDismissed by remember { mutableStateOf(false) }
    var adjustmentExpanded    by remember { mutableStateOf(false) }

    // Animate hero bottom padding so content shifts up when the adjustment
    // panel expands and relaxes toward the bottom when it's collapsed.
    val heroBottomPad by animateDpAsState(
        targetValue = when {
            !isActive          -> 64.dp
            adjustmentExpanded -> 290.dp
            else               -> 100.dp
        },
        animationSpec = tween(durationMillis = 350),
        label         = "heroBottomPad",
    )

    // Next-song strip: briefly show "ready" after a new pre-gen completes
    var showNextSongReady by remember { mutableStateOf(false) }
    LaunchedEffect(chunksReady) {
        if (chunksReady >= 2 && isPlaying) {
            showNextSongReady = true
            delay(3000)
            showNextSongReady = false
        }
    }
    val isPreGenerating = isPlaying && chunksReady < 2

    // Scene-tinted background
    val targetTint by animateColorAsState(
        targetValue   = confirmedScene.tintColor(),
        animationSpec = tween(durationMillis = 1200),
        label         = "sceneTint",
    )
    val targetGlow by animateColorAsState(
        targetValue   = confirmedScene.glowColor(),
        animationSpec = tween(durationMillis = 1200),
        label         = "sceneGlow",
    )

    val dragThreshold = with(LocalDensity.current) { 50.dp.toPx() }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState       = scaffoldState,
        sheetPeekHeight     = 72.dp,
        sheetContainerColor = Surface1,
        sheetShape          = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle     = {
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
                confirmedScene         = confirmedScene,
                sensorState            = sensorState,
                metricsContext         = metricsContext,
                mentalState            = mentalState,
                songParams             = songParams,
                songHistory            = songHistory,
                tasteMemory            = tasteMemory,
                isActive               = isActive,
                hasHealthPermissions   = hasHealthPermissions,
                healthDiagnostic       = healthDiagnostic,
                isRefreshingBiometrics = isRefreshingBiometrics,
                onRefreshBiometrics    = { viewModel.refreshBiometrics() },
                onGrantHealth          = { healthPermissionLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
                onExpand               = { coroutineScope.launch { scaffoldState.bottomSheetState.expand() } },
                onClearMemory          = { viewModel.resetTasteMemory() },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f   to targetTint.copy(alpha = 0.85f),
                        0.5f to Surface0,
                        1f   to Surface0,
                    )
                )
                .pointerInput(dragThreshold) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > dragThreshold) showSceneOverride = true
                    }
                },
        ) {
            // ── Hero content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top    = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + heroBottomPad,
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ── Header ────────────────────────────────────────────────
                Text(
                    text  = "CADENCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = targetGlow,
                )

                Spacer(Modifier.height(12.dp))

                // ── Scene name — tappable for reasoning modal ─────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text      = confirmedScene?.displayName() ?: "Detecting…",
                        style     = MaterialTheme.typography.headlineLarge,
                        color     = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier  = if (isActive) Modifier.clickable { showReasoningModal = true } else Modifier,
                    )
                    if (isActive) {
                        IconButton(
                            onClick  = { showSceneOverride = true },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Tune,
                                contentDescription = "Override scene",
                                tint               = targetGlow.copy(alpha = 0.5f),
                                modifier           = Modifier.size(18.dp),
                            )
                        }
                    }
                }

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

                Spacer(Modifier.height(16.dp))

                // ── HR drift adaptation banner ────────────────────────────
                AnimatedVisibility(
                    visible = isAdaptingToHrDrift,
                    enter   = fadeIn() + slideInVertically { -20 },
                    exit    = fadeOut() + slideOutVertically { -20 },
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint               = targetGlow,
                                modifier           = Modifier.size(16.dp),
                            )
                            Text(
                                text  = "Heart rate shift detected — adapting music…",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                // ── Hero waveform ─────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(200.dp)
                        .semantics {
                            contentDescription = when {
                                isBuffering -> "Generating music"
                                isPlaying   -> "Music playing"
                                else        -> "Music paused"
                            }
                        },
                ) {
                    if (isBuffering) {
                        GenerationProgressArc(
                            chunksReady       = chunksReady,
                            generationStartMs = generationStartMs,
                            glowColor         = targetGlow,
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

                // ── Active adjustment pills ───────────────────────────────
                if (!currentAdjustment.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.fillMaxWidth(),
                    ) {
                        items(buildAdjustmentHints(currentAdjustment)) { hint ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(targetGlow.copy(alpha = 0.15f))
                                    .border(1.dp, targetGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text  = hint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = targetGlow.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Controls ──────────────────────────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    IconButton(
                        onClick  = { viewModel.skipToPrevious() },
                        enabled  = isPlaying,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint               = if (isPlaying) TextPrimary else TextTertiary,
                            modifier           = Modifier.size(30.dp),
                        )
                    }

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
                        elevation      = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
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
                            tint               = if (isPlaying) TextPrimary else TextTertiary,
                            modifier           = Modifier.size(30.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Feedback ──────────────────────────────────────────────
                if (isActive) {
                    TrackFeedbackRow(
                        tasteMemory   = tasteMemory,
                        onThumbsUp    = { viewModel.thumbsUp() },
                        onThumbsDown  = { viewModel.thumbsDown() },
                        onClearMemory = { viewModel.resetTasteMemory() },
                        glowColor     = targetGlow,
                    )
                }
            }

            // ── Error banner — full-bleed amber at top ────────────────────
            if (lastError != null && isActive) {
                ErrorBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top   = innerPadding.calculateTopPadding() + 8.dp,
                            start = 16.dp,
                            end   = 16.dp,
                        ),
                    onRetry = { viewModel.retryGeneration() },
                )
            }

            // ── Bottom panel ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        bottom = innerPadding.calculateBottomPadding() + 8.dp,
                        start  = 24.dp,
                        end    = 24.dp,
                    ),
            ) {
                // Health access banner (shown when health permissions not granted)
                AnimatedVisibility(
                    visible = !hasHealthPermissions && !isActive && !healthBannerDismissed,
                    enter   = fadeIn() + slideInVertically { 40 },
                    exit    = fadeOut() + slideOutVertically { 40 },
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint               = WarningAmber,
                                modifier           = Modifier.size(18.dp),
                            )
                            Text(
                                text     = "Grant health access for better music personalisation",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { healthPermissionLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
                                colors  = ButtonDefaults.textButtonColors(contentColor = WarningAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick  = { healthBannerDismissed = true },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint               = TextTertiary,
                                    modifier           = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                // Next song generating strip
                AnimatedVisibility(
                    visible = isPreGenerating || showNextSongReady,
                    enter   = fadeIn() + slideInVertically { 40 },
                    exit    = fadeOut() + slideOutVertically { 40 },
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (isPreGenerating) {
                                val dotAlpha by rememberInfiniteTransition(label = "dot").animateFloat(
                                    initialValue = 0.4f,
                                    targetValue  = 1f,
                                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                                    label        = "dotAlpha",
                                )
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(color = targetGlow.copy(alpha = dotAlpha))
                                }
                                Text(
                                    text     = "Next song generating…",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Icon(
                                    imageVector        = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint               = GlowDefault,
                                    modifier           = Modifier.size(14.dp),
                                )
                                Text(
                                    text     = "Next song ready",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = GlowDefault,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (isActive) {
                    AdjustmentPanel(
                        adjustment       = currentAdjustment,
                        expanded         = adjustmentExpanded,
                        onExpandedChange = { adjustmentExpanded = it },
                        onToggleGenre    = { viewModel.toggleGenre(it) },
                        onClearGenres    = { viewModel.clearGenres() },
                        onEnergyBias     = { viewModel.setEnergyBias(it) },
                        onFreeText       = { viewModel.submitFreeText(it) },
                    )
                }
            }

            // Scene override menu
            DropdownMenu(
                expanded         = showSceneOverride,
                onDismissRequest = { showSceneOverride = false },
                modifier         = Modifier.background(Surface2),
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

    // ── AI Reasoning modal ────────────────────────────────────────────────
    if (showReasoningModal) {
        val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReasoningModal = false },
            sheetState       = modalSheetState,
            containerColor   = Surface1,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text  = "WHY THIS MUSIC?",
                    style = MaterialTheme.typography.labelLarge,
                    color = confirmedScene.glowColor(),
                    modifier = Modifier.padding(top = 8.dp),
                )
                AIReasoningPanel(
                    metricsContext = metricsContext,
                    mentalState    = mentalState,
                    songParams     = songParams,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Bottom Sheet Content
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomSheetContent(
    confirmedScene: Scene?,
    sensorState: SensorState,
    metricsContext: String,
    mentalState: MentalState?,
    songParams: SongParams?,
    songHistory: List<GeneratedSong>,
    tasteMemory: UserTasteMemory,
    isActive: Boolean,
    hasHealthPermissions: Boolean,
    healthDiagnostic: String?,
    isRefreshingBiometrics: Boolean,
    onRefreshBiometrics: () -> Unit,
    onGrantHealth: () -> Unit,
    onExpand: () -> Unit,
    onClearMemory: () -> Unit,
) {
    val sceneGlow = confirmedScene.glowColor()

    // ── Peek row — insight card, tappable to expand ───────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: scene name + HR
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.weight(1f),
        ) {
            AnimatedContent(
                targetState = confirmedScene?.displayName() ?: "Detecting",
                label       = "peekScene",
            ) { sceneName ->
                Text(
                    text       = sceneName,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = sceneGlow,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                )
            }
            if (sensorState.heartRate > 0) {
                Text(
                    text  = "♥ ${sensorState.heartRate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = sceneGlow.copy(alpha = 0.65f),
                )
            }
        }

        // Right: top-2 style tags + chevron + refresh
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (songParams?.descriptions != null) {
                val topTags = songParams.descriptions
                    .split(",", ";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(2)
                topTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sceneGlow.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text  = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = sceneGlow.copy(alpha = 0.75f),
                        )
                    }
                }
            }
            if (!hasHealthPermissions) {
                Icon(
                    imageVector        = Icons.Default.MonitorHeart,
                    contentDescription = "Grant health access",
                    tint               = WarningAmber.copy(alpha = 0.7f),
                    modifier           = Modifier
                        .size(16.dp)
                        .clickable(onClick = onGrantHealth),
                )
            }
            Icon(
                imageVector        = Icons.Default.ExpandLess,
                contentDescription = "Expand",
                tint               = TextTertiary,
                modifier           = Modifier.size(16.dp),
            )
            IconButton(
                onClick  = onRefreshBiometrics,
                enabled  = !isRefreshingBiometrics,
                modifier = Modifier.size(36.dp),
            ) {
                if (isRefreshingBiometrics) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color       = GlowDefault.copy(alpha = 0.6f),
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = "Refresh biometrics",
                        tint               = GlowDefault.copy(alpha = 0.6f),
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
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
                    text       = "If you use Samsung Health, open it → Settings → Health Connect → enable sharing for Heart Rate, Steps, Exercise, and Calories.",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = WarningAmber.copy(alpha = 0.7f),
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
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label    = "HEART RATE",
                value    = if (sensorState.heartRate > 0) "${sensorState.heartRate}" else "—",
                unit     = "BPM",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "READINESS",
                value    = if (sensorState.readinessScore > 0) "${sensorState.readinessScore}" else "—",
                unit     = if (sensorState.readinessScore > 0) "/100" else "",
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
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label    = "STEPS",
                value    = if (sensorState.stepsToday > 0) "${sensorState.stepsToday}" else "—",
                unit     = "Today",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "ACTIVE",
                value    = if (sensorState.activityMinutesToday > 0) "${sensorState.activityMinutesToday}" else "—",
                unit     = "MINS",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label    = "KCAL",
                value    = if (sensorState.caloriesBurned > 0f) "${"%.0f".format(sensorState.caloriesBurned)}" else "—",
                unit     = "BURNED",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "SLEEP",
                value    = if (sensorState.sleepScore > 0) "${sensorState.sleepScore}" else "—",
                unit     = if (sensorState.sleepScore > 0) "/100" else "",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label    = "WEATHER",
                value    = sensorState.weather,
                unit     = "",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "GPS",
                value    = "${"%.1f".format(sensorState.speedKmh)}",
                unit     = "KM/H",
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
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
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

        // ── Taste profile ────────────────────────────────────────────────
        HorizontalDivider(
            color    = Color.White.copy(alpha = 0.07f),
            modifier = Modifier.padding(vertical = 4.dp),
        )
        TasteProfileSection(
            tasteMemory    = tasteMemory,
            glowColor      = confirmedScene.glowColor(),
            onClearMemory  = onClearMemory,
        )
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
    val tags = remember(descriptions) {
        descriptions.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.take(5)
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        items(tags) { tag ->
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
            value             = fraction,
            onValueChange     = { v ->
                isDragging   = true
                dragFraction = v
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((dragFraction * progress.durationMs).toLong())
            },
            enabled = isEnabled && progress.durationMs > 0,
            colors  = SliderDefaults.colors(
                thumbColor                 = glowColor,
                activeTrackColor           = glowColor,
                inactiveTrackColor         = Color.White.copy(alpha = 0.2f),
                disabledThumbColor         = Color.White.copy(alpha = 0.2f),
                disabledActiveTrackColor   = Color.White.copy(alpha = 0.15f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier              = Modifier
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
    onClearMemory: () -> Unit = {},
    modifier: Modifier = Modifier,
    glowColor: Color = GlowDefault,
) {
    var flash by remember { mutableStateOf<String?>(null) }
    var bounceUp by remember { mutableStateOf(false) }
    var bounceDown by remember { mutableStateOf(false) }
    var confirmMessage by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(flash) {
        if (flash != null) {
            delay(900)
            flash = null
        }
    }

    LaunchedEffect(confirmMessage) {
        if (confirmMessage != null) {
            delay(2000)
            confirmMessage = null
        }
    }

    val thumbDownTint by animateColorAsState(
        targetValue   = if (flash == "down") FeedbackDislike else FeedbackNeutral,
        animationSpec = tween(300),
        label         = "thumbDownTint",
    )
    val thumbUpTint by animateColorAsState(
        targetValue   = if (flash == "up") FeedbackLike else FeedbackNeutral,
        animationSpec = tween(300),
        label         = "thumbUpTint",
    )
    val scaleDown by animateFloatAsState(
        targetValue   = if (bounceDown) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scaleDown",
        finishedListener = { bounceDown = false },
    )
    val scaleUp by animateFloatAsState(
        targetValue   = if (bounceUp) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scaleUp",
        finishedListener = { bounceUp = false },
    )

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick  = {
                    flash = "down"
                    bounceDown = true
                    confirmMessage = "Skipping this style"
                    onThumbsDown()
                },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.ThumbDown,
                    contentDescription = "Dislike",
                    tint               = thumbDownTint,
                    modifier           = Modifier.size(22.dp).scale(scaleDown),
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
                        text     = "${tasteMemory.feedbackCount} signal${if (tasteMemory.feedbackCount == 1) "" else "s"} learned",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = glowColor.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                    )
                }
                // Confirmation toast
                if (confirmMessage != null) {
                    Text(
                        text  = confirmMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (confirmMessage!!.contains("Got it")) FeedbackLike else FeedbackDislike,
                        fontSize = 9.sp,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick  = {
                    flash = "up"
                    bounceUp = true
                    confirmMessage = "Got it — learning your taste"
                    onThumbsUp()
                },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.ThumbUp,
                    contentDescription = "Like",
                    tint               = thumbUpTint,
                    modifier           = Modifier.size(22.dp).scale(scaleUp),
                )
            }
        }

        if (tasteMemory.feedbackCount > 0) {
            if (showClearConfirm) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text  = "Wipe all taste memory?",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                    Text(
                        text     = "CONFIRM",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = io.cadence.music.ui.theme.ErrorRed,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = io.cadence.music.ui.theme.ErrorRed.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .clickable {
                                onClearMemory()
                                showClearConfirm = false
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text     = "Cancel",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        modifier = Modifier.clickable { showClearConfirm = false },
                    )
                }
            } else {
                TextButton(
                    onClick        = { showClearConfirm = true },
                    modifier       = Modifier.height(28.dp),
                    colors         = ButtonDefaults.textButtonColors(contentColor = TextTertiary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("Clear taste memory", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    val alpha by rememberInfiniteTransition(label = "errorPulse").animateFloat(
        initialValue  = 0.88f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "errorAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningAmber.copy(alpha = alpha)),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = Color.Black,
                modifier           = Modifier.size(18.dp),
            )
            Text(
                text     = "Generation failed — tap retry",
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.Black,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
                colors  = ButtonDefaults.textButtonColors(contentColor = Color.Black),
            ) {
                Text("Retry", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
        text     = "AI REASONING CHAIN",
        style    = MaterialTheme.typography.labelSmall,
        color    = TextTertiary,
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
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "INPUT: BIOMETRIC CONTEXT",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = GlowDefault,
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
                text       = "ESTIMATION: MENTAL STATE",
                style      = MaterialTheme.typography.labelSmall,
                color      = GlowDefault,
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
                text       = "RECOMMENDATION: MUSIC STYLES",
                style      = MaterialTheme.typography.labelSmall,
                color      = GlowDefault,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (songParams != null) {
                Text(
                    text       = "Style: ${songParams.descriptions ?: "none"}",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = if (expanded) Int.MAX_VALUE else 1,
                    overflow   = TextOverflow.Ellipsis,
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
            width = 1.dp,
            color = Color.White.copy(alpha = 0.07f),
            shape = RoundedCornerShape(14.dp),
        ),
        color          = Surface1,
        shape          = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
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
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
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
        color          = backgroundColor,
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
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
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
                    text     = song.params.descriptions ?: "—",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
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

// ── Mental State — visual dot-track scales ────────────────────────────────

@Composable
fun MentalStateRow(ms: MentalState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MentalMetricBar(
                label    = "AROUSAL",
                value    = ms.arousal,
                max      = 10,
                color    = GlowRunning,
                modifier = Modifier.weight(1f),
            )
            ValenceBar(
                value    = ms.valence,
                modifier = Modifier.weight(1f),
            )
            MentalMetricBar(
                label    = "STRESS",
                value    = ms.stress,
                max      = 10,
                color    = FeedbackDislike,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            MentalMetricBar(
                label    = "ENERGY",
                value    = ms.energy,
                max      = 10,
                color    = GlowWalking,
                modifier = Modifier.weight(1f),
            )
            MentalMetricBar(
                label    = "FOCUS",
                value    = ms.focus,
                max      = 10,
                color    = GlowCommuting,
                modifier = Modifier.weight(1f),
            )
            ms.mood?.let { mood ->
                Column(
                    modifier            = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text     = "MOOD",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        fontSize = 9.sp,
                    )
                    Text(
                        text      = mood,
                        style     = MaterialTheme.typography.bodySmall,
                        color     = TextSecondary,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                    )
                }
            } ?: Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MentalMetricBar(
    label: String,
    value: Int?,
    max: Int = 10,
    color: Color = GlowDefault,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = TextTertiary,
            fontSize = 9.sp,
        )
        Spacer(Modifier.height(3.dp))
        if (value != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                for (i in 1..max) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= value) color.copy(alpha = 0.85f)
                                else color.copy(alpha = 0.15f)
                            ),
                    )
                }
            }
            Text(
                text     = "$value",
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                fontSize = 9.sp,
            )
        } else {
            Text(
                text  = "—",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun ValenceBar(value: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text     = "VALENCE",
            style    = MaterialTheme.typography.labelSmall,
            color    = TextTertiary,
            fontSize = 9.sp,
        )
        Spacer(Modifier.height(3.dp))
        if (value != null) {
            // -5..+5, 10 dots total (skip 0 centre)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                for (i in -5..-1) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (value <= i) FeedbackDislike.copy(alpha = 0.85f)
                                else FeedbackDislike.copy(alpha = 0.15f)
                            ),
                    )
                }
                Spacer(Modifier.width(2.dp))
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (value >= i) FeedbackLike.copy(alpha = 0.85f)
                                else FeedbackLike.copy(alpha = 0.15f)
                            ),
                    )
                }
            }
            val valenceStr = if (value >= 0) "+$value" else "$value"
            Text(
                text     = valenceStr,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                fontSize = 9.sp,
            )
        } else {
            Text(
                text  = "—",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

// ── Taste Profile ─────────────────────────────────────────────────────────

@Composable
private fun TasteProfileSection(
    tasteMemory: UserTasteMemory,
    glowColor: Color,
    onClearMemory: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = "TASTE PROFILE",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        if (tasteMemory.feedbackCount > 0) {
            TextButton(
                onClick        = { showClearConfirm = true },
                colors         = ButtonDefaults.textButtonColors(contentColor = io.cadence.music.ui.theme.ErrorRed.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier       = Modifier.height(28.dp),
            ) {
                Text("Reset memory", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (showClearConfirm) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "Wipe all ${tasteMemory.feedbackCount} signals?",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text     = "CONFIRM",
                style    = MaterialTheme.typography.labelSmall,
                color    = io.cadence.music.ui.theme.ErrorRed,
                modifier = Modifier
                    .border(1.dp, io.cadence.music.ui.theme.ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .clickable { onClearMemory(); showClearConfirm = false }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(
                text     = "Cancel",
                style    = MaterialTheme.typography.labelSmall,
                color    = TextTertiary,
                modifier = Modifier.clickable { showClearConfirm = false },
            )
        }
        Spacer(Modifier.height(4.dp))
    }

    if (tasteMemory.feedbackCount == 0) {
        Text(
            text      = "Rate tracks to teach Cadence your taste.",
            style     = MaterialTheme.typography.bodySmall,
            color     = TextTertiary,
            fontStyle = FontStyle.Italic,
        )
        return
    }

    val topTags = tasteMemory.tagScores.entries
        .sortedByDescending { it.value }
        .take(6)

    if (topTags.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            topTags.forEach { (tag, score) ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text     = tag,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextSecondary,
                        modifier = Modifier.width(80.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val fillFraction = ((score + 1f) / 2f).coerceIn(0f, 1f)
                    val barColor = when {
                        score >= 0.3f  -> FeedbackLike
                        score <= -0.3f -> FeedbackDislike
                        else           -> TextTertiary
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fillFraction)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor.copy(alpha = 0.7f)),
                        )
                    }
                    Text(
                        text      = "${"%.0f".format(score * 100)}%",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = TextTertiary,
                        modifier  = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Adjustment Panel
// ═══════════════════════════════════════════════════════════════════════════

private val GENRE_OPTIONS = listOf(
    "jazz",
    "electronic",
    "pop",
    "rock",
    "ambient",
    "folk",
    "hip-hop",
    "classical",
)

private fun buildAdjustmentHints(adjustment: UserMusicAdjustment): List<String> = buildList {
    adjustment.genreOverrides.forEach { add(it.uppercase()) }
    when {
        adjustment.energyBias >= 1  -> add("+ENERGY")
        adjustment.energyBias <= -1 -> add("-ENERGY")
    }
    if (adjustment.freeText != null) add("CUSTOM")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdjustmentPanel(
    adjustment: UserMusicAdjustment,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleGenre: (String) -> Unit,
    onClearGenres: () -> Unit,
    onEnergyBias: (Int) -> Unit,
    onFreeText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var freeTextValue by remember { mutableStateOf("") }
    var sliderPosition by remember { mutableFloatStateOf(adjustment.energyBias.toFloat()) }
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Clear keyboard when panel collapses
    LaunchedEffect(expanded) {
        if (!expanded) focusManager.clearFocus()
    }

    // Darken the card when expanded so text fields are readable without breaking the dark theme
    val cardBackground by animateColorAsState(
        targetValue   = if (expanded) Color(0xFF0E1117).copy(alpha = 0.92f)
                        else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(300),
        label         = "adjustCardBg",
    )

    GlassCard(modifier = modifier.fillMaxWidth(), backgroundColor = cardBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Header row — toggles expanded state
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "ADJUST MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                val activeHints = buildAdjustmentHints(adjustment).joinToString(" · ")
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (activeHints.isNotEmpty()) {
                        Text(
                            text  = activeHints,
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowDefault,
                        )
                    }
                    Text(
                        text  = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))

                // Genre Chips
                Text(
                    text  = "GENRE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // "Auto" chip clears all genre overrides
                    item {
                        FilterChip(
                            selected = adjustment.genreOverrides.isEmpty(),
                            onClick  = { onClearGenres() },
                            label    = { Text("Auto", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowDefault.copy(alpha = 0.25f),
                                selectedLabelColor     = GlowDefault,
                                containerColor         = Color.Transparent,
                                labelColor             = TextSecondary,
                            ),
                        )
                    }
                    items(GENRE_OPTIONS) { genre ->
                        val selected = genre in adjustment.genreOverrides
                        FilterChip(
                            selected = selected,
                            onClick  = { onToggleGenre(genre) },
                            label    = {
                                Text(
                                    genre.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowDefault.copy(alpha = 0.25f),
                                selectedLabelColor     = GlowDefault,
                                containerColor         = Color.Transparent,
                                labelColor             = TextSecondary,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Energy Slider
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "ENERGY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text     = "Calmer",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        fontSize = 9.sp,
                    )
                    Slider(
                        value                 = sliderPosition,
                        onValueChange         = { sliderPosition = it },
                        onValueChangeFinished = {
                            val snapped = sliderPosition.toInt().coerceIn(-2, 2)
                            sliderPosition = snapped.toFloat()
                            onEnergyBias(snapped)
                        },
                        valueRange = -2f..2f,
                        steps      = 3,
                        modifier   = Modifier.weight(1f),
                        colors     = SliderDefaults.colors(
                            thumbColor         = GlowDefault,
                            activeTrackColor   = GlowDefault,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                    Text(
                        text     = "More",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        fontSize = 9.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Free Text
                OutlinedTextField(
                    value         = freeTextValue,
                    onValueChange = { freeTextValue = it },
                    placeholder   = {
                        Text(
                            text  = "Try: 'more cinematic', 'add piano'…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    },
                    trailingIcon = {
                        if (freeTextValue.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onFreeText(freeTextValue)
                                    freeTextValue = ""
                                    focusManager.clearFocus()
                                },
                            ) {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Apply",
                                    tint               = GlowDefault,
                                    modifier           = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (freeTextValue.isNotBlank()) {
                            onFreeText(freeTextValue)
                            freeTextValue = ""
                            focusManager.clearFocus()
                        }
                    }),
                    singleLine = true,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GlowDefault,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor          = GlowDefault,
                    ),
                )
            }
        }
    }
}

// ── Utilities ──────────────────────────────────────────────────────────────

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

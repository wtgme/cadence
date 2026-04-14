package io.cadence.music.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cadence.music.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Linear 32-bar waveform — kept for the onboarding background layer. */
@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    bpm: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    val beatPeriodMs = if (bpm > 0) (60_000f / bpm) else 600f

    val phase by rememberInfiniteTransition(label = "waveform").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatPeriodMs.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val barCount = 32
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f
        val maxAmp = size.height / 2f * 0.85f

        for (i in 0 until barCount) {
            val x = i * size.width / barCount + barWidth
            val amplitude = if (isPlaying) {
                maxAmp * (0.4f + 0.6f * sin(phase + i * 0.4f).toFloat().coerceIn(-1f, 1f).let {
                    (it + 1f) / 2f
                })
            } else {
                maxAmp * 0.1f
            }
            drawLine(
                color = color.copy(alpha = if (isPlaying) 0.85f else 0.3f),
                start = Offset(x, centerY - amplitude),
                end = Offset(x, centerY + amplitude),
                strokeWidth = barWidth * 0.7f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Circular waveform visualizer — radial bars arranged in a ring.
 * When [isPlaying] the bars pulse outward in a wave driven by [bpm].
 * Use [glowColor] to tint it for the current scene.
 */
@Composable
fun CircularWaveformVisualizer(
    isPlaying: Boolean,
    bpm: Int,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    val beatPeriodMs = if (bpm > 0) (60_000f / bpm).coerceIn(300f, 2000f) else 800f

    val phase by rememberInfiniteTransition(label = "circWaveform").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatPeriodMs.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "circPhase",
    )

    Canvas(modifier = modifier.size(240.dp)) {
        drawCircularWaveform(
            phase = phase,
            isPlaying = isPlaying,
            glowColor = glowColor,
        )
    }
}

/**
 * Indeterminate progress arc shown while music is being generated.
 * Two arcs pulse in alternation to signal ongoing work.
 * Shows elapsed time so the user always sees forward progress.
 */
@Composable
fun GenerationProgressArc(
    chunksReady: Int,
    generationStartMs: Long,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    // Infinite pulsing animation — each arc breathes in and out with a phase offset
    val infiniteTransition = rememberInfiniteTransition(label = "arcPulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse2",
    )

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(generationStartMs) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - generationStartMs) / 1000).toInt()
            delay(1000)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.size(200.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW  = 8.dp.toPx()
            val inset    = strokeW / 2f
            val arcSize  = Size(size.width - strokeW, size.height - strokeW)
            val topLeft  = Offset(inset, inset)
            val style    = Stroke(strokeW, cap = StrokeCap.Round)

            // Background tracks
            drawArc(glowColor.copy(alpha = 0.12f), 195f, 150f, false, topLeft, arcSize, style = style)
            drawArc(glowColor.copy(alpha = 0.12f),  15f, 150f, false, topLeft, arcSize, style = style)

            // Pulsing fill arcs (indeterminate)
            drawArc(glowColor.copy(alpha = 0.85f * pulse1), 195f, 150f, false, topLeft, arcSize, style = style)
            drawArc(glowColor.copy(alpha = 0.85f * pulse2),  15f, 150f, false, topLeft, arcSize, style = style)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = "Synthesising",
                style     = MaterialTheme.typography.labelMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${elapsedSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                color = glowColor.copy(alpha = 0.6f),
            )
        }
    }
}

private fun DrawScope.drawCircularWaveform(
    phase: Float,
    isPlaying: Boolean,
    glowColor: Color,
) {
    val barCount = 48
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.minDimension / 2f * 0.52f
    val maxBarLen = size.minDimension / 2f * 0.35f
    val minBarLen = size.minDimension / 2f * 0.04f
    val barWidth = 3.5f

    for (i in 0 until barCount) {
        val angle = (2 * PI * i / barCount).toFloat()
        val waveVal = if (isPlaying) {
            val raw = sin(phase + i * (2 * PI / barCount).toFloat() * 2f).toFloat()
            (raw + 1f) / 2f  // 0..1
        } else {
            0.12f
        }
        val barLen = minBarLen + waveVal * (maxBarLen - minBarLen)
        val alpha = if (isPlaying) 0.55f + waveVal * 0.45f else 0.25f

        val innerX = cx + cos(angle) * radius
        val innerY = cy + sin(angle) * radius
        val outerX = cx + cos(angle) * (radius + barLen)
        val outerY = cy + sin(angle) * (radius + barLen)

        drawLine(
            color = glowColor.copy(alpha = alpha),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
        )
    }

    // Centre ring
    drawCircle(
        color = glowColor.copy(alpha = if (isPlaying) 0.12f else 0.06f),
        radius = radius,
        center = Offset(cx, cy),
    )
}

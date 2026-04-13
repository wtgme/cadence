package io.cadence.music.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
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

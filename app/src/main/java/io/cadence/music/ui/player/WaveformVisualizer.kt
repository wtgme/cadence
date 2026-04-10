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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

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
        targetValue = (2 * Math.PI).toFloat(),
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

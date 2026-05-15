package io.cadence.music.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBlueDimHi
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceBorderHi
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDimHi
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextDim
import io.cadence.music.ui.theme.CadenceTextMute

@Composable
fun StepDots(step: Int, total: Int = 5, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val active = i == step
            val past   = i < step
            val width by animateDpAsState(if (active) 22.dp else 6.dp, tween(200), label = "stepWidth")
            Box(
                modifier = Modifier
                    .width(width)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            active -> CadenceOrange
                            past   -> CadenceBlue
                            else   -> Color.White.copy(alpha = 0.16f)
                        }
                    ),
            )
        }
    }
}

@Composable
fun OnboardingTopBar(
    step: Int,
    totalSteps: Int = 5,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CadenceBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CadenceTextMute,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }

        StepDots(step = step, total = totalSteps)

        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            trailing?.invoke()
        }
    }
}

enum class CadenceButtonTone { Blue, Orange }

@Composable
fun PrimaryCadenceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: CadenceButtonTone = CadenceButtonTone.Blue,
) {
    val gradient = when {
        !enabled -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
        tone == CadenceButtonTone.Orange -> Brush.verticalGradient(listOf(CadenceOrange, Color(0xFFE67630)))
        else -> Brush.verticalGradient(listOf(CadenceBlue, Color(0xFF3870E6)))
    }
    val textColor = if (enabled) CadenceBg else CadenceTextDim

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun GhostCadenceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CadenceBorderHi, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = CadenceText,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Conic-gradient Cadence mark with an inner waveform. */
@Composable
fun CadenceMark(size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val conicBrush = Brush.sweepGradient(listOf(CadenceBlue, CadenceOrange, CadenceBlue))
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 32))
            .background(conicBrush)
            .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(percent = 32)),
        contentAlignment = Alignment.Center,
    ) {
        // Inner dark cutout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.14f)
                .clip(RoundedCornerShape(percent = 22))
                .background(CadenceBg),
            contentAlignment = Alignment.Center,
        ) {
            CadenceWaveform(maxBarHeight = size * 0.42f)
        }
    }
}

@Composable
private fun CadenceWaveform(maxBarHeight: Dp) {
    val heights = listOf(0.4f, 0.85f, 0.55f, 1f, 0.55f, 0.85f, 0.4f)
    val colors  = listOf(CadenceBlue, CadenceBlue, CadenceBlue, Color.White, CadenceOrange, CadenceOrange, CadenceOrange)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
        heights.forEachIndexed { i, h ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(maxBarHeight * h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors[i]),
            )
        }
    }
}

@Suppress("unused")
val SkipLabel: @Composable () -> Unit = {
    Text(
        text = "Skip",
        color = CadenceTextMute,
        style = MaterialTheme.typography.titleSmall,
    )
}

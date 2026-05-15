package io.cadence.music.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDim
import io.cadence.music.ui.theme.CadenceOrangeDimHi
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceSurfaceHi
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextDim
import io.cadence.music.ui.theme.CadenceTextMute
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ReadyScreen(onStartListening: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CadenceBg)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingTopBar(step = 4)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
            ) {
                LivePreviewCard(title = "Morning Drift", subtitle = "seeded from your taste · 92 BPM")

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "YOUR PROFILE",
                    color = CadenceTextMute,
                    style = MaterialTheme.typography.labelMedium,
                )

                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileRow(label = "Signal source", value = "Health Connect", dot = CadenceBlue)
                    ProfileRow(label = "Resting HR", value = "calibrating", dot = CadenceOrange)
                    ProfileRow(label = "Default scene", value = "auto-detect", dot = CadenceBlue)
                    ProfileRow(label = "Pipeline", value = "biometric → style → song", dot = CadenceOrange)
                }

                Spacer(Modifier.weight(1f))

                PrimaryCadenceButton(
                    text = "Start listening",
                    onClick = onStartListening,
                    tone = CadenceButtonTone.Orange,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Your first session calibrates Cadence to your\nbaseline. Headphones recommended.",
                    color = CadenceTextDim,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun LivePreviewCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(CadenceSurfaceHi, CadenceSurface)))
            .border(1.dp, CadenceBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(CadenceOrangeDim),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CadenceOrange),
                    )
                }
                Text(
                    text = "LIVE PREVIEW",
                    color = CadenceOrange,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = CadenceText,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = CadenceTextMute,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(18.dp))
            Waveform()
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0:42", color = CadenceTextDim, style = MaterialTheme.typography.labelSmall)
                Text("∞", color = CadenceTextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Waveform() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val bars = 42
        repeat(bars) { i ->
            val h = 0.25f + 0.75f * (sin(i * 0.55) * cos(i * 0.18)).absoluteValue.toFloat()
            val past = i < 18
            Box(
                modifier = Modifier
                    .height((48 * h).dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (past) CadenceOrange else Color.White.copy(alpha = 0.18f)),
            )
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String, dot: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CadenceSurface)
            .border(1.dp, CadenceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = label,
            color = CadenceTextMute,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = CadenceText,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
        )
    }
}

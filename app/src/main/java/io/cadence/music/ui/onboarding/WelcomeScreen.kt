package io.cadence.music.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBlueDimHi
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDim
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextMute

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CadenceBg)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            // Hero — fills available space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BiometricRing()

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "C · A · D · E · N · C · E",
                    color = CadenceOrange,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Music that\nmoves with ")
                        withStyle(SpanStyle(color = CadenceOrange, fontWeight = FontWeight.SemiBold)) {
                            append("you")
                        }
                        append(".")
                    },
                    color = CadenceText,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Generative tracks tuned in real time to your heart rate, motion and surroundings.",
                    color = CadenceTextMute,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            // CTA cluster
            PrimaryCadenceButton(
                text = "Get started",
                onClick = onGetStarted,
                tone = CadenceButtonTone.Blue,
            )
        }
    }
}

@Composable
private fun BiometricRing() {
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ring (blue)
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .border(1.dp, CadenceBlueDimHi, CircleShape),
        )
        // Middle ring (orange)
        Box(
            modifier = Modifier
                .size(184.dp)
                .clip(CircleShape)
                .border(1.dp, CadenceOrangeDim, CircleShape),
        )
        // Inner radial blue glow
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CadenceBlueDimHi, Color.Transparent),
                    )
                ),
        )
        // Gradient arc
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            val padding = 32.dp.toPx()
            val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
            drawArc(
                brush = Brush.linearGradient(listOf(CadenceBlue, CadenceOrange)),
                startAngle = -90f,
                sweepAngle = 230f,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = arcSize,
                style = stroke,
            )
        }
        // Center mark
        CadenceMark(size = 92.dp)
    }
}

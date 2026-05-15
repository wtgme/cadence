package io.cadence.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.cadence.music.data.model.Scene
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceBorderHi
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDim
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextMute

@Composable
fun ActivityPickerMenu(
    expanded: Boolean,
    currentScene: Scene?,
    onSelect: (Scene) -> Unit,
    onAutoDetect: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!expanded) return

    Popup(
        alignment   = Alignment.TopStart,
        offset      = IntOffset(0, 120),
        onDismissRequest = onDismiss,
        properties  = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(228.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CadenceSurface)
                .border(1.dp, CadenceBorderHi, RoundedCornerShape(16.dp))
                .padding(6.dp),
        ) {
            Text(
                text     = "PICK ACTIVITY",
                color    = CadenceTextMute,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            )

            Scene.entries.forEach { scene ->
                ActivityRow(
                    label  = scene.displayName(),
                    icon   = scene.icon(),
                    active = scene == currentScene,
                    onClick = { onSelect(scene) },
                )
            }

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 8.dp)
                    .background(CadenceBorder),
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAutoDetect)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = CadenceBlue,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text  = "Auto-detect",
                    color = CadenceBlue,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) CadenceOrangeDim else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) CadenceOrange else CadenceTextMute,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text  = label,
            color = if (active) CadenceOrange else CadenceText,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (active) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CadenceOrange,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun Scene.icon(): ImageVector = when (this) {
    Scene.RUNNING   -> Icons.AutoMirrored.Filled.DirectionsRun
    Scene.CYCLING   -> Icons.AutoMirrored.Filled.DirectionsBike
    Scene.WALKING   -> Icons.AutoMirrored.Filled.DirectionsWalk
    Scene.COMMUTING -> Icons.Default.DirectionsCar
    Scene.WORKOUT   -> Icons.Default.FitnessCenter
    Scene.FOCUS     -> Icons.Default.CenterFocusStrong
    Scene.PARTY     -> Icons.Default.Celebration
    Scene.RESTING   -> Icons.Default.Bedtime
}

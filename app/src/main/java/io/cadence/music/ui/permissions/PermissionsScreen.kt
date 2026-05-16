package io.cadence.music.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
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
import io.cadence.music.ui.onboarding.CadenceButtonTone
import io.cadence.music.ui.onboarding.OnboardingTopBar
import io.cadence.music.ui.onboarding.PrimaryCadenceButton
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBlueDim
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDim
import io.cadence.music.ui.theme.CadenceOrangeDimHi
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextMute
import kotlinx.coroutines.launch

internal val HEALTH_CONNECT_PERMISSIONS = setOf(
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
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var locationGranted by remember { mutableStateOf(false) }
    var healthGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }

    LaunchedEffect(Unit) {
        val fineGranted   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        locationGranted = fineGranted || coarseGranted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            healthGranted = HEALTH_CONNECT_PERMISSIONS.all { it in granted }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val healthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                healthGranted = HEALTH_CONNECT_PERMISSIONS.all { it in granted }
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CadenceBg)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingTopBar(step = 1)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                    Text(
                        text = "STEP 02 / PERMISSIONS",
                        color = CadenceBlue,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Grant access to\nyour signals",
                        color = CadenceText,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Cadence reads these locally on-device. Nothing is uploaded raw.",
                        color = CadenceTextMute,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PermissionRow(
                        icon = Icons.Default.Favorite,
                        title = "Health & biosignals",
                        body = "Heart rate, motion, sleep readiness. Drives BPM-matched generation.",
                        tag = PermTag.Required,
                        granted = healthGranted,
                        onToggle = { healthLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
                    )
                    PermissionRow(
                        icon = Icons.Default.LocationOn,
                        title = "Location & motion",
                        body = "Detects walking, running, transit. Lets the soundtrack switch with the scene.",
                        tag = PermTag.Required,
                        granted = locationGranted,
                        onToggle = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        },
                    )
                    PermissionRow(
                        icon = Icons.Default.Mic,
                        title = "Microphone",
                        body = "Sample ambient timbre to colour mixes. Audio never leaves the device.",
                        tag = PermTag.Optional,
                        granted = false,
                        onToggle = { /* not currently requested */ },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            body = "Background playback controls and session reminders.",
                            tag = PermTag.Optional,
                            granted = notificationsGranted,
                            onToggle = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
            ) {
                PrimaryCadenceButton(
                    text = if (locationGranted) "Continue" else "Allow & continue",
                    onClick = {
                        if (locationGranted) onAllGranted()
                        else locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                    tone = CadenceButtonTone.Blue,
                    enabled = true,
                )
            }
        }
    }
}

private enum class PermTag { Required, Optional }

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    body: String,
    tag: PermTag,
    granted: Boolean,
    onToggle: () -> Unit,
) {
    val borderColor = if (granted) CadenceBlue else CadenceBorder
    val bgColor = if (granted) CadenceBlueDim else CadenceSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (granted) CadenceBlue else Color.White.copy(alpha = 0.04f))
                .border(
                    1.dp,
                    if (granted) Color.Transparent else CadenceBorder,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) CadenceBg else CadenceTextMute,
                modifier = Modifier.size(20.dp),
            )
        }

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = CadenceText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                PermTagPill(tag)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                color = CadenceTextMute,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Toggle
        ToggleSwitch(checked = granted, onClick = onToggle)
    }
}

@Composable
private fun PermTagPill(tag: PermTag) {
    val (txt, fg, bg, border) = when (tag) {
        PermTag.Required -> Quad("REQUIRED", CadenceOrange, CadenceOrangeDim, CadenceOrangeDimHi)
        PermTag.Optional -> Quad("OPTIONAL", CadenceTextMute, Color.White.copy(alpha = 0.06f), Color.Transparent)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = txt,
            color = fg,
            fontSize = 9.sp,
            letterSpacing = 0.6.sp,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun ToggleSwitch(checked: Boolean, onClick: () -> Unit) {
    val bgColor = if (checked) CadenceBlue else Color.White.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

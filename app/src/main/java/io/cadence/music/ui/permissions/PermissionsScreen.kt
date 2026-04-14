package io.cadence.music.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import io.cadence.music.ui.theme.GlowDefault
import io.cadence.music.ui.theme.Surface0
import io.cadence.music.ui.theme.Surface1
import io.cadence.music.ui.theme.Surface2
import io.cadence.music.ui.theme.TextPrimary
import io.cadence.music.ui.theme.TextSecondary
import io.cadence.music.ui.theme.TextTertiary
import kotlinx.coroutines.delay
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
    val scope   = rememberCoroutineScope()

    var locationGranted      by remember { mutableStateOf(false) }
    var healthGranted        by remember { mutableStateOf(false) }
    var notificationsGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }

    // Stagger reveal animation
    var showBranding by remember { mutableStateOf(false) }
    var showItems    by remember { mutableStateOf(false) }
    var showCta      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Check current permission state
        val fineGranted   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        locationGranted = fineGranted || coarseGranted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            val client  = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            healthGranted = HEALTH_CONNECT_PERMISSIONS.all { it in granted }
        }

        // Stagger
        showBranding = true
        delay(200)
        showItems = true
        delay(300)
        showCta = true
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
    }

    val healthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                val client  = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                healthGranted = HEALTH_CONNECT_PERMISSIONS.all { it in granted }
            }
            if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
    }

    val bg = Brush.verticalGradient(listOf(Surface0, Color(0xFF080814), Surface0))

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Branding ──────────────────────────────────────────────
                AnimatedVisibility(
                    visible = showBranding,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -30 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Logo mark
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GlowDefault.copy(alpha = 0.15f))
                                .border(1.dp, GlowDefault.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = GlowDefault,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "CADENCE",
                            style = MaterialTheme.typography.labelLarge,
                            color = GlowDefault,
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Music that moves\nwith you",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Cadence reads your biometrics and environment to generate music that adapts to your physiological state in real time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ── Permission items ─────────────────────────────────────
                AnimatedVisibility(
                    visible = showItems,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PermissionFeatureItem(
                            icon        = Icons.Default.LocationOn,
                            title       = "Location",
                            description = "Detects your speed and activity context.",
                            isGranted   = locationGranted,
                            badge       = "Required",
                            onClick     = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    )
                                )
                            },
                        )

                        PermissionFeatureItem(
                            icon        = Icons.Default.Favorite,
                            title       = "Health Data",
                            description = "Heart rate, steps, sleep and readiness. Enables BPM-aware generation and sleep-based mood calibration.",
                            isGranted   = healthGranted,
                            onClick     = { healthLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionFeatureItem(
                                icon        = Icons.Default.Notifications,
                                title       = "Notifications",
                                description = "Background playback controls.",
                                isGranted   = notificationsGranted,
                                badge       = "Optional",
                                onClick     = {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ── CTA ───────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = showCta,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = onAllGranted,
                            enabled = locationGranted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor         = GlowDefault,
                                disabledContainerColor = GlowDefault.copy(alpha = 0.25f),
                                contentColor           = Color.Black,
                                disabledContentColor   = Color.Black.copy(alpha = 0.4f),
                            ),
                        ) {
                            Text(
                                text       = if (locationGranted) "Start Listening" else "Grant Location to Continue",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (!healthGranted) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text  = "Health access is optional but improves personalisation.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val borderColor = if (isGranted) GlowDefault.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
    val bgColor     = if (isGranted) GlowDefault.copy(alpha = 0.07f) else Surface1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = if (isGranted) ({}) else onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) GlowDefault.copy(alpha = 0.2f)
                    else Surface2
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (isGranted) GlowDefault else TextSecondary,
                modifier           = Modifier.size(22.dp),
            )
        }

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (badge != null) {
                    val badgeColor = if (badge == "Required") Color(0xFFFF6B35) else TextTertiary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text  = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp),
                        )
                    }
                }
            }
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        // Checkmark / tap to grant
        AnimatedVisibility(visible = isGranted) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(GlowDefault.copy(alpha = 0.2f))
                    .border(1.dp, GlowDefault.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector     = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint            = GlowDefault,
                    modifier        = Modifier.size(16.dp),
                )
            }
        }

        AnimatedVisibility(visible = !isGranted) {
            Button(
                onClick = onClick,
                modifier = Modifier.width(72.dp).height(34.dp),
                shape   = RoundedCornerShape(10.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = GlowDefault.copy(alpha = 0.18f),
                    contentColor   = GlowDefault,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text("Allow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

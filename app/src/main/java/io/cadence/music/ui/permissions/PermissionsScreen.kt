package io.cadence.music.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

private val HEALTH_CONNECT_PERMISSIONS = setOf(
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
    var notificationsGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }

    val mainGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A1A),
            Color(0xFF0A0A0A),
            Color(0xFF050505)
        )
    )

    LaunchedEffect(Unit) {
        // Check current location permission state (fine or coarse both suffice)
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        locationGranted = fineGranted || coarseGranted

        // Check notification permission state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        // Check health connect permissions
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
        if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
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
            if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (locationGranted && healthGranted && notificationsGranted) onAllGranted()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainGradient)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CADENCE",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF4CAF50),
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Personalize Your Soundscape",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Cadence reads your real-time health metrics to synthesize music that adapts to your physiological state.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(Modifier.height(48.dp))

            PermissionButton(
                text = if (locationGranted) "Location Access Granted" else "Grant Location Access",
                isGranted = locationGranted,
                onClick = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            PermissionButton(
                text = if (healthGranted) "Health Data Granted" else "Grant Health Data Access",
                isGranted = healthGranted,
                onClick = { healthLauncher.launch(HEALTH_CONNECT_PERMISSIONS) }
            )
            
            Text(
                text = "Connects to heart rate, steps, and sleep data.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(12.dp))
                PermissionButton(
                    text = if (notificationsGranted) "Notifications Granted" else "Grant Notifications",
                    isGranted = notificationsGranted,
                    onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { onAllGranted() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                ),
                enabled = locationGranted,
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }

            if (!healthGranted) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Health data is optional but enhances the adaptive experience.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PermissionButton(
    text: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isGranted) Color.White.copy(alpha = 0.1f) else Color(0xFF1A1A1A),
            contentColor = if (isGranted) Color(0xFF4CAF50) else Color.White
        ),
        border = if (isGranted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f)) else null,
        enabled = !isGranted,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

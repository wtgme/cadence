package io.cadence.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.ui.theme.Surface0
import io.cadence.music.ui.theme.Surface1
import io.cadence.music.ui.theme.Surface2
import io.cadence.music.ui.theme.TextPrimary
import io.cadence.music.ui.theme.TextSecondary
import io.cadence.music.ui.theme.TextTertiary
import io.cadence.music.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val defaults = viewModel.defaults

    var s2sBaseUrl by remember(settings) { mutableStateOf(settings.signal2StyleBaseUrl) }
    var s2sApiKey  by remember(settings) { mutableStateOf(settings.signal2StyleApiKey) }
    var s2sModel   by remember(settings) { mutableStateOf(settings.signal2StyleModel) }
    var sgBaseUrl  by remember(settings) { mutableStateOf(settings.songGenBaseUrl) }
    var sgApiKey   by remember(settings) { mutableStateOf(settings.songGenApiKey) }
    var sgModel    by remember(settings) { mutableStateOf(settings.songGenModel) }

    var s2sKeyVisible by remember { mutableStateOf(false) }
    var sgKeyVisible  by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = TextPrimary,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text  = "API Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Override defaults from local.properties. Changes take effect on the next generation.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(20.dp))

            SectionCard(title = "Step 1 · Signal2Style (LLM)") {
                SettingField(
                    label         = "Base URL",
                    value         = s2sBaseUrl,
                    placeholder   = defaults.signal2StyleBaseUrl,
                    onValueChange = { s2sBaseUrl = it },
                )
                SecretField(
                    label              = "API key",
                    value              = s2sApiKey,
                    visible            = s2sKeyVisible,
                    onToggleVisibility = { s2sKeyVisible = !s2sKeyVisible },
                    onValueChange      = { s2sApiKey = it },
                )
                SettingField(
                    label         = "Model",
                    value         = s2sModel,
                    placeholder   = defaults.signal2StyleModel,
                    onValueChange = { s2sModel = it },
                )
                TextButton(onClick = {
                    s2sBaseUrl = defaults.signal2StyleBaseUrl
                    s2sApiKey  = defaults.signal2StyleApiKey
                    s2sModel   = defaults.signal2StyleModel
                }) {
                    Text("Reset this section", color = WarningAmber)
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Step 2 · SongGen (music)") {
                SettingField(
                    label         = "Base URL",
                    value         = sgBaseUrl,
                    placeholder   = defaults.songGenBaseUrl,
                    onValueChange = { sgBaseUrl = it },
                )
                SecretField(
                    label              = "API key",
                    value              = sgApiKey,
                    visible            = sgKeyVisible,
                    onToggleVisibility = { sgKeyVisible = !sgKeyVisible },
                    onValueChange      = { sgApiKey = it },
                )
                SettingField(
                    label         = "Model",
                    value         = sgModel,
                    placeholder   = defaults.songGenModel,
                    onValueChange = { sgModel = it },
                )
                TextButton(onClick = {
                    sgBaseUrl = defaults.songGenBaseUrl
                    sgApiKey  = defaults.songGenApiKey
                    sgModel   = defaults.songGenModel
                }) {
                    Text("Reset this section", color = WarningAmber)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.save(
                        signal2StyleBaseUrl = s2sBaseUrl,
                        signal2StyleApiKey  = s2sApiKey,
                        signal2StyleModel   = s2sModel,
                        songGenBaseUrl      = sgBaseUrl,
                        songGenApiKey       = sgApiKey,
                        songGenModel        = sgModel,
                    ) { result ->
                        toast(when (result) {
                            SettingsViewModel.Result.Saved      -> "Saved"
                            is SettingsViewModel.Result.Invalid -> result.message
                        })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextPrimary),
                shape    = RoundedCornerShape(12.dp),
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick  = { viewModel.resetAll { toast("All settings reset to defaults") } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset all to defaults", color = WarningAmber)
            }

            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data ->
            Snackbar(
                containerColor = Surface2,
                contentColor   = TextPrimary,
            ) { Text(data.visuals.message) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = TextSecondary) },
        placeholder   = { Text(placeholder, color = TextTertiary) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors        = fieldColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretField(
    label: String,
    value: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label, color = TextSecondary) },
        placeholder          = { Text("(empty)", color = TextTertiary) },
        singleLine           = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector        = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key",
                    tint               = TextSecondary,
                )
            }
        },
        modifier             = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors               = fieldColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = TextPrimary,
    unfocusedTextColor     = TextPrimary,
    focusedBorderColor     = TextSecondary,
    unfocusedBorderColor   = TextTertiary,
    cursorColor            = TextPrimary,
)

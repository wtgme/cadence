package io.cadence.music.ui.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.ui.onboarding.CadenceButtonTone
import io.cadence.music.ui.onboarding.PrimaryCadenceButton
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBlueDim
import io.cadence.music.ui.theme.CadenceBlueDimHi
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextDim
import io.cadence.music.ui.theme.CadenceTextMute
import io.cadence.music.ui.theme.FontJetBrainsMono
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
            .background(CadenceBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // App bar — matches PlayerScreen's top bar style
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = CadenceText,
                        modifier           = Modifier.size(16.dp),
                    )
                }
                Text(
                    text  = "API SETTINGS",
                    style = MaterialTheme.typography.labelLarge,
                    color = CadenceOrange,
                )
                Spacer(Modifier.size(36.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text  = "Override defaults from ",
                        color = CadenceTextMute,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text  = "local.properties",
                        color = CadenceText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontJetBrainsMono),
                    )
                }
                Text(
                    text  = "Changes take effect on the next generation.",
                    color = CadenceTextMute,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(18.dp))

                SettingsSection(
                    step  = "STEP 1",
                    title = "Signal2Style",
                    sub   = "LLM",
                    onReset = {
                        s2sBaseUrl = defaults.signal2StyleBaseUrl
                        s2sApiKey  = defaults.signal2StyleApiKey
                        s2sModel   = defaults.signal2StyleModel
                    },
                ) {
                    SettingField(
                        label = "Base URL",
                        value = s2sBaseUrl,
                        placeholder = defaults.signal2StyleBaseUrl,
                        onValueChange = { s2sBaseUrl = it },
                    )
                    SecretField(
                        label   = "API key",
                        value   = s2sApiKey,
                        visible = s2sKeyVisible,
                        onToggleVisibility = { s2sKeyVisible = !s2sKeyVisible },
                        onValueChange = { s2sApiKey = it },
                    )
                    SettingField(
                        label = "Model",
                        value = s2sModel,
                        placeholder = defaults.signal2StyleModel,
                        onValueChange = { s2sModel = it },
                    )
                }

                Spacer(Modifier.height(12.dp))

                SettingsSection(
                    step  = "STEP 2",
                    title = "SongGen",
                    sub   = "music",
                    onReset = {
                        sgBaseUrl = defaults.songGenBaseUrl
                        sgApiKey  = defaults.songGenApiKey
                        sgModel   = defaults.songGenModel
                    },
                ) {
                    SettingField(
                        label = "Base URL",
                        value = sgBaseUrl,
                        placeholder = defaults.songGenBaseUrl,
                        onValueChange = { sgBaseUrl = it },
                    )
                    SecretField(
                        label   = "API key",
                        value   = sgApiKey,
                        visible = sgKeyVisible,
                        onToggleVisibility = { sgKeyVisible = !sgKeyVisible },
                        onValueChange = { sgApiKey = it },
                    )
                    SettingField(
                        label = "Model",
                        value = sgModel,
                        placeholder = defaults.songGenModel,
                        onValueChange = { sgModel = it },
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Info banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CadenceBlueDim)
                        .border(1.dp, CadenceBlueDimHi, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CadenceBlue,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp),
                    )
                    Text(
                        text  = "Keys are stored in the device keystore and never sync to Cadence servers.",
                        color = CadenceText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(18.dp))

                PrimaryCadenceButton(
                    text = "Save",
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
                    tone = CadenceButtonTone.Blue,
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.resetAll { toast("All settings reset to defaults") } }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Reset all to defaults",
                        color = CadenceOrange,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data ->
            Snackbar(
                containerColor = CadenceSurface,
                contentColor   = CadenceText,
            ) { Text(data.visuals.message) }
        }
    }
}

@Composable
private fun SettingsSection(
    step: String,
    title: String,
    sub: String,
    onReset: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CadenceSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = step,
                    color = CadenceBlue,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text  = title,
                    color = CadenceText,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text  = sub,
                    color = CadenceTextMute,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text     = "Reset this section",
                color    = CadenceOrange,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReset)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            )
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
    StyledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = CadenceTextDim,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontJetBrainsMono),
                )
            },
            singleLine = true,
            textStyle = TextStyle(color = CadenceText, fontFamily = FontJetBrainsMono, fontSize = 14.sp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
    StyledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "(empty)",
                    color = CadenceTextDim,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontJetBrainsMono),
                )
            },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector        = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key",
                    tint               = CadenceTextMute,
                    modifier           = Modifier
                        .size(18.dp)
                        .clickable(onClick = onToggleVisibility),
                )
            },
            textStyle = TextStyle(color = CadenceText, fontFamily = FontJetBrainsMono, fontSize = 14.sp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StyledField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text     = label.uppercase(),
            color    = CadenceTextMute,
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = CadenceText,
    unfocusedTextColor     = CadenceText,
    focusedBorderColor     = CadenceBlue,
    unfocusedBorderColor   = CadenceBorder,
    cursorColor            = CadenceBlue,
    focusedContainerColor  = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)


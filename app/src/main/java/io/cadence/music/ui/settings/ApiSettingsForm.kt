package io.cadence.music.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import io.cadence.music.data.settings.ApiSettings
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceBorder
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextDim
import io.cadence.music.ui.theme.CadenceTextMute
import io.cadence.music.ui.theme.FontJetBrainsMono

/** Mutable, in-screen draft of the six API fields. */
data class ApiSettingsDraft(
    val signal2StyleBaseUrl: String,
    val signal2StyleApiKey:  String,
    val signal2StyleModel:   String,
    val songGenBaseUrl:      String,
    val songGenApiKey:       String,
    val songGenModel:        String,
) {
    companion object {
        fun from(s: ApiSettings) = ApiSettingsDraft(
            signal2StyleBaseUrl = s.signal2StyleBaseUrl,
            signal2StyleApiKey  = s.signal2StyleApiKey,
            signal2StyleModel   = s.signal2StyleModel,
            songGenBaseUrl      = s.songGenBaseUrl,
            songGenApiKey       = s.songGenApiKey,
            songGenModel        = s.songGenModel,
        )
    }
}

/**
 * Two-section form (Signal2Style + SongGen) with base URL, API key, and model fields each.
 * Used by [SettingsScreen] and by the onboarding API setup screen.
 */
@Composable
fun ApiSettingsForm(
    draft: ApiSettingsDraft,
    defaults: ApiSettings,
    onChange: (ApiSettingsDraft) -> Unit,
) {
    var s2sKeyVisible by remember { mutableStateOf(false) }
    var sgKeyVisible  by remember { mutableStateOf(false) }

    SettingsSection(
        step  = "STEP 1",
        title = "Signal2Style",
        sub   = "LLM",
        onReset = {
            onChange(draft.copy(
                signal2StyleBaseUrl = defaults.signal2StyleBaseUrl,
                signal2StyleApiKey  = defaults.signal2StyleApiKey,
                signal2StyleModel   = defaults.signal2StyleModel,
            ))
        },
    ) {
        SettingField(
            label       = "Base URL",
            value       = draft.signal2StyleBaseUrl,
            placeholder = defaults.signal2StyleBaseUrl,
            onValueChange = { onChange(draft.copy(signal2StyleBaseUrl = it)) },
        )
        SecretField(
            label   = "API key",
            value   = draft.signal2StyleApiKey,
            visible = s2sKeyVisible,
            onToggleVisibility = { s2sKeyVisible = !s2sKeyVisible },
            onValueChange = { onChange(draft.copy(signal2StyleApiKey = it)) },
        )
        SettingField(
            label       = "Model",
            value       = draft.signal2StyleModel,
            placeholder = defaults.signal2StyleModel,
            onValueChange = { onChange(draft.copy(signal2StyleModel = it)) },
        )
    }

    Spacer(Modifier.height(12.dp))

    SettingsSection(
        step  = "STEP 2",
        title = "SongGen",
        sub   = "music",
        onReset = {
            onChange(draft.copy(
                songGenBaseUrl = defaults.songGenBaseUrl,
                songGenApiKey  = defaults.songGenApiKey,
                songGenModel   = defaults.songGenModel,
            ))
        },
    ) {
        SettingField(
            label       = "Base URL",
            value       = draft.songGenBaseUrl,
            placeholder = defaults.songGenBaseUrl,
            onValueChange = { onChange(draft.copy(songGenBaseUrl = it)) },
        )
        SecretField(
            label   = "API key",
            value   = draft.songGenApiKey,
            visible = sgKeyVisible,
            onToggleVisibility = { sgKeyVisible = !sgKeyVisible },
            onValueChange = { onChange(draft.copy(songGenApiKey = it)) },
        )
        SettingField(
            label       = "Model",
            value       = draft.songGenModel,
            placeholder = defaults.songGenModel,
            onValueChange = { onChange(draft.copy(songGenModel = it)) },
        )
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
                Text(step,  color = CadenceBlue,     style = MaterialTheme.typography.labelMedium)
                Text(title, color = CadenceText,     style = MaterialTheme.typography.titleMedium)
                Text(sub,   color = CadenceTextMute, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
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
                    text  = placeholder,
                    color = CadenceTextDim,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontJetBrainsMono),
                )
            },
            singleLine = true,
            textStyle  = TextStyle(color = CadenceText, fontFamily = FontJetBrainsMono, fontSize = 14.sp),
            colors     = fieldColors(),
            modifier   = Modifier.fillMaxWidth(),
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
                    text  = "(empty)",
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
            colors    = fieldColors(),
            modifier  = Modifier.fillMaxWidth(),
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
    focusedTextColor        = CadenceText,
    unfocusedTextColor      = CadenceText,
    focusedBorderColor      = CadenceBlue,
    unfocusedBorderColor    = CadenceBorder,
    cursorColor             = CadenceBlue,
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

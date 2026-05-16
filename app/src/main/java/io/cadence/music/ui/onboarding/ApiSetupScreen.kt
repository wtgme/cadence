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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cadence.music.ui.settings.ApiSettingsDraft
import io.cadence.music.ui.settings.ApiSettingsForm
import io.cadence.music.ui.settings.SettingsViewModel
import io.cadence.music.ui.theme.CadenceBg
import io.cadence.music.ui.theme.CadenceBlue
import io.cadence.music.ui.theme.CadenceOrange
import io.cadence.music.ui.theme.CadenceOrangeDim
import io.cadence.music.ui.theme.CadenceOrangeDimHi
import io.cadence.music.ui.theme.CadenceSurface
import io.cadence.music.ui.theme.CadenceText
import io.cadence.music.ui.theme.CadenceTextMute
import kotlinx.coroutines.launch

/**
 * Onboarding API setup. Shows defaults pre-filled, warns the user that the
 * defaults are the developer's personal endpoints, and lets them substitute
 * their own keys. Reused in two contexts (mid-onboarding and existing-user
 * one-shot after update) via flow-specific [onSaveAndContinue].
 */
@Composable
fun ApiSetupScreen(
    onSaveAndContinue: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val defaults = viewModel.defaults

    var draft by remember(settings) { mutableStateOf(ApiSettingsDraft.from(settings)) }

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
            .background(CadenceBg)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingTopBar(step = 2)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
            ) {
                Text(
                    text  = "STEP 03 / API SETUP",
                    color = CadenceBlue,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "Bring your\nown keys",
                    color = CadenceText,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "Cadence ships with shared default endpoints so you can try it immediately — but you'll get faster, more reliable generation with your own.",
                    color = CadenceTextMute,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(20.dp))

                // Warning banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CadenceOrangeDim)
                        .border(1.dp, CadenceOrangeDimHi, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.Top,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = null,
                        tint               = CadenceOrange,
                        modifier           = Modifier.size(18.dp).padding(top = 1.dp),
                    )
                    Text(
                        text  = "The default endpoints use the developer's personal account. They can be slow or rate-limited under load. We recommend using your own keys.",
                        color = CadenceText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(18.dp))

                ApiSettingsForm(
                    draft    = draft,
                    defaults = defaults,
                    onChange = { draft = it },
                )

                Spacer(Modifier.height(18.dp))

                PrimaryCadenceButton(
                    text = "Save & continue",
                    onClick = {
                        viewModel.save(
                            signal2StyleBaseUrl = draft.signal2StyleBaseUrl,
                            signal2StyleApiKey  = draft.signal2StyleApiKey,
                            signal2StyleModel   = draft.signal2StyleModel,
                            songGenBaseUrl      = draft.songGenBaseUrl,
                            songGenApiKey       = draft.songGenApiKey,
                            songGenModel        = draft.songGenModel,
                        ) { result ->
                            when (result) {
                                SettingsViewModel.Result.Saved      -> onSaveAndContinue()
                                is SettingsViewModel.Result.Invalid -> toast(result.message)
                            }
                        }
                    },
                    tone = CadenceButtonTone.Blue,
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text       = "You can change these anytime from Settings (top-right of the player).",
                    color      = CadenceTextMute,
                    style      = MaterialTheme.typography.bodySmall,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth(),
                )
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

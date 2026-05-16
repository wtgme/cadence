package io.cadence.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cadence.music.BuildConfig
import io.cadence.music.data.onboarding.OnboardingRepository
import io.cadence.music.ui.debug.DebugScreen
import io.cadence.music.ui.onboarding.ApiSetupScreen
import io.cadence.music.ui.onboarding.ReadyScreen
import io.cadence.music.ui.onboarding.WelcomeScreen
import io.cadence.music.ui.permissions.PermissionsScreen
import io.cadence.music.ui.player.PlayerScreen
import io.cadence.music.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private object Routes {
    const val WELCOME              = "welcome"
    const val PERMISSIONS          = "permissions"
    const val API_SETUP            = "api_setup"             // mid-onboarding (new users)
    const val API_SETUP_FIRST_RUN  = "api_setup_first_run"   // one-shot for users who onboarded pre-feature
    const val READY                = "ready"
    const val PLAYER               = "player"
    const val DEBUG                = "debug"
    const val SETTINGS             = "settings"
}

@HiltViewModel
class OnboardingNavViewModel @Inject constructor(
    private val onboardingRepo: OnboardingRepository,
) : ViewModel() {
    val completed: StateFlow<Boolean?> = onboardingRepo.completed
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val apiSetupSeen: StateFlow<Boolean?> = onboardingRepo.apiSetupSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markComplete() {
        viewModelScope.launch { onboardingRepo.markCompleted() }
    }

    fun markApiSetupSeen() {
        viewModelScope.launch { onboardingRepo.markApiSetupSeen() }
    }
}

@Composable
fun CadenceNavHost() {
    val navController = rememberNavController()
    val onboardingVm: OnboardingNavViewModel = hiltViewModel()
    val onboarded     by onboardingVm.completed.collectAsState()
    val apiSetupSeen  by onboardingVm.apiSetupSeen.collectAsState()

    // Wait until both flags resolve, then decide where to start.
    val startDestination = remember(onboarded, apiSetupSeen) {
        when {
            onboarded == null || apiSetupSeen == null -> null      // still loading
            onboarded == false                        -> Routes.WELCOME
            apiSetupSeen == false                     -> Routes.API_SETUP_FIRST_RUN
            else                                      -> Routes.PLAYER
        }
    }

    if (startDestination == null) return

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.PERMISSIONS) },
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate(Routes.API_SETUP) {
                        popUpTo(Routes.WELCOME) { inclusive = false }
                    }
                }
            )
        }

        // Mid-onboarding API setup: doesn't mark apiSetupSeen — READY's
        // onStartListening sets both flags atomically when onboarding completes.
        composable(Routes.API_SETUP) {
            ApiSetupScreen(
                onSaveAndContinue = {
                    navController.navigate(Routes.READY) {
                        popUpTo(Routes.WELCOME) { inclusive = false }
                    }
                },
            )
        }

        // Existing-user one-shot: came from a pre-feature install. Mark seen
        // on exit and jump straight to PLAYER.
        composable(Routes.API_SETUP_FIRST_RUN) {
            ApiSetupScreen(
                onSaveAndContinue = {
                    onboardingVm.markApiSetupSeen()
                    navController.navigate(Routes.PLAYER) {
                        popUpTo(Routes.API_SETUP_FIRST_RUN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.READY) {
            ReadyScreen(
                onStartListening = {
                    onboardingVm.markComplete()
                    onboardingVm.markApiSetupSeen()
                    navController.navigate(Routes.PLAYER) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PLAYER) {
            PlayerScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // Debug screen only available in debug builds
        if (BuildConfig.DEBUG) {
            composable(Routes.DEBUG) {
                DebugScreen()
            }
        }
    }
}

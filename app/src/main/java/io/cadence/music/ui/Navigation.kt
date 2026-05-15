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
    const val WELCOME = "welcome"
    const val PERMISSIONS = "permissions"
    const val READY = "ready"
    const val PLAYER = "player"
    const val DEBUG = "debug"
    const val SETTINGS = "settings"
}

@HiltViewModel
class OnboardingNavViewModel @Inject constructor(
    private val onboardingRepo: OnboardingRepository,
) : ViewModel() {
    val completed: StateFlow<Boolean?> = onboardingRepo.completed
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markComplete() {
        viewModelScope.launch { onboardingRepo.markCompleted() }
    }
}

@Composable
fun CadenceNavHost() {
    val navController = rememberNavController()
    val onboardingVm: OnboardingNavViewModel = hiltViewModel()
    val onboardedState by onboardingVm.completed.collectAsState()

    // Wait for onboarding flag to load (null) before deciding start destination.
    val startDestination = remember(onboardedState) {
        when (onboardedState) {
            true -> Routes.PLAYER
            else -> Routes.WELCOME
        }
    }

    // Show nothing until we've resolved the flag (avoids a flash of Welcome).
    if (onboardedState == null) return

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.PERMISSIONS) },
                onSignIn = { navController.navigate(Routes.PERMISSIONS) },
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate(Routes.READY) {
                        popUpTo(Routes.WELCOME) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.READY) {
            ReadyScreen(
                onStartListening = {
                    onboardingVm.markComplete()
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

package io.cadence.music.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.cadence.music.BuildConfig
import io.cadence.music.ui.debug.DebugScreen
import io.cadence.music.ui.permissions.PermissionsScreen
import io.cadence.music.ui.player.PlayerScreen

private object Routes {
    const val PERMISSIONS = "permissions"
    const val PLAYER = "player"
    const val DEBUG = "debug"
}

@Composable
fun CadenceNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PERMISSIONS) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate(Routes.PLAYER) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PLAYER) {
            PlayerScreen()
        }

        // Debug screen only available in debug builds
        if (BuildConfig.DEBUG) {
            composable(Routes.DEBUG) {
                DebugScreen()
            }
        }
    }
}

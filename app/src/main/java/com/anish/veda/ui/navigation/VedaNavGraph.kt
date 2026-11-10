package com.anish.veda.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anish.veda.ui.screens.BluetoothScreen
import com.anish.veda.ui.screens.CallSettingsScreen
import com.anish.veda.ui.screens.DashboardScreen
import com.anish.veda.ui.screens.DiagnosticsScreen
import com.anish.veda.ui.screens.GeneralSettingsScreen
import com.anish.veda.ui.screens.NotificationSettingsScreen
import com.anish.veda.ui.screens.PermissionsOnboardingScreen
import com.anish.veda.ui.screens.SplashScreen
import com.anish.veda.viewmodel.BluetoothViewModel
import com.anish.veda.viewmodel.CallSettingsViewModel
import com.anish.veda.viewmodel.DiagnosticsViewModel
import com.anish.veda.viewmodel.GeneralSettingsViewModel
import com.anish.veda.viewmodel.MainViewModel
import com.anish.veda.viewmodel.NotificationSettingsViewModel
import com.anish.veda.viewmodel.PermissionsViewModel

@Composable
fun VedaNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 350 }, animationSpec = tween(280)) +
                    fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -350 }, animationSpec = tween(280)) +
                    fadeOut(animationSpec = tween(280))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -350 }, animationSpec = tween(280)) +
                    fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 350 }, animationSpec = tween(280)) +
                    fadeOut(animationSpec = tween(280))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Dashboard.route) {
            val mainViewModel: MainViewModel = viewModel()
            DashboardScreen(
                navController = navController,
                viewModel = mainViewModel
            )
        }

        composable(Screen.Bluetooth.route) {
            val bluetoothViewModel: BluetoothViewModel = viewModel()
            BluetoothScreen(
                navController = navController,
                viewModel = bluetoothViewModel
            )
        }

        composable(Screen.NotificationSettings.route) {
            val notificationViewModel: NotificationSettingsViewModel = viewModel()
            NotificationSettingsScreen(
                navController = navController,
                viewModel = notificationViewModel
            )
        }

        composable(Screen.CallSettings.route) {
            val callViewModel: CallSettingsViewModel = viewModel()
            CallSettingsScreen(
                navController = navController,
                viewModel = callViewModel
            )
        }

        composable(Screen.GeneralSettings.route) {
            val generalViewModel: GeneralSettingsViewModel = viewModel()
            GeneralSettingsScreen(
                navController = navController,
                viewModel = generalViewModel
            )
        }

        composable(Screen.Diagnostics.route) {
            val diagnosticsViewModel: DiagnosticsViewModel = viewModel()
            DiagnosticsScreen(
                navController = navController,
                viewModel = diagnosticsViewModel
            )
        }

        composable(Screen.PermissionsOnboarding.route) {
            val permissionsViewModel: PermissionsViewModel = viewModel()
            PermissionsOnboardingScreen(
                navController = navController,
                viewModel = permissionsViewModel
            )
        }
    }
}

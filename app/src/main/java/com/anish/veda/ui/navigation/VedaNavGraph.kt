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
import com.anish.veda.ui.screens.AiAssistantScreen
import com.anish.veda.ui.screens.BluetoothScreen
import com.anish.veda.ui.screens.CallSettingsScreen
import com.anish.veda.ui.screens.DashboardScreen
import com.anish.veda.ui.screens.DiagnosticsScreen
import com.anish.veda.ui.screens.GeneralSettingsScreen
import com.anish.veda.ui.screens.GuardianScreen
import com.anish.veda.ui.screens.HapticStudioScreen
import com.anish.veda.ui.screens.HealthScreen
import com.anish.veda.ui.screens.MenuScreen
import com.anish.veda.ui.screens.NotificationSettingsScreen
import com.anish.veda.ui.screens.PermissionsOnboardingScreen
import com.anish.veda.ui.screens.PersonalizationStudioScreen
import com.anish.veda.ui.screens.SplashScreen
import com.anish.veda.ui.screens.WatchRemoteControlScreen
import com.anish.veda.ui.screens.WatchStudioScreen
import com.anish.veda.viewmodel.AiAssistantViewModel
import com.anish.veda.viewmodel.BluetoothViewModel
import com.anish.veda.viewmodel.CallSettingsViewModel
import com.anish.veda.viewmodel.DiagnosticsViewModel
import com.anish.veda.viewmodel.GeneralSettingsViewModel
import com.anish.veda.viewmodel.GuardianViewModel
import com.anish.veda.viewmodel.HapticStudioViewModel
import com.anish.veda.viewmodel.HealthViewModel
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
            slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(240)) +
                    fadeIn(animationSpec = tween(240))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -300 }, animationSpec = tween(240)) +
                    fadeOut(animationSpec = tween(240))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -300 }, animationSpec = tween(240)) +
                    fadeIn(animationSpec = tween(240))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(240)) +
                    fadeOut(animationSpec = tween(240))
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

        composable(Screen.Health.route) {
            val healthViewModel: HealthViewModel = viewModel()
            HealthScreen(
                navController = navController,
                viewModel = healthViewModel
            )
        }

        composable(Screen.AiAssistant.route) {
            val aiViewModel: AiAssistantViewModel = viewModel()
            AiAssistantScreen(
                navController = navController,
                viewModel = aiViewModel
            )
        }

        composable(Screen.NotificationSettings.route) {
            val notificationViewModel: NotificationSettingsViewModel = viewModel()
            NotificationSettingsScreen(
                navController = navController,
                viewModel = notificationViewModel
            )
        }

        composable(Screen.WatchStudio.route) {
            WatchStudioScreen(navController = navController)
        }

        composable(Screen.HapticStudio.route) {
            val hapticViewModel: HapticStudioViewModel = viewModel()
            HapticStudioScreen(
                navController = navController,
                viewModel = hapticViewModel
            )
        }

        composable(Screen.Guardian.route) {
            val guardianViewModel: GuardianViewModel = viewModel()
            GuardianScreen(
                navController = navController,
                viewModel = guardianViewModel
            )
        }

        composable(Screen.Bluetooth.route) {
            val bluetoothViewModel: BluetoothViewModel = viewModel()
            BluetoothScreen(
                navController = navController,
                viewModel = bluetoothViewModel
            )
        }

        composable(Screen.GeneralSettings.route) {
            val generalViewModel: GeneralSettingsViewModel = viewModel()
            GeneralSettingsScreen(
                navController = navController,
                viewModel = generalViewModel
            )
        }

        composable(Screen.CallSettings.route) {
            val callViewModel: CallSettingsViewModel = viewModel()
            CallSettingsScreen(
                navController = navController,
                viewModel = callViewModel
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

        composable(Screen.Menu.route) {
            MenuScreen(navController = navController)
        }

        composable(Screen.PersonalizationStudio.route) {
            PersonalizationStudioScreen(navController = navController)
        }

        composable(Screen.WatchRemoteControl.route) {
            WatchRemoteControlScreen(navController = navController)
        }
    }
}

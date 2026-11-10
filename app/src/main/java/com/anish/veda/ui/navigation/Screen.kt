package com.anish.veda.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Bluetooth : Screen("bluetooth")
    data object NotificationSettings : Screen("notification_settings")
    data object CallSettings : Screen("call_settings")
    data object GeneralSettings : Screen("general_settings")
    data object Diagnostics : Screen("diagnostics")
    data object PermissionsOnboarding : Screen("permissions_onboarding")
}

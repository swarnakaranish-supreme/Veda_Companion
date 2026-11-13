package com.anish.veda.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Health : Screen("health")
    data object AiAssistant : Screen("ai_assistant")
    data object NotificationSettings : Screen("notification_settings")
    data object WatchStudio : Screen("watch_studio")
    data object HapticStudio : Screen("haptic_studio")
    data object Guardian : Screen("guardian")
    data object Bluetooth : Screen("bluetooth")
    data object GeneralSettings : Screen("general_settings")
    data object CallSettings : Screen("call_settings")
    data object Diagnostics : Screen("diagnostics")
    data object PermissionsOnboarding : Screen("permissions_onboarding")
    data object Menu : Screen("menu")
    data object PersonalizationStudio : Screen("personalization_studio")
    data object WatchRemoteControl : Screen("watch_remote_control")
}

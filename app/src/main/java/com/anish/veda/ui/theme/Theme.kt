package com.anish.veda.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = CanvasDeepBlack,
    primaryContainer = SurfaceHighlight,
    onPrimaryContainer = AccentCyan,
    secondary = AccentViolet,
    onSecondary = CanvasDeepBlack,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusSuccess,
    background = CanvasDeepBlack,
    onBackground = TextPrimary,
    surface = SurfaceGraphite,
    onSurface = TextPrimary,
    surfaceVariant = CanvasMidnightNavy,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    error = StatusError,
    onError = CanvasDeepBlack
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008CA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBF1F8),
    onPrimaryContainer = Color(0xFF008CA8),
    secondary = Color(0xFF604ADB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F4FA),
    onSecondaryContainer = Color(0xFF0E1724),
    tertiary = Color(0xFF2E9A65),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0E1724),
    surface = Color.White,
    onSurface = Color(0xFF0E1724),
    surfaceVariant = Color(0xFFEBF1F8),
    onSurfaceVariant = Color(0xFF4E6178),
    outline = Color(0xFFD5E0EC),
    error = Color(0xFFD9384F),
    onError = Color.White
)

fun createVedaColorScheme(
    themePreset: String = "DEFAULT",
    accentColor: Color = AccentCyan
): androidx.compose.material3.ColorScheme {
    val (bg, surface, elevated, onSurface) = when (themePreset.uppercase()) {
        "GRAPHITE" -> Quad(Color(0xFF0B0E14), Color(0xFF151B26), Color(0xFF1E2636), Color(0xFFF3F6FA))
        "MIDNIGHT" -> Quad(Color(0xFF050811), Color(0xFF0D1527), Color(0xFF15223D), Color(0xFFF3F6FA))
        "LIGHT" -> Quad(Color(0xFFF4F7FB), Color(0xFFFFFFFF), Color(0xFFEBF1F8), Color(0xFF0E1724))
        else -> Quad(CanvasPrimary, SurfaceGraphite, SurfaceElevated, TextPrimary)
    }

    return darkColorScheme(
        primary = accentColor,
        onPrimary = if (themePreset.uppercase() == "LIGHT") Color.White else CanvasDeepBlack,
        primaryContainer = elevated,
        onPrimaryContainer = accentColor,
        secondary = AccentViolet,
        onSecondary = CanvasDeepBlack,
        secondaryContainer = elevated,
        onSecondaryContainer = onSurface,
        tertiary = StatusSuccess,
        background = bg,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = elevated,
        onSurfaceVariant = TextSecondary,
        outline = StrokeSubtle,
        error = StatusError,
        onError = CanvasDeepBlack
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun VedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentCyan,
    themePreset: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val colorScheme = createVedaColorScheme(themePreset, accentColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val isLight = themePreset.uppercase() == "LIGHT"
                val barColor = if (isLight) Color(0xFFF4F7FB).toArgb() else CanvasDeepBlack.toArgb()
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

package com.anish.veda.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// VEDA V2.0 PREMIUM COMMERCIAL DESIGN SYSTEM PALETTE
// ============================================================================

// Neutral Graphite Surface & Background Luminance
val CanvasPrimary        = Color(0xFF080B10) // Pure Deep Graphite Background
val CanvasSecondary      = Color(0xFF101720) // Grouped container / Sheet background
val SurfaceGraphite      = Color(0xFF121822) // Neutral Slate Card Surface (No heavy blue)
val SurfaceElevated      = Color(0xFF171F2B) // Raised dialogs, dropdowns, modal cards
val SurfaceHighlight     = Color(0xFF1E2838) // Interactive pressed / hover states

// Aliases for layout consistency
val CanvasDeepBlack      = CanvasPrimary
val CanvasMidnightNavy   = CanvasSecondary

// Subtle Hairline Strokes (Controlled contrast, no glowing outlines)
val StrokeSubtle         = Color(0xFF1E2633) // Standard card border & dividers
val StrokeFocused        = Color(0xFF2B3747) // Focused / active borders
val StrokeAccent         = Color(0x6635D6E8) // Subtle cyan focus hairline (40%)

// Accents (Selective, purposeful, never overwhelming)
val AccentCyan           = Color(0xFF35D6E8) // VEDA Cyan: Primary brand & BLE active
val AccentCyanDim        = Color(0x1A35D6E8) // 10% tint for active containers & pills
val AccentViolet         = Color(0xFF9A85F5) // Premium Violet: AI Intelligence
val AccentVioletDim      = Color(0x1A9A85F5) // 10% tint for AI highlights
val StatusGreen          = Color(0xFF45D49A) // Health Green: Optimal telemetry, connected
val StatusGreenDim       = Color(0x1A45D49A)
val StatusSuccess        = StatusGreen
val StatusSuccessDim     = StatusGreenDim
val StatusWarning        = Color(0xFFF2B84B) // Warning Amber: Attention, sensor settling
val StatusWarningDim     = Color(0x1AF2B84B)
val StatusError          = Color(0xFFF05C70) // Emergency Red: SOS, errors, disconnected
val StatusErrorDim       = Color(0x1AF05C70)

// Typography (High contrast & readability)
val TextPrimary          = Color(0xFFF3F6FA) // High-contrast primary text
val TextSecondary        = Color(0xFFA1ADBB) // Slate secondary / descriptions
val TextTertiary         = Color(0xFF748295) // Muted captions, timestamps, units

// ============================================================================
// BACKWARD COMPATIBILITY ALIASES
// ============================================================================
val VedaBlack            = CanvasPrimary
val VedaDarkNavy         = CanvasSecondary
val VedaCardNavy         = SurfaceGraphite
val VedaSurfaceNavy      = SurfaceElevated
val VedaElevatedNavy     = SurfaceHighlight

val NeonCyan             = AccentCyan
val CyanGlow             = AccentCyan
val StrokeHighlight      = StrokeFocused
val CyberBlue            = AccentCyan
val ElectricIndigo       = AccentViolet
val MatrixGreen          = StatusGreen
val AlertRed             = StatusError
val WarningAmber         = StatusWarning

val TextWhite            = TextPrimary
val TextMuted            = TextSecondary
val TextDim              = TextTertiary

val BorderGlow           = StrokeAccent
val BorderNavy           = StrokeSubtle
val CardGradientStart    = SurfaceGraphite
val CardGradientEnd      = SurfaceGraphite

// ============================================================================
// VEDA V2.1 PERSONALIZATION ACCENTS & THEME PRESETS
// ============================================================================
val AccentEmerald        = Color(0xFF45D49A)
val AccentAmber          = Color(0xFFF2B84B)
val AccentRose           = Color(0xFFF05C70)

val AccentColors = listOf(
    AccentCyan,     // 0: Cyan
    AccentViolet,   // 1: Violet
    AccentEmerald,  // 2: Emerald
    AccentAmber,    // 3: Amber
    AccentRose      // 4: Rose
)

val AccentColorNames = listOf("Cyan", "Violet", "Emerald", "Amber", "Rose")

fun getAccentColor(index: Int): Color {
    return AccentColors.getOrElse(index) { AccentCyan }
}

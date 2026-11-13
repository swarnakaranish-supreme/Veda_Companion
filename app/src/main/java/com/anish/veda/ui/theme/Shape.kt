package com.anish.veda.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================================
// VEDA V2 SPACING & RADII TOKENS
// ============================================================================
object VedaSpacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object VedaRadii {
    val xs   = 4.dp
    val sm   = 6.dp   // Badges, micro chips
    val md   = 10.dp  // Buttons, text fields, notification cards
    val lg   = 16.dp  // Primary HUD cards, dialog boxes
    val xl   = 22.dp  // Bottom navigation, large metric containers
    val pill = 999.dp // Status pills, interactive toggles
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(VedaRadii.xs),
    small = RoundedCornerShape(VedaRadii.sm),
    medium = RoundedCornerShape(VedaRadii.md),
    large = RoundedCornerShape(VedaRadii.lg),
    extraLarge = RoundedCornerShape(VedaRadii.xl)
)

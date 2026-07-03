package com.eigenlux.roamer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale (DESIGN.md §4): cards 16dp, buttons 12dp, chips 8dp — restrained medium rounding,
 * no pill-shaped cartoonish look. Components still pass `shape=` explicitly (see MainActivity); this only backfills the Material3 default slots.
 */
val RoamerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/** Spacing scale (DESIGN.md §4): 4 · 8 · 12 · 16 · 24 · 32. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

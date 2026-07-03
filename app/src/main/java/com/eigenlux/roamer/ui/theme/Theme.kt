package com.eigenlux.roamer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = HarborPrimaryLight,
    onPrimary = HarborOnPrimaryLight,
    primaryContainer = HarborContainerLight,
    onPrimaryContainer = HarborOnContainerLight,
    secondary = SlateLight,
    onSecondary = SlateOnLight,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = BrassLight,
    onTertiary = BrassOnLight,
    tertiaryContainer = BrassContainerLight,
    onTertiaryContainer = BrassOnContainerLight,
    background = BgLight,
    onBackground = OnBgLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = HarborPrimaryDark,
    onPrimary = HarborOnPrimaryDark,
    primaryContainer = HarborContainerDark,
    onPrimaryContainer = HarborOnContainerDark,
    secondary = SlateDark,
    onSecondary = SlateOnDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SlateOnContainerDark,
    tertiary = BrassDark,
    onTertiary = BrassOnDark,
    tertiaryContainer = BrassContainerDark,
    onTertiaryContainer = BrassOnContainerDark,
    background = BgDark,
    onBackground = OnBgDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * Roamer theme.
 *
 * dynamicColor is **off** by default: this app has a defined brand color (Harbor blue),
 * so it does not adopt Material You's wallpaper-derived colors, ensuring consistent brand
 * recognition across devices. Can be explicitly enabled when needed.
 */
@Composable
fun RoamerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = RoamerShapes,
        content = content,
    )
}

/**
 * Theme-aware "success" green. The Material colorScheme has no success slot, so it lives here separately:
 * expresses the success semantic (e.g. Shizuku granted), distinct from tertiary's brass amber (that's the "overridden / roaming" highlight).
 * Light theme uses dark green for contrast on white surfaces; dark theme uses light green.
 */
val successColor: Color
    @Composable @ReadOnlyComposable
    // Determine dark by the luminance of the actually effective color scheme (not isSystemInDarkTheme), so it stays correct when forcing light/dark.
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) SuccessDark else SuccessLight

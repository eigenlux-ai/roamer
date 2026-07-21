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
 * Main application theme wrapper.
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
 * Theme-aware success state color.
 */
val successColor: Color
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) SuccessDark else SuccessLight

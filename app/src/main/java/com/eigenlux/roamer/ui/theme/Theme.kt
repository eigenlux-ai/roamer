package com.eigenlux.roamer.ui.theme

import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.eigenlux.roamer.ui.PressHighlight

private val LightColorScheme = lightColorScheme(
    primary = BrassLight,
    onPrimary = BrassOnLight,
    primaryContainer = BrassFillLight,
    onPrimaryContainer = BrassOnFill,
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
    surfaceTint = BrassLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = BrassDark,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrassDark,
    onPrimary = BrassOnDark,
    primaryContainer = BrassFillDark,
    onPrimaryContainer = BrassOnFill,
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
    surfaceTint = BrassDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = BrassLight,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * Main application theme wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    ) {
        // Press feedback is a flat highlight or a scale, never the Material ripple.
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
            LocalIndication provides PressHighlight,
            content = content,
        )
    }
}

/**
 * Theme-aware success state color.
 */
val successColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkScheme) SuccessDark else SuccessLight

/**
 * Theme-aware container color for success messages.
 */
val successContainerColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkScheme) SuccessContainerDark else SuccessContainerLight

/**
 * Theme-aware content color on [successContainerColor].
 */
val onSuccessContainerColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkScheme) OnSuccessContainerDark else OnSuccessContainerLight

/**
 * Harbor blue of the ambient ground glow.
 */
val ambientColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkScheme) HarborPrimaryDark else HarborPrimaryLight

/**
 * True when the active color scheme is the dark one.
 */
val isDarkScheme: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

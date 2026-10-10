package com.eigenlux.roamer.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.eigenlux.roamer.ui.theme.ambientColor
import com.eigenlux.roamer.ui.theme.isDarkScheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Colors of the glass material used by the floating control layer.
 *
 * @property tint Fill laid over the blurred backdrop; opaque enough to keep text legible.
 * @property soft Flat fill of buttons, chips, and fields that sit on solid content.
 * @property highlight Specular highlight along the top edge.
 * @property edge Darkened rim that separates the glass from what is behind it.
 */
data class GlassColors(
    val tint: Color,
    val soft: Color,
    val highlight: Color,
    val edge: Color,
)

val glassColors: GlassColors
    @Composable @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        return if (isDarkScheme) {
            GlassColors(
                tint = scheme.surfaceContainerHigh.copy(alpha = 0.62f),
                soft = Color.White.copy(alpha = 0.08f),
                highlight = Color.White.copy(alpha = 0.22f),
                edge = Color.Black.copy(alpha = 0.45f),
            )
        } else {
            GlassColors(
                tint = scheme.surface.copy(alpha = 0.66f),
                soft = scheme.onSurface.copy(alpha = 0.06f),
                highlight = Color.White.copy(alpha = 0.9f),
                edge = Color.Black.copy(alpha = 0.12f),
            )
        }
    }

/**
 * Rim of a glass element: a bright highlight at the top fading into a darkened lower edge.
 */
@Composable
@ReadOnlyComposable
fun glassRim(): Brush {
    val glass = glassColors
    return Brush.verticalGradient(
        0f to glass.highlight,
        0.45f to glass.highlight.copy(alpha = glass.highlight.alpha * 0.2f),
        1f to glass.edge,
    )
}

/**
 * Glass material: the content of [hazeState]'s source is blurred behind this element, tinted,
 * and finished with the rim. Only for controls floating above scrolling content.
 */
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: Shape,
    alpha: Float = 1f,
): Modifier {
    val glass = glassColors
    val style = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(glass.tint)),
        blurRadius = 28.dp,
        noiseFactor = 0.03f,
    )
    return this
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = 24.dp,
                color = Color.Black.copy(alpha = if (isDarkScheme) 0.36f else 0.14f),
                offset = DpOffset(0.dp, 8.dp),
            ),
        )
        .clip(shape)
        .hazeEffect(state = hazeState, style = style) { this.alpha = alpha }
        .border(1.dp, glassRim(), shape)
}

/**
 * Fixed backdrop behind the content: the background color with a soft brand glow at the top,
 * joined by an amber glow while a SIM is overridden ([maskedAmount] from 0 to 1).
 */
@Composable
fun Modifier.roamerGround(maskedAmount: Float = 0f): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkScheme
    val base = scheme.background
    val blue = ambientColor.copy(alpha = if (dark) 0.16f else 0.14f)
    val amber = scheme.tertiary.copy(alpha = (if (dark) 0.16f else 0.18f) * maskedAmount)
    return drawBehind {
        drawRect(base)
        drawRect(
            Brush.radialGradient(
                colors = listOf(blue, Color.Transparent),
                center = Offset(size.width * 0.1f, 0f),
                radius = size.width * 1.05f,
            ),
        )
        if (maskedAmount > 0f) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(amber, Color.Transparent),
                    center = Offset(size.width, size.height * 0.12f),
                    radius = size.width * 0.9f,
                ),
            )
        }
    }
}

/**
 * Background of a toolbar that turns into uniform glass once content scrolls beneath it.
 */
@Composable
fun Modifier.glassToolbar(hazeState: HazeState, amount: Float): Modifier {
    val glass = glassColors
    val style = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(glass.tint)),
        blurRadius = 28.dp,
        noiseFactor = 0.03f,
    )
    val edge = glass.edge
    return this
        .hazeEffect(state = hazeState, style = style) { alpha = amount }
        .drawBehind {
            drawLine(
                color = edge.copy(alpha = edge.alpha * amount),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
}

package com.eigenlux.roamer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.eigenlux.roamer.ui.theme.Spacing
import com.eigenlux.roamer.ui.theme.isDarkScheme
import com.eigenlux.roamer.ui.theme.successColor
import kotlinx.coroutines.launch

/** Visual height shared by buttons and the segmented control. */
private val ButtonHeight = 44.dp

/** Rounded rectangle shared by buttons, chips, and fields. */
val ControlShape = RoundedCornerShape(12.dp)

private val SegmentTrackShape = RoundedCornerShape(10.dp)
private val SegmentThumbShape = RoundedCornerShape(8.dp)

private const val DisabledAlpha = 0.38f

/**
 * Soft, diffuse shadow for thumbs: low opacity, wide blur, small downward offset. Elevation
 * shadows are not used; their hard directional edge reads as Material.
 */
fun Modifier.softShadow(shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = 6.dp,
        color = Color.Black.copy(alpha = 0.16f),
        offset = DpOffset(0.dp, 2.dp),
    ),
)

/**
 * Press feedback for rows: a flat highlight while pressed, in place of the Material ripple.
 */
object PressHighlight : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode = HighlightNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()

    private class HighlightNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
        private var pressCount = 0

        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect { interaction ->
                    val before = pressCount
                    when (interaction) {
                        is PressInteraction.Press -> pressCount++
                        is PressInteraction.Release, is PressInteraction.Cancel -> pressCount--
                    }
                    if ((before > 0) != (pressCount > 0)) invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (pressCount > 0) drawRect(Color.Gray.copy(alpha = 0.18f))
        }
    }
}

/**
 * Springy scale-down and slight dim while the element is pressed.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (pressed) 0.82f else 1f
    }
}

/**
 * Flat rounded button with a soft fill for secondary actions. [container] and [contentColor]
 * override the neutral look for a selected state.
 */
@Composable
fun SoftButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = glassColors.soft,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg),
    content: @Composable RowScope.() -> Unit,
) {
    RoundedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = container,
        contentColor = contentColor,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Filled rounded button for the main action of a group.
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    RoundedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        container = if (enabled) scheme.primaryContainer else glassColors.soft,
        contentColor = if (enabled) scheme.onPrimaryContainer else scheme.onSurface,
        contentPadding = PaddingValues(horizontal = Spacing.xl),
        content = content,
    )
}

@Composable
private fun RoundedButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(container, tween(200), label = "buttonContainer")
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .pressScale(interaction)
            .clip(ControlShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = ButtonHeight)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) contentColor else contentColor.copy(alpha = DisabledAlpha),
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

/**
 * Toolbar action: a bare icon in the accent color, with no container.
 */
@Composable
fun ToolbarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(48.dp)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) color else color.copy(alpha = DisabledAlpha),
            content = content,
        )
    }
}

/**
 * Borderless text button in the accent color.
 */
@Composable
fun PlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        )
    }
}

/**
 * Compact toggle with a capsule track and a round thumb.
 */
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackWidth = 46.dp
    val trackHeight = 28.dp
    val inset = 2.dp
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The round thumb stretches slightly while held.
    val thumbWidth by animateDpAsState(
        targetValue = if (pressed) 28.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "thumbWidth",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbWidth - inset else inset,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "thumbOffset",
    )
    val offTrack = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkScheme) 0.22f else 0.16f)
    val track by animateColorAsState(if (checked) successColor else offTrack, tween(200), label = "track")
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .size(trackWidth, trackHeight)
            .clip(CircleShape)
            .background(track),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbWidth, trackHeight - inset * 2)
                .softShadow(CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * Segmented control: a rounded track with a thumb that slides to the selected option.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(SegmentTrackShape)
            .background(glassColors.soft)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            label = "segmentThumb",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .softShadow(SegmentThumbShape)
                .background(scheme.surfaceBright, SegmentThumbShape),
        )
        Row(modifier = Modifier.fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Round selection mark for list rows: a filled check when [checked], an empty ring otherwise.
 */
@Composable
fun CheckCircle(checked: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    if (checked) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = scheme.primary,
            modifier = modifier.size(26.dp),
        )
    } else {
        Box(
            modifier = modifier
                .size(26.dp)
                .padding(2.dp)
                .border(1.5.dp, scheme.outline.copy(alpha = 0.7f), CircleShape),
        )
    }
}

/**
 * Menu entry with a check mark in front of the current choice.
 */
@Composable
fun MenuItemRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

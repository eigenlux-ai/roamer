package com.eigenlux.roamer.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eigenlux.roamer.data.CountryPresets
import com.eigenlux.roamer.ui.theme.Spacing
import com.eigenlux.roamer.ui.theme.destructiveColor
import com.eigenlux.roamer.ui.theme.successColor
import java.util.Locale
import kotlinx.coroutines.delay

private val GroupRadius = 20.dp

/** Place of a row inside its group; decides which corners are rounded and whether it has a divider. */
enum class RowPosition {
    SINGLE, FIRST, MIDDLE, LAST;

    companion object {
        fun of(index: Int, count: Int): RowPosition = when {
            count <= 1 -> SINGLE
            index == 0 -> FIRST
            index == count - 1 -> LAST
            else -> MIDDLE
        }
    }
}

private fun RowPosition.shape(): Shape = when (this) {
    RowPosition.SINGLE -> RoundedCornerShape(GroupRadius)
    RowPosition.FIRST -> RoundedCornerShape(topStart = GroupRadius, topEnd = GroupRadius)
    RowPosition.LAST -> RoundedCornerShape(bottomStart = GroupRadius, bottomEnd = GroupRadius)
    RowPosition.MIDDLE -> RoundedCornerShape(0.dp)
}

/**
 * One row of an inset grouped list: a solid surface whose corners follow its [position], with a
 * hairline under every row but the last.
 */
@Composable
fun GroupRow(
    position: RowPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val divider = scheme.outlineVariant.copy(alpha = 0.6f)
    val hasDivider = position == RowPosition.FIRST || position == RowPosition.MIDDLE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(position.shape())
            .background(scheme.surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .drawBehind {
                if (hasDivider) {
                    val inset = Spacing.lg.toPx()
                    drawLine(divider, Offset(inset, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
            }
            .heightIn(min = 52.dp)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        content = content,
    )
}

/**
 * Drag-to-reorder state for the rows of a lazy list. The dragged row follows the finger, and
 * whenever it passes the middle of a neighbor the two are swapped through [onMove].
 */
class ReorderState(val listState: LazyListState) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set

    internal var indexOf: (Any) -> Int = { -1 }
    internal var count: () -> Int = { 0 }
    internal var onMove: (Int, Int) -> Unit = { _, _ -> }
    private var rowHeight = 0f

    fun start(key: Any, heightPx: Float) {
        draggingKey = key
        rowHeight = heightPx
        offset = 0f
    }

    fun dragBy(delta: Float) {
        val key = draggingKey ?: return
        offset += delta
        if (rowHeight <= 0f) return
        val index = indexOf(key)
        if (index < 0) return
        if (offset > rowHeight / 2 && index < count() - 1) {
            onMove(index, index + 1)
            offset -= rowHeight
        } else if (offset < -rowHeight / 2 && index > 0) {
            onMove(index, index - 1)
            offset += rowHeight
        }
    }

    fun end() {
        draggingKey = null
        offset = 0f
    }
}

/**
 * Creates the [ReorderState] of a list and keeps the list scrolling while a row is held near
 * the top or bottom edge.
 */
@Composable
fun <T> rememberReorderState(
    listState: LazyListState,
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
): ReorderState {
    val state = remember(listState) { ReorderState(listState) }
    SideEffect {
        state.indexOf = { k -> items.indexOfFirst { key(it) == k } }
        state.count = { items.size }
        state.onMove = onMove
    }
    LaunchedEffect(state.draggingKey) {
        val dragged = state.draggingKey ?: return@LaunchedEffect
        while (true) {
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.key == dragged }
            if (item != null) {
                val top = item.offset + state.offset
                val bottom = top + item.size
                val end = layout.viewportEndOffset - layout.afterContentPadding
                val step = when {
                    top < item.size * 0.6f -> -14f
                    bottom > end - item.size * 0.6f -> 14f
                    else -> 0f
                }
                if (step != 0f) {
                    // The row's slot moves with the list; the finger does not, so compensate.
                    val scrolled = listState.scrollBy(step)
                    if (scrolled != 0f) state.dragBy(scrolled)
                }
            }
            delay(16)
        }
    }
    return state
}

/**
 * Row of an editable list. In [editing] mode it shows a delete button in front (when [onDelete]
 * is set), then [content], [editTrailing], and a drag handle; otherwise [trailing] is shown at
 * the end.
 */
@Composable
fun LazyItemScope.EditableRow(
    rowKey: Any,
    position: RowPosition,
    editing: Boolean,
    reorder: ReorderState?,
    onDelete: (() -> Unit)?,
    deleteLabel: String,
    reorderLabel: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    editTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val dragging = reorder?.draggingKey == rowKey
    var height by remember { mutableFloatStateOf(0f) }
    GroupRow(
        position = position,
        onClick = if (editing) null else onClick,
        modifier = Modifier
            .onSizeChanged { height = it.height.toFloat() }
            .zIndex(if (dragging) 1f else 0f)
            .then(if (dragging) Modifier else Modifier.animateItem())
            .graphicsLayer {
                translationY = if (dragging) reorder!!.offset else 0f
                shadowElevation = if (dragging) 12.dp.toPx() else 0f
            },
    ) {
        EditControl(visible = editing) {
            if (onDelete != null) {
                CircleGlyphButton(
                    color = destructiveColor,
                    plus = false,
                    label = deleteLabel,
                    onClick = onDelete,
                )
            } else {
                // A row that cannot be removed keeps the space, so all names stay aligned.
                Box(modifier = Modifier.size(40.dp))
            }
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            content = content,
        )
        if (!editing) trailing()
        if (editing && editTrailing != null) editTrailing()
        EditControl(visible = editing && reorder != null) {
            DragHandle(
                label = reorderLabel,
                modifier = Modifier.pointerInput(rowKey) {
                    detectDragGestures(
                        onDragStart = { reorder?.start(rowKey, height) },
                        onDragEnd = { reorder?.end() },
                        onDragCancel = { reorder?.end() },
                    ) { change, drag ->
                        change.consume()
                        reorder?.dragBy(drag.y)
                    }
                },
            )
        }
    }
}

@Composable
private fun EditControl(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(tween(200)) + fadeIn(tween(200)),
        exit = shrinkHorizontally(tween(200)) + fadeOut(tween(120)),
    ) { content() }
}

/**
 * Round button with a plus (green by default) or minus (red) glyph, for adding and removing rows.
 */
@Composable
fun CircleGlyphButton(
    plus: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = successColor,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
                .background(color, CircleShape)
                .drawBehind {
                    val arm = size.width * 0.27f
                    val stroke = 2.dp.toPx()
                    drawLine(
                        Color.White,
                        Offset(center.x - arm, center.y),
                        Offset(center.x + arm, center.y),
                        stroke,
                        StrokeCap.Round,
                    )
                    if (plus) {
                        drawLine(
                            Color.White,
                            Offset(center.x, center.y - arm),
                            Offset(center.x, center.y + arm),
                            stroke,
                            StrokeCap.Round,
                        )
                    }
                },
        )
    }
}

@Composable
private fun DragHandle(label: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Box(
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = label }
            .drawBehind {
                val half = 9.dp.toPx()
                val gap = 5.dp.toPx()
                for (i in -1..1) {
                    val y = center.y + i * gap
                    drawLine(color, Offset(center.x - half, y), Offset(center.x + half, y), 1.6.dp.toPx(), StrokeCap.Round)
                }
            },
    )
}

/**
 * Names of the regions the user defined, by code. Kept as snapshot state so every name on
 * screen follows an edit.
 */
object CustomRegionNames {
    var names by mutableStateOf<Map<String, String>>(emptyMap())
}

/**
 * Display name of a country or region in the app's language. A region the user defined uses
 * the name they gave it, presets use their curated name, and anything else falls back to the
 * platform's name for that region code.
 */
fun regionName(context: Context, iso: String): String {
    CustomRegionNames.names[iso]?.let { return it }
    CountryPresets.byIso(iso)?.let { return context.getString(it.nameRes) }
    val region = iso.uppercase(Locale.ROOT)
    val name = runCatching {
        Locale.Builder().setRegion(region).build().getDisplayCountry(context.resources.configuration.locales[0])
    }.getOrDefault("")
    return name.ifBlank { region }
}

@Composable
fun regionName(iso: String): String {
    // Read so the name is recomputed when the app language changes.
    LocalConfiguration.current
    return regionName(LocalContext.current, iso)
}

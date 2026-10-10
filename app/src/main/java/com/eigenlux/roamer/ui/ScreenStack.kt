package com.eigenlux.roamer.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow

/** How far the page underneath is shifted left while it is fully covered. */
private const val ParallaxFraction = 0.28f

/** Opacity of the dimming layer over the page underneath while it is fully covered. */
private const val ScrimAlpha = 0.22f

/**
 * Page stack. [base] is always at the bottom; each entry of [stack] slides in over the previous
 * one from the right. The system Back gesture drags the top page along with the finger and
 * reveals the page beneath, and letting go either completes the return or settles the page back.
 * Pages below the top stay composed, so they keep their scroll position and state.
 */
@Composable
fun <T : Any> ScreenStack(
    stack: List<T>,
    onBack: () -> Unit,
    base: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    val target = stack.toList()
    // The pages being drawn; a popped page stays here while it slides out.
    var shown by remember { mutableStateOf(target) }
    // Position of the top page: 0 = it covers the screen, 1 = it is fully off to the right.
    val offset = remember { Animatable(0f) }

    LaunchedEffect(target) {
        when {
            target.size > shown.size -> {
                offset.snapTo(1f)
                shown = target
                offset.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 320f))
            }
            target.size < shown.size -> {
                offset.animateTo(1f, tween(260))
                shown = target
                offset.snapTo(0f)
            }
            else -> shown = target
        }
    }

    PredictiveBackHandler(enabled = stack.isNotEmpty()) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { event -> offset.snapTo(event.progress) }
            onBack()
        } catch (e: CancellationException) {
            offset.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 400f))
            throw e
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val top = shown.lastIndex
        // index -1 is the base page
        @Composable
        fun Layer(index: Int, page: @Composable () -> Unit) {
            val isTop = index == top
            val isBeneath = index == top - 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = when {
                            isTop && index >= 0 -> size.width * offset.value
                            isBeneath -> -size.width * ParallaxFraction * (1f - offset.value)
                            else -> 0f
                        }
                    }
                    .drawWithContent {
                        // Pages deeper than the one beneath the top are covered; skip drawing them.
                        if (!isTop && !isBeneath) return@drawWithContent
                        drawContent()
                        if (isBeneath) drawRect(Color.Black.copy(alpha = ScrimAlpha * (1f - offset.value)))
                    }
                    .then(if (isTop) Modifier else Modifier.clearAndSetSemantics { })
                    .then(
                        if (index >= 0) {
                            Modifier
                                .background(MaterialTheme.colorScheme.background)
                                // Keeps touches from reaching the page underneath.
                                .pointerInput(Unit) { detectTapGestures { } }
                        } else {
                            Modifier
                        },
                    ),
            ) { page() }
        }

        Layer(-1, base)
        shown.forEachIndexed { index, screen ->
            key(screen) { Layer(index) { content(screen) } }
        }
    }
}

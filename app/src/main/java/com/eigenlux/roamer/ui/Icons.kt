package com.eigenlux.roamer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Back chevron drawn as a thin stroked line with round ends. The Material arrow is a filled
 * shape whose weight cannot be changed, and it reads too heavy at toolbar size.
 */
val ChevronBack: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChevronBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(14.5f, 5f)
            lineTo(7.5f, 12f)
            lineTo(14.5f, 19f)
        }
    }.build()
}

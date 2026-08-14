package com.zatiki.memocards.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.ui.theme.MemoPalette

/** Hueco inferior para que las listas no queden bajo la barra translúcida. */
val BottomBarContentGap = 96.dp

fun Modifier.memoGlass(
    palette: MemoPalette,
    shape: Shape,
    alpha: Float = 0.72f,
    elevation: Dp = 8.dp,
): Modifier {
    val top = (alpha + 0.12f).coerceAtMost(0.90f)
    val bottom = (alpha - 0.10f).coerceAtLeast(0.42f)
    return this
        .shadow(elevation, shape, clip = false)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    palette.card.copy(alpha = top),
                    palette.card.copy(alpha = bottom),
                ),
            ),
            shape,
        )
}

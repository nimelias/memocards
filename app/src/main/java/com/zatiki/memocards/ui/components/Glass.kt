package com.zatiki.memocards.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.ui.theme.MemoPalette

/** Hueco inferior para que las listas no queden bajo la barra translúcida. */
val BottomBarContentGap = 96.dp

/**
 * Cristal translúcido sin [Modifier.shadow].
 *
 * La elevation de Compose pinta una sombra opaca detrás; con fill semitransparente
 * esa sombra se ve a través y produce bandas/halos (artefactos). Aquí solo hay
 * un velo suave dibujado detrás y el fondo recortado a la forma.
 */
fun Modifier.memoGlass(
    palette: MemoPalette,
    shape: Shape,
    alpha: Float = 0.72f,
    elevation: Dp = 8.dp,
): Modifier {
    val top = (alpha + 0.10f).coerceAtMost(0.92f)
    val bottom = (alpha - 0.06f).coerceAtLeast(0.52f)
    val depth = elevation
    return this
        .drawBehind {
            if (depth <= 0.dp) return@drawBehind
            val blur = depth.toPx().coerceIn(4f, 28f)
            val veil = 0.045f + (blur / 900f)
            // Velo inferior suave (sin rectángulo fantasma de shadow).
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = veil),
                    ),
                    startY = size.height - blur,
                    endY = size.height + blur * 0.4f,
                ),
                topLeft = Offset(blur * 0.15f, size.height - blur * 0.35f),
                size = Size(size.width - blur * 0.3f, blur),
            )
        }
        .clip(shape)
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

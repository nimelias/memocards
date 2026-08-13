package com.zatiki.memocards.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.ui.theme.LocalMemoPalette

/**
 * Brillos degradados tipo Emich: orbes suaves detrás del contenido.
 * [intensity] 0 = sin glow, 1 = intensidad por defecto del tema, 2 = máximo.
 */
@Composable
fun AmbientGlowBackdrop(
    theme: ThemeName,
    intensity: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalMemoPalette.current
    val strong = theme == ThemeName.EMICH || theme == ThemeName.DARK
    val factor = intensity.coerceIn(0f, 2.0f)
    val a1 = (if (strong) 0.38f else 0.10f) * factor
    val a2 = (if (strong) 0.28f else 0.07f) * factor
    val a3 = (if (strong) 0.24f else 0.05f) * factor
    val c1 = if (theme == ThemeName.EMICH) Color(0xFF6B5CFF) else palette.primary
    val c2 = if (theme == ThemeName.EMICH) Color(0xFF4A7AFF) else palette.primary
    val c3 = if (theme == ThemeName.EMICH) Color(0xFFB24AFF) else palette.primary

    Box(
        modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (factor > 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c1.copy(alpha = a1), Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.08f),
                        radius = w * 0.55f,
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.88f, h * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c2.copy(alpha = a2), Color.Transparent),
                        center = Offset(w * 0.05f, h * 0.45f),
                        radius = w * 0.65f,
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.05f, h * 0.45f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c3.copy(alpha = a3), Color.Transparent),
                        center = Offset(w * 0.75f, h * 0.92f),
                        radius = w * 0.5f,
                    ),
                    radius = w * 0.5f,
                    center = Offset(w * 0.75f, h * 0.92f),
                )
            }
        }
        content()
    }
}

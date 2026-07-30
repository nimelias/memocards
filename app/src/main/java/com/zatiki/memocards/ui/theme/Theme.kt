package com.zatiki.memocards.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings

data class MemoPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val primary: Color,
)

val LocalMemoPalette = staticCompositionLocalOf {
    MemoPalette(
        background = Color(0xFFF8FAFC),
        card = Color.White,
        text = Color(0xFF0F172A),
        muted = Color(0xFF64748B),
        border = Color(0xFFE2E8F0),
        primary = Color(0xFF2563EB),
    )
}

val LocalFontScale = staticCompositionLocalOf { 1f }

fun paletteFor(theme: ThemeName): MemoPalette = when (theme) {
    ThemeName.LIGHT -> MemoPalette(
        background = Color(0xFFF8FAFC),
        card = Color.White,
        text = Color(0xFF0F172A),
        muted = Color(0xFF64748B),
        border = Color(0xFFE2E8F0),
        primary = Color(0xFF2563EB),
    )
    ThemeName.DARK -> MemoPalette(
        background = Color(0xFF0B1220),
        card = Color(0xFF0F172A),
        text = Color(0xFFE2E8F0),
        muted = Color(0xFF94A3B8),
        border = Color(0xFF1E293B),
        primary = Color(0xFF60A5FA),
    )
    ThemeName.SAND -> MemoPalette(
        background = Color(0xFFF7F2E8),
        card = Color(0xFFFFFAF1),
        text = Color(0xFF3A2F20),
        muted = Color(0xFF7C6A50),
        border = Color(0xFFE9DCC5),
        primary = Color(0xFFB7791F),
    )
}

@Composable
fun MemoCardsTheme(
    settings: UiSettings,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(settings.theme)
    val scheme = if (settings.theme == ThemeName.DARK) {
        darkColorScheme(
            primary = palette.primary,
            background = palette.background,
            surface = palette.card,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            background = palette.background,
            surface = palette.card,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    }

    CompositionLocalProvider(
        LocalMemoPalette provides palette,
        LocalFontScale provides settings.fontScale,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}

@Composable
fun scaledSp(base: Float): TextUnit = (base * LocalFontScale.current).sp

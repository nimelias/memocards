package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.domain.ArcLabelMode
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp

@Composable
fun SettingsScreen(
    settings: UiSettings,
    onBack: () -> Unit,
    onThemeChange: (ThemeName) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onRatingLayoutChange: (RatingLayout) -> Unit,
    onArcLabelModeChange: (ArcLabelMode) -> Unit,
) {
    val palette = LocalMemoPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = palette.text)
            }
            Text(
                "Ajustes",
                fontSize = scaledSp(22f),
                fontWeight = FontWeight.Bold,
                color = palette.text,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Tema", fontWeight = FontWeight.SemiBold, color = palette.text, fontSize = scaledSp(15f))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeName.entries.forEach { theme ->
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { onThemeChange(theme) },
                    label = {
                        Text(
                            when (theme) {
                                ThemeName.LIGHT -> "Claro"
                                ThemeName.DARK -> "Oscuro"
                                ThemeName.SAND -> "Arena"
                            },
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Escala de fuente (${"%.2f".format(settings.fontScale)})",
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            fontSize = scaledSp(15f),
        )
        Slider(
            value = settings.fontScale,
            onValueChange = onFontScaleChange,
            valueRange = 0.9f..1.4f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Botones de dificultad",
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            fontSize = scaledSp(15f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tras revelar la respuesta, toca un lateral para el menú de arco.",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = settings.ratingLayout == RatingLayout.BAR,
                onClick = { onRatingLayoutChange(RatingLayout.BAR) },
                label = { Text("Barra de botones") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = settings.ratingLayout.isArc,
                onClick = { onRatingLayoutChange(RatingLayout.ARC_RIGHT) },
                label = { Text("Menú de arco") },
                modifier = Modifier.weight(1f),
            )
        }

        if (settings.ratingLayout.isArc) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Contenido del arco",
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                fontSize = scaledSp(15f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Mostrar solo iconos o solo texto en cada sector, no ambos.",
                color = palette.muted,
                fontSize = scaledSp(12f),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArcLabelMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.arcLabelMode == mode,
                        onClick = { onArcLabelModeChange(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ArcLabelMode.ICONS -> "Solo iconos"
                                    ArcLabelMode.TEXT -> "Solo texto"
                                },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "MemoCards nativo · Kotlin + Jetpack Compose + Room",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
    }
}

package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.ArcLabelMode
import com.zatiki.memocards.domain.EstudiaProject
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.SyncSettings
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repo: MemoRepository,
    settings: UiSettings,
    onBack: () -> Unit,
    onThemeChange: (ThemeName) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onGlowIntensityChange: (Float) -> Unit = {},
    onRatingLayoutChange: (RatingLayout) -> Unit,
    onArcLabelModeChange: (ArcLabelMode) -> Unit,
    showBack: Boolean = true,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var syncSettings by remember { mutableStateOf(SyncSettings()) }
    var projects by remember { mutableStateOf<List<EstudiaProject>>(emptyList()) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        syncSettings = repo.getSyncSettings()
    }

    fun persistSync(next: SyncSettings) {
        syncSettings = next
        scope.launch { repo.saveSyncSettings(next) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = palette.text)
                }
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(ThemeName.LIGHT, ThemeName.DARK).forEach { theme ->
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { onThemeChange(theme) },
                    label = {
                        Text(
                            when (theme) {
                                ThemeName.LIGHT -> "Claro"
                                ThemeName.DARK -> "Oscuro"
                                else -> theme.value
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(ThemeName.SAND, ThemeName.EMICH).forEach { theme ->
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { onThemeChange(theme) },
                    label = {
                        Text(
                            when (theme) {
                                ThemeName.SAND -> "Arena"
                                ThemeName.EMICH -> "Emich"
                                else -> theme.value
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
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
            "Interlineado lectura (${"%.2f".format(settings.lineHeight)})",
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            fontSize = scaledSp(15f),
        )
        Text(
            "Afecta al modo libro.",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
        Slider(
            value = settings.lineHeight,
            onValueChange = onLineHeightChange,
            valueRange = 1.15f..2.0f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Intensidad de degradados (${"%.0f".format(settings.glowIntensity * 100)}%)",
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            fontSize = scaledSp(15f),
        )
        Text(
            "Brillos de fondo en inicio, mazo y estudio.",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
        Slider(
            value = settings.glowIntensity,
            onValueChange = onGlowIntensityChange,
            valueRange = 0f..2.0f,
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

        Spacer(Modifier.height(28.dp))
        Text(
            "estudIA",
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            fontSize = scaledSp(15f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Sincroniza barajas y envía estadísticas de repaso a estudIA (X-KEY, sin login).",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = syncSettings.baseUrl,
            onValueChange = { persistSync(syncSettings.copy(baseUrl = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("URL del servidor") },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = syncSettings.apiKey,
            onValueChange = { persistSync(syncSettings.copy(apiKey = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Clave X-KEY") },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        testing = true
                        syncMessage = repo.testEstudiaConnectionMessage(syncSettings)
                        testing = false
                    }
                },
                enabled = !testing,
                modifier = Modifier.weight(1f),
            ) { Text(if (testing) "Probando…" else "Probar conexión") }
            Button(
                onClick = {
                    scope.launch {
                        projects = repo.listEstudiaProjects(syncSettings)
                        syncMessage = if (projects.isEmpty()) "Sin proyectos" else "${projects.size} proyectos"
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Cargar proyectos") }
        }
        if (projects.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Asignatura", fontWeight = FontWeight.Medium, color = palette.text, fontSize = scaledSp(13f))
            Spacer(Modifier.height(6.dp))
            projects.forEach { project ->
                FilterChip(
                    selected = syncSettings.projectId == project.id,
                    onClick = { persistSync(syncSettings.copy(projectId = project.id)) },
                    label = { Text(project.name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sincronización automática", fontWeight = FontWeight.Medium, color = palette.text)
                Text("Actualiza barajas importadas periódicamente", color = palette.muted, fontSize = scaledSp(12f))
            }
            Switch(
                checked = syncSettings.autoSyncEnabled,
                onCheckedChange = { persistSync(syncSettings.copy(autoSyncEnabled = it)) },
            )
        }
        if (syncSettings.autoSyncEnabled) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Intervalo: ${syncSettings.autoSyncIntervalMinutes} min",
                color = palette.text,
                fontSize = scaledSp(13f),
            )
            Slider(
                value = syncSettings.autoSyncIntervalMinutes.toFloat(),
                onValueChange = {
                    persistSync(syncSettings.copy(autoSyncIntervalMinutes = it.toInt().coerceIn(5, 240)))
                },
                valueRange = 5f..240f,
                steps = 46,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        syncMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = palette.muted, fontSize = scaledSp(12f))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "MemoCards nativo · Kotlin + FSRS-6 + Jetpack Compose + Room",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
    }
}

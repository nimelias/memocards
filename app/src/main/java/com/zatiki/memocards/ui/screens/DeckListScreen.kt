package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSummary
import com.zatiki.memocards.domain.EstudiaDeckSummary
import com.zatiki.memocards.domain.HomeStats
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun DeckListScreen(
    repo: MemoRepository,
    settings: UiSettings,
    onToggleTheme: () -> Unit,
    onOpenDeck: (Deck) -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var decks by remember { mutableStateOf<List<DeckSummary>>(emptyList()) }
    var homeStats by remember { mutableStateOf(HomeStats(0, 0)) }
    var showImport by remember { mutableStateOf(false) }
    var remoteDecks by remember { mutableStateOf<List<EstudiaDeckSummary>>(emptyList()) }
    var importLoading by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        decks = repo.listDeckSummaries()
        homeStats = repo.getHomeStats()
    }

    LaunchedEffect(Unit) {
        repo.ensureDemoDeckIfNeeded()
        reload()
        repo.syncEstudiaIfDue()
        reload()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        PerspectiveGrid(
            color = palette.primary.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .shadow(2.dp, RoundedCornerShape(50))
                        .background(palette.primary, RoundedCornerShape(50))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        "MEMOCARDS",
                        color = palette.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(14f),
                        letterSpacing = 1.2.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (settings.theme == ThemeName.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Tema",
                        tint = palette.primary,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val statsShape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, statsShape)
                    .background(palette.surface, statsShape)
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatBlock(
                    value = homeStats.cardsDone.toString(),
                    label = "Cards Done",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(palette.border),
                )
                StatBlock(
                    value = homeStats.leftToAnswer.toString(),
                    label = "Left to Answer",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier
                    .background(palette.surface, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    "DECKS",
                    color = palette.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = scaledSp(12f),
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        importLoading = true
                        importMessage = null
                        val sync = repo.getSyncSettings()
                        remoteDecks = repo.listEstudiaDecks(sync)
                        importLoading = false
                        if (remoteDecks.isEmpty()) {
                            importMessage = "Configura estudIA en Ajustes o no hay barajas"
                        } else {
                            showImport = true
                        }
                    }
                },
                enabled = !importLoading,
                contentPadding = PaddingValues(0.dp),
            ) {
                if (importLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                        color = palette.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Importar estudIA", color = palette.primary, fontSize = scaledSp(13f))
            }
            importMessage?.let {
                Text(it, color = palette.muted, fontSize = scaledSp(12f))
            }

            Spacer(Modifier.height(8.dp))

            if (decks.isEmpty()) {
                Text(
                    "Sin mazos todavía. Usa + para crear el primero.",
                    color = palette.muted,
                    fontSize = scaledSp(15f),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(decks, key = { it.deck.id }) { summary ->
                        val shape = RoundedCornerShape(14.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.surface, shape)
                                .clickable { onOpenDeck(summary.deck) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                summary.deck.name,
                                color = palette.text,
                                fontSize = scaledSp(16f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                summary.cardCount.toString(),
                                color = palette.primary,
                                fontSize = scaledSp(16f),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Barajas en estudIA") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    remoteDecks.forEach { remote ->
                        Button(
                            onClick = {
                                scope.launch {
                                    importLoading = true
                                    val deckId = repo.importEstudiaDeck(remote.id)
                                    importLoading = false
                                    showImport = false
                                    reload()
                                    decks.find { it.deck.id == deckId }?.let { onOpenDeck(it.deck) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${remote.title} (${remote.cardCount} cartas)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImport = false }) { Text("Cerrar") }
            },
        )
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = palette.primary,
            fontSize = scaledSp(32f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            color = palette.muted,
            fontSize = scaledSp(13f),
        )
    }
}

@Composable
private fun PerspectiveGrid(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cols = 12
        val rows = 8
        val horizonY = size.height * 0.05f
        val bottomY = size.height
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        for (i in 0..cols) {
            val t = i / cols.toFloat()
            val topX = size.width * (0.35f + t * 0.3f)
            val bottomX = size.width * t
            drawLine(
                color = color,
                start = Offset(topX, horizonY),
                end = Offset(bottomX, bottomY),
                strokeWidth = 1.5f,
                pathEffect = pathEffect,
            )
        }
        for (r in 1..rows) {
            val p = (r / rows.toFloat()).let { it * it }
            val y = horizonY + (bottomY - horizonY) * p
            val inset = (1f - p) * size.width * 0.28f
            drawLine(
                color = color,
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = 1.2f,
                pathEffect = pathEffect,
            )
        }
    }
}

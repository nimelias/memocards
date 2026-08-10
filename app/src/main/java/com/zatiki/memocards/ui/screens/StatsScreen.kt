package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.ActivityStats
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun StatsScreen(
    repo: MemoRepository,
) {
    val palette = LocalMemoPalette.current
    var stats by remember {
        mutableStateOf(
            ActivityStats(0, 0L, 0, 0, 0, emptyList()),
        )
    }

    LaunchedEffect(Unit) {
        stats = repo.getActivityStats()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Estadísticas",
            fontSize = scaledSp(26f),
            fontWeight = FontWeight.Bold,
            color = palette.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Actividad local de estudio",
            color = palette.muted,
            fontSize = scaledSp(14f),
        )

        Spacer(Modifier.height(18.dp))

        val cardShape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .background(palette.surface, cardShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Hoy: ${stats.cardsStudiedToday} cartas · ${formatDuration(stats.elapsedMsToday)}",
                color = palette.text,
                fontSize = scaledSp(15f),
                fontWeight = FontWeight.Medium,
            )

            QueueRow("Nuevas", stats.newCount)
            QueueRow("Aprendizaje", stats.learningCount)
            QueueRow("Repaso", stats.reviewCount)

            Spacer(Modifier.height(4.dp))
            Text(
                heatmapTitle(stats),
                color = palette.muted,
                fontSize = scaledSp(13f),
            )
            HeatmapGrid(counts = stats.heatmap.map { it.reviewCount })
        }
    }
}

@Composable
private fun QueueRow(label: String, count: Int) {
    val palette = LocalMemoPalette.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = palette.text, fontSize = scaledSp(15f))
        Text(
            count.toString(),
            color = palette.primary,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(16f),
        )
    }
}

@Composable
private fun HeatmapGrid(counts: List<Int>) {
    val palette = LocalMemoPalette.current
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val columns = 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        counts.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { count ->
                    val intensity = count.toFloat() / max
                    val color = when {
                        count == 0 -> palette.border
                        intensity < 0.34f -> palette.primary.copy(alpha = 0.35f)
                        intensity < 0.67f -> palette.primary.copy(alpha = 0.65f)
                        else -> palette.primary
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMin = TimeUnit.MILLISECONDS.toMinutes(ms)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun heatmapTitle(stats: ActivityStats): String {
    val cal = Calendar.getInstance()
    if (stats.heatmap.isNotEmpty()) {
        cal.timeInMillis = stats.heatmap.last().dayStart
    }
    val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
    val year = cal.get(Calendar.YEAR)
    return "Actividad · $month / $year"
}

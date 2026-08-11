package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.ActivityStats
import com.zatiki.memocards.domain.HourActivity
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@Composable
fun StatsScreen(
    repo: MemoRepository,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var stats by remember {
        mutableStateOf(
            ActivityStats(0, 0L, 0, 0, 0, emptyList()),
        )
    }

    suspend fun reload() {
        stats = repo.getActivityStats()
    }

    LaunchedEffect(Unit) { reload() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                "Hoy por hora",
                color = palette.muted,
                fontSize = scaledSp(13f),
            )
            HourlyChart(hours = stats.hourlyToday)

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
private fun HourlyChart(hours: List<HourActivity>) {
    val palette = LocalMemoPalette.current
    val max = (hours.maxOfOrNull { it.reviewCount } ?: 0).coerceAtLeast(1)
    val active = hours.filter { it.reviewCount > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            hours.forEach { hour ->
                val ratio = hour.reviewCount.toFloat() / max
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(ratio.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (hour.reviewCount == 0) palette.border
                            else palette.primary.copy(alpha = 0.35f + ratio * 0.65f),
                        ),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0h", color = palette.muted, fontSize = scaledSp(11f))
            Text("12h", color = palette.muted, fontSize = scaledSp(11f))
            Text("23h", color = palette.muted, fontSize = scaledSp(11f))
        }
        if (active.isNotEmpty()) {
            Text(
                "Pico: ${active.maxBy { it.reviewCount }.hour}:00 · " +
                    active.maxBy { it.reviewCount }.reviewCount + " cartas",
                color = palette.text,
                fontSize = scaledSp(12f),
            )
        }
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

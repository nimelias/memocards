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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.ActivityStats
import com.zatiki.memocards.domain.DayActivity
import com.zatiki.memocards.domain.HourActivity
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlin.math.roundToInt
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

private val RATING_AGAIN = Color(0xFFDC2626)
private val RATING_HARD = Color(0xFFEA580C)
private val RATING_GOOD = Color(0xFF16A34A)
private val RATING_EASY = Color(0xFF2563EB)

@Composable
fun StatsScreen(
    repo: MemoRepository,
    deckId: Long? = null,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var stats by remember {
        mutableStateOf(
            ActivityStats(0, 0L, 0, 0, 0, emptyList()),
        )
    }
    var deckName by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        stats = repo.getActivityStats(deckId = deckId)
        deckName = deckId?.let { repo.getDeck(it)?.name }
    }

    LaunchedEffect(deckId) { reload() }
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Estadísticas",
            fontSize = scaledSp(24f),
            fontWeight = FontWeight.Bold,
            color = palette.text,
        )
        Text(
            if (deckId != null) "Tipo: Mazo · ${deckName ?: "Mazo #$deckId"}" else "Tipo: Totales globales",
            color = palette.muted,
            fontSize = scaledSp(12f),
        )
        Spacer(Modifier.height(10.dp))

        val cardShape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .background(palette.surface, cardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Hoy · ${stats.cardsStudiedToday} cartas · ${formatDuration(stats.elapsedMsToday)}",
                color = palette.text,
                fontSize = scaledSp(13f),
                fontWeight = FontWeight.Medium,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QueueChip("Nuevas", stats.newCount, Modifier.weight(1f))
                QueueChip("Aprender", stats.learningCount, Modifier.weight(1f))
                QueueChip("Repaso", stats.reviewCount, Modifier.weight(1f))
            }

            Text(
                "Hoy por hora (color = feedback)",
                color = palette.muted,
                fontSize = scaledSp(12f),
            )
            HourlyChart(hours = stats.hourlyToday)

            Text(
                heatmapTitle(stats),
                color = palette.muted,
                fontSize = scaledSp(12f),
            )
            HeatmapGrid(days = stats.heatmap)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(RATING_AGAIN, "Otra")
                LegendDot(RATING_HARD, "Difícil")
                LegendDot(RATING_GOOD, "Bien")
                LegendDot(RATING_EASY, "Fácil")
            }
        }
    }
}

@Composable
private fun QueueChip(label: String, count: Int, modifier: Modifier = Modifier) {
    val palette = LocalMemoPalette.current
    Column(
        modifier
            .background(palette.background.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            count.toString(),
            color = palette.primary,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(16f),
        )
        Text(label, color = palette.muted, fontSize = scaledSp(11f))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val palette = LocalMemoPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Text(label, color = palette.muted, fontSize = scaledSp(10f))
    }
}

@Composable
private fun HourlyChart(hours: List<HourActivity>) {
    val palette = LocalMemoPalette.current
    val max = (hours.maxOfOrNull { it.reviewCount } ?: 0).coerceAtLeast(1)
    val active = hours.filter { it.reviewCount > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp),
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
                            else feedbackColor(hour.ratingBuckets, hour.reviewCount),
                        ),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0h", color = palette.muted, fontSize = scaledSp(10f))
            Text("12h", color = palette.muted, fontSize = scaledSp(10f))
            Text("23h", color = palette.muted, fontSize = scaledSp(10f))
        }
        if (active.isNotEmpty()) {
            val peak = active.maxBy { it.reviewCount }
            Text(
                "Pico ${peak.hour}:00 · ${peak.reviewCount}",
                color = palette.text,
                fontSize = scaledSp(11f),
            )
        }
    }
}

@Composable
private fun HeatmapGrid(days: List<DayActivity>) {
    val palette = LocalMemoPalette.current
    val max = (days.maxOfOrNull { it.reviewCount } ?: 0).coerceAtLeast(1)
    val columns = 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { day ->
                    val count = day.reviewCount
                    val base = when {
                        count == 0 -> palette.border
                        else ->
                            feedbackColor(
                                day.ratingBuckets,
                                count,
                                intensity = (count.toFloat() / max).coerceIn(0.35f, 1f),
                            )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(base),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isLatest = day == days.last()
                        if (isLatest && count > 0) {
                            // Marca en último bucket para ubicar el "ahora" en modo adaptable.
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.85f)),
                            )
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
        val latest = days.lastOrNull()
        if (latest != null && latest.reviewCount > 0) {
            Text(
                "Último bloque · ${latest.reviewCount} cartas (marca blanca)",
                color = palette.muted,
                fontSize = scaledSp(11f),
            )
        }
    }
}

private fun feedbackColor(ratingBuckets: List<Int>, count: Int, intensity: Float = 1f): Color {
    if (count <= 0 || ratingBuckets.isEmpty()) return Color(0xFF94A3B8)
    val buckets = IntArray(4)
    for (i in 0 until minOf(4, ratingBuckets.size)) {
        buckets[i] = ratingBuckets[i].coerceAtLeast(0)
    }
    val total = buckets.sum().coerceAtLeast(1)
    val r = (
        RATING_AGAIN.red * buckets[0] +
            RATING_HARD.red * buckets[1] +
            RATING_GOOD.red * buckets[2] +
            RATING_EASY.red * buckets[3]
        ) / total.toFloat()
    val g = (
        RATING_AGAIN.green * buckets[0] +
            RATING_HARD.green * buckets[1] +
            RATING_GOOD.green * buckets[2] +
            RATING_EASY.green * buckets[3]
        ) / total.toFloat()
    val b = (
        RATING_AGAIN.blue * buckets[0] +
            RATING_HARD.blue * buckets[1] +
            RATING_GOOD.blue * buckets[2] +
            RATING_EASY.blue * buckets[3]
        ) / total.toFloat()
    val base = Color(r, g, b, alpha = 1f)
    return base.copy(alpha = (0.45f + 0.55f * intensity).coerceIn(0.35f, 1f))
}

private fun heatmapGranularityLabel(stats: ActivityStats): String {
    if (stats.heatmap.size < 2) return "día"
    val bucketMs = (stats.heatmap[1].dayStart - stats.heatmap[0].dayStart).coerceAtLeast(1L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(bucketMs)
    return when {
        minutes < 60L -> "${minutes.coerceAtLeast(1)} min"
        minutes < 24L * 60L -> "${(minutes / 60f).roundToInt()} h"
        else -> "${(minutes / (24f * 60f)).roundToInt()} d"
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
    val granularity = heatmapGranularityLabel(stats)
    return "Actividad adaptable · ${stats.heatmap.size} bloques de $granularity · color por feedback"
}

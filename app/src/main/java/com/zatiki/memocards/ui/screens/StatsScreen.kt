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
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

private val RATING_AGAIN = Color(0xFFDC2626)
private val RATING_HARD = Color(0xFFEA580C)
private val RATING_GOOD = Color(0xFF16A34A)
private val RATING_EASY = Color(0xFF2563EB)

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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Estadísticas",
            fontSize = scaledSp(24f),
            fontWeight = FontWeight.Bold,
            color = palette.text,
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
                .height(72.dp),
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
                            else feedbackColor(hour.ratingSum, hour.reviewCount),
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
    val todayStart = StudyDay.startOfToday()
    val columns = 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { day ->
                    val count = day.reviewCount
                    val isToday = day.dayStart == todayStart
                    val base = when {
                        count == 0 -> palette.border
                        else -> feedbackColor(day.ratingSum, count, intensity = (count.toFloat() / max).coerceIn(0.35f, 1f))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(base),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isToday && count > 0) {
                            // Intradía: mini barra inferior con intensidad relativa al pico del día.
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
        // Franja intradía del día actual bajo el heatmap.
        val today = days.lastOrNull { it.dayStart == todayStart }
        if (today != null && today.reviewCount > 0) {
            Text(
                "Hoy en heatmap · ${today.reviewCount} cartas (marca blanca)",
                color = palette.muted,
                fontSize = scaledSp(11f),
            )
        }
    }
}

private object StudyDay {
    fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

private fun feedbackColor(ratingSum: Int, count: Int, intensity: Float = 1f): Color {
    if (count <= 0 || ratingSum <= 0) return Color(0xFF94A3B8)
    val avg = ratingSum.toFloat() / count
    val base = when {
        avg < 1.75f -> RATING_AGAIN
        avg < 2.5f -> RATING_HARD
        avg < 3.25f -> RATING_GOOD
        else -> RATING_EASY
    }
    return base.copy(alpha = (0.45f + 0.55f * intensity).coerceIn(0.35f, 1f))
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
    return "Actividad · $month / $year · color por feedback"
}

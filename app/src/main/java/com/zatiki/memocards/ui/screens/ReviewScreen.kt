package com.zatiki.memocards.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class RatingUi(
    val rating: ReviewRating,
    val label: String,
    val shortLabel: String,
    val color: Color,
)

private val RATINGS = listOf(
    RatingUi(1, "Otra vez", "×", Color(0xFFDC2626)),
    RatingUi(2, "Difícil", "D", Color(0xFFEA580C)),
    RatingUi(3, "Bien", "B", Color(0xFF16A34A)),
    RatingUi(4, "Fácil", "F", Color(0xFF2563EB)),
)

@Composable
fun ReviewScreen(
    repo: MemoRepository,
    deckId: Long,
    deckName: String,
    settings: UiSettings,
    onDone: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<CardWithNote>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        loading = true
        queue = repo.getDueCards(deckId, 50)
        index = 0
        revealed = false
        finished = queue.isEmpty()
        loading = false
    }

    val current = queue.getOrNull(index)

    fun revealBack() {
        revealed = true
    }

    fun advanceAfterRating(cardId: Long, rating: ReviewRating) {
        // Primero ocultar reverso y avanzar de carta (mismo frame); luego persistir SM-2.
        revealed = false
        if (index + 1 >= queue.size) {
            finished = true
        } else {
            index += 1
        }
        scope.launch {
            repo.reviewCard(cardId, rating)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            when {
                loading -> Text("Cargando tarjetas…", color = palette.muted)
                finished || current == null -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Sesión completada", fontSize = scaledSp(24f), fontWeight = FontWeight.Bold, color = palette.text)
                        Spacer(Modifier.height(8.dp))
                        Text(deckName, color = palette.muted)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onDone) { Text("Volver al mazo") }
                    }
                }
                else -> {
                    key(current.card.id) {
                        Text(
                            "${index + 1} / ${queue.size}",
                            color = palette.muted,
                            fontSize = scaledSp(13f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(revealed, current.card.id) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        if (!revealed && dragAmount < -36f) revealBack()
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CardFacePanel(
                                title = "Anverso",
                                text = current.note.fields.front.ifBlank { "—" },
                                accent = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clickable {
                                        if (!revealed) revealBack()
                                    },
                            )
                            if (revealed) {
                                CardFacePanel(
                                    title = "Reverso",
                                    text = current.note.fields.back.ifBlank { "—" },
                                    accent = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                )
                            } else {
                                Box(Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (!revealed) {
                            Text(
                                "Toca la carta o desliza a la izquierda para ver el reverso.",
                                color = palette.muted,
                                fontSize = scaledSp(12f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { revealBack() },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 14.dp),
                            ) {
                                Text("Mostrar respuesta", fontSize = scaledSp(15f))
                            }
                        } else if (settings.ratingLayout == RatingLayout.BAR) {
                            RatingBar(
                                onRate = { rating -> advanceAfterRating(current.card.id, rating) },
                            )
                        } else {
                            // Reserva altura similar a la barra para que el arco no tape el reverso.
                            Spacer(Modifier.height(56.dp))
                        }
                    }
                }
            }
        }

        if (
            !loading &&
            !finished &&
            current != null &&
            revealed &&
            settings.ratingLayout != RatingLayout.BAR
        ) {
            RatingArcMenu(
                layout = settings.ratingLayout,
                onRate = { rating -> advanceAfterRating(current.card.id, rating) },
            )
        }
    }
}

@Composable
private fun RatingBar(onRate: (ReviewRating) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RATINGS.forEach { item ->
            Button(
                onClick = { onRate(item.rating) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = item.color),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
            ) {
                Text(item.label, fontSize = scaledSp(12f))
            }
        }
    }
}

/**
 * Dial radial tipo LexiCards: FAB flotante; toque abre; arrastre selecciona sector.
 */
@Composable
private fun RatingArcMenu(
    layout: RatingLayout,
    onRate: (ReviewRating) -> Unit,
) {
    val palette = LocalMemoPalette.current
    val right = layout == RatingLayout.ARC_RIGHT
    var expanded by remember { mutableStateOf(false) }
    var highlightIndex by remember { mutableIntStateOf(-1) }
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "arcExpand",
    )
    val density = LocalDensity.current
    val dialSize = 288.dp
    val fabSize = 56.dp
    val dialPx = with(density) { dialSize.toPx() }
    val fabPx = with(density) { fabSize.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = scaledSp(11f),
        fontWeight = FontWeight.Bold,
    )

    /** Ángulo matemático (Y hacia arriba): 0 = derecha, π/2 = arriba. */
    fun mathAngle(x: Float, y: Float, pivotX: Float, pivotY: Float): Float =
        atan2(-(y - pivotY), x - pivotX)

    fun sectorIndex(angle: Float): Int {
        if (right) {
            // Cuadrante π/2 (arriba) … π (izquierda)
            var a = angle
            if (a < 0f) a += (2f * PI).toFloat()
            if (a < PI.toFloat() / 2f || a > PI.toFloat()) return -1
            val t = (a - PI.toFloat() / 2f) / (PI.toFloat() / 2f)
            return (3 - (t * 4f).toInt().coerceIn(0, 3)).coerceIn(0, 3)
        }
        // Cuadrante 0 (derecha) … π/2 (arriba)
        if (angle < 0f || angle > PI.toFloat() / 2f) return -1
        val t = angle / (PI.toFloat() / 2f)
        return (t * 4f).toInt().coerceIn(0, 3)
    }

    fun commitHighlight() {
        val idx = highlightIndex
        if (idx in RATINGS.indices) onRate(RATINGS[idx].rating)
        expanded = false
        highlightIndex = -1
    }

    Box(Modifier.fillMaxSize()) {
        if (expandProgress > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f * expandProgress))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            expanded = false
                            highlightIndex = -1
                        }
                    },
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = if (right) Alignment.BottomEnd else Alignment.BottomStart,
        ) {
            Box(Modifier.size(dialSize)) {
                if (expandProgress > 0.01f) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(right) {
                                detectTapGestures { offset ->
                                    val pivotX = if (right) size.width.toFloat() else 0f
                                    val pivotY = size.height.toFloat()
                                    val dist = hypot(offset.x - pivotX, offset.y - pivotY)
                                    val outer = size.minDimension * 0.98f
                                    val inner = size.minDimension * 0.26f
                                    if (dist in inner..outer) {
                                        val idx = sectorIndex(mathAngle(offset.x, offset.y, pivotX, pivotY))
                                        if (idx in RATINGS.indices) {
                                            onRate(RATINGS[idx].rating)
                                            expanded = false
                                            highlightIndex = -1
                                            return@detectTapGestures
                                        }
                                    }
                                    expanded = false
                                    highlightIndex = -1
                                }
                            },
                    ) {
                        val pivot = Offset(if (right) size.width else 0f, size.height)
                        val outerR = size.minDimension * 0.98f * expandProgress
                        val innerR = size.minDimension * 0.26f * expandProgress
                        // Compose: 0° = 3 en punto; Y↓. Derecha: 180→90 antihorario. Izquierda: 0→270 antihorario.
                        val startAngleDeg = if (right) 180f else 0f
                        val sweepPer = -90f / RATINGS.size

                        RATINGS.forEachIndexed { i, item ->
                            val sectorStart = startAngleDeg + i * sweepPer
                            val path = Path().apply {
                                val startRad = Math.toRadians(sectorStart.toDouble())
                                moveTo(
                                    pivot.x + cos(startRad).toFloat() * innerR,
                                    pivot.y + sin(startRad).toFloat() * innerR,
                                )
                                arcTo(
                                    Rect(pivot.x - outerR, pivot.y - outerR, pivot.x + outerR, pivot.y + outerR),
                                    sectorStart,
                                    sweepPer,
                                    false,
                                )
                                arcTo(
                                    Rect(pivot.x - innerR, pivot.y - innerR, pivot.x + innerR, pivot.y + innerR),
                                    sectorStart + sweepPer,
                                    -sweepPer,
                                    false,
                                )
                                close()
                            }
                            val lit = highlightIndex == i
                            drawPath(
                                path,
                                item.color.copy(alpha = (if (lit) 1f else 0.86f) * expandProgress),
                            )
                            if (lit) {
                                drawPath(path, Color.White.copy(alpha = 0.4f * expandProgress), style = Stroke(4f))
                            }
                            val midDeg = sectorStart + sweepPer / 2f
                            val midRad = Math.toRadians(midDeg.toDouble())
                            val labelR = (innerR + outerR) / 2f
                            val lx = pivot.x + cos(midRad).toFloat() * labelR
                            val ly = pivot.y + sin(midRad).toFloat() * labelR
                            val measured = textMeasurer.measure(item.label.uppercase(), labelStyle)
                            drawText(
                                measured,
                                topLeft = Offset(lx - measured.size.width / 2f, ly - measured.size.height / 2f),
                            )
                        }

                        drawCircle(
                            color = Color(0xFF1E293B).copy(alpha = 0.94f * expandProgress),
                            radius = innerR * 0.9f,
                            center = pivot,
                        )
                        val hub = textMeasurer.measure(
                            "Dificultad",
                            TextStyle(
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = scaledSp(10f),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        drawText(
                            hub,
                            topLeft = Offset(
                                if (right) pivot.x - hub.size.width - 10f else pivot.x + 10f,
                                pivot.y - hub.size.height - 12f,
                            ),
                        )
                    }
                }

                Box(
                    Modifier
                        .align(if (right) Alignment.BottomEnd else Alignment.BottomStart)
                        .size(fabSize)
                        .background(
                            if (expanded) palette.primary else palette.card,
                            CircleShape,
                        )
                        .border(1.dp, palette.border, CircleShape)
                        .pointerInput(right) {
                            detectTapGestures {
                                expanded = !expanded
                                highlightIndex = -1
                            }
                        }
                        .pointerInput(right) {
                            detectDragGestures(
                                onDragStart = {
                                    expanded = true
                                    highlightIndex = -1
                                },
                                onDragEnd = { commitHighlight() },
                                onDragCancel = {
                                    expanded = false
                                    highlightIndex = -1
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pivotX = if (right) dialPx else 0f
                                    val pivotY = dialPx
                                    val fabOriginX = if (right) dialPx - fabPx else 0f
                                    val fabOriginY = dialPx - fabPx
                                    val x = fabOriginX + change.position.x
                                    val y = fabOriginY + change.position.y
                                    val dist = hypot(x - pivotX, y - pivotY)
                                    highlightIndex = if (dist > fabPx * 0.7f) {
                                        sectorIndex(mathAngle(x, y, pivotX, pivotY))
                                    } else {
                                        -1
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = if (expanded) "Cerrar dificultad" else "Calificar",
                        tint = if (expanded) Color.White else palette.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFacePanel(
    title: String,
    text: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    val borderColor = if (accent) palette.primary else palette.border

    Column(
        modifier
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(palette.card, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            title,
            color = if (accent) palette.primary else palette.muted,
            fontSize = scaledSp(12f),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text,
            color = palette.text,
            fontSize = scaledSp(21f),
        )
    }
}

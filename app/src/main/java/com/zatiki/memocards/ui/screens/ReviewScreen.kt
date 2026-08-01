package com.zatiki.memocards.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
    val icon: ImageVector,
)

private val RATINGS = listOf(
    RatingUi(1, "Otra vez", "×", Color(0xFFDC2626), Icons.Outlined.Refresh),
    RatingUi(2, "Difícil", "D", Color(0xFFEA580C), Icons.Outlined.ThumbDown),
    RatingUi(3, "Bien", "B", Color(0xFF16A34A), Icons.Outlined.ThumbUp),
    RatingUi(4, "Fácil", "F", Color(0xFF2563EB), Icons.Outlined.Star),
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
    val revealTapSource = remember { MutableInteractionSource() }
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
        if (!loading && !finished && current != null && !revealed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = revealTapSource,
                        onClick = { revealBack() },
                    ),
            )
        }
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
                        }
                        // Arco: FAB overlay; no reserva espacio en el layout.
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

@Composable
private fun rememberRatingIconPainters(): List<Painter> {
    val white = ColorFilter.tint(Color.White)
    val p0 = rememberVectorPainter(image = RATINGS[0].icon, colorFilter = white)
    val p1 = rememberVectorPainter(image = RATINGS[1].icon, colorFilter = white)
    val p2 = rememberVectorPainter(image = RATINGS[2].icon, colorFilter = white)
    val p3 = rememberVectorPainter(image = RATINGS[3].icon, colorFilter = white)
    return listOf(p0, p1, p2, p3)
}

/**
 * Abanico semicircular 180° anclado al lateral pulsado (centro en Y del toque).
 * Tamaño ≈ un tercio del diámetro anterior; iconos blancos y texto bold más grande.
 */
@Composable
private fun RatingArcMenu(
    layout: RatingLayout,
    onRate: (ReviewRating) -> Unit,
) {
    val palette = LocalMemoPalette.current
    val defaultRight = layout == RatingLayout.ARC_RIGHT
    var arcFromRight by remember { mutableStateOf(defaultRight) }
    var arcPivotYFraction by remember { mutableFloatStateOf(0.5f) }
    var expanded by remember { mutableStateOf(false) }
    var highlightIndex by remember { mutableIntStateOf(-1) }
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "arcExpand",
    )
    val density = LocalDensity.current
    val fabSize = 56.dp
    val edgePad = 12.dp
    val fabPx = with(density) { fabSize.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = scaledSp(16f),
        fontWeight = FontWeight.Bold,
    )
    val iconPainters = rememberRatingIconPainters()
    val gapDeg = 4f
    val totalSweepAbs = 180f
    val sectorSweepAbs = (totalSweepAbs - gapDeg * (RATINGS.size - 1)) / RATINGS.size
    val startAngleDeg = 270f
    val sweepSign get() = if (arcFromRight) -1f else 1f

    fun mathAngle(x: Float, y: Float, pivotX: Float, pivotY: Float): Float =
        atan2(-(y - pivotY), x - pivotX)

    fun sectorIndex(angle: Float): Int {
        if (arcFromRight) {
            var a = angle
            if (a < 0f) a += (2f * PI).toFloat()
            if (a < PI.toFloat() / 2f || a > 3f * PI.toFloat() / 2f) return -1
            val t = ((a - PI.toFloat() / 2f) / PI.toFloat()).coerceIn(0f, 0.999f)
            return (RATINGS.lastIndex - (t * RATINGS.size).toInt()).coerceIn(0, RATINGS.lastIndex)
        }
        if (angle > PI.toFloat() / 2f || angle < -PI.toFloat() / 2f) return -1
        val t = ((PI.toFloat() / 2f - angle) / PI.toFloat()).coerceIn(0f, 0.999f)
        return (RATINGS.lastIndex - (t * RATINGS.size).toInt()).coerceIn(0, RATINGS.lastIndex)
    }

    fun openArc(fromRight: Boolean, yFraction: Float) {
        arcFromRight = fromRight
        arcPivotYFraction = yFraction.coerceIn(0.12f, 0.88f)
        expanded = true
        highlightIndex = -1
    }

    fun commitHighlight() {
        val idx = highlightIndex
        if (idx in RATINGS.indices) onRate(RATINGS[idx].rating)
        expanded = false
        highlightIndex = -1
    }

    Box(Modifier.fillMaxSize()) {
        if (!expanded) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                openArc(false, offset.y / size.height.toFloat())
                            }
                        },
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                openArc(true, offset.y / size.height.toFloat())
                            }
                        },
                )
            }
        }

        if (expandProgress > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f * expandProgress))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            expanded = false
                            highlightIndex = -1
                        }
                    },
            )
        }

        if (expandProgress > 0.01f) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(edgePad)
                    .pointerInput(arcFromRight, arcPivotYFraction) {
                        detectTapGestures { offset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val pivot = Offset(
                                if (arcFromRight) w else 0f,
                                h * arcPivotYFraction,
                            )
                            val outerFull = minOf(w, h) * 0.92f / 3f
                            val inner = outerFull * 0.28f
                            val dist = hypot(offset.x - pivot.x, offset.y - pivot.y)
                            if (dist in inner..outerFull) {
                                val idx = sectorIndex(
                                    mathAngle(offset.x, offset.y, pivot.x, pivot.y),
                                )
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
                val pivot = Offset(
                    if (arcFromRight) size.width else 0f,
                    size.height * arcPivotYFraction,
                )
                val outerFull = minOf(size.width, size.height) * 0.92f / 3f
                val outerR = outerFull * expandProgress
                val innerR = outerFull * 0.28f
                val iconSize = outerR * 0.22f

                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f * expandProgress),
                    radius = innerR * 0.9f,
                    center = pivot,
                )

                RATINGS.forEachIndexed { i, item ->
                    val visualIndex = RATINGS.lastIndex - i
                    val sectorStart = startAngleDeg + sweepSign * visualIndex * (sectorSweepAbs + gapDeg)
                    val sweep = sweepSign * sectorSweepAbs
                    val path = Path().apply {
                        val startRad = Math.toRadians(sectorStart.toDouble())
                        moveTo(
                            pivot.x + cos(startRad).toFloat() * innerR,
                            pivot.y + sin(startRad).toFloat() * innerR,
                        )
                        arcTo(
                            Rect(pivot.x - outerR, pivot.y - outerR, pivot.x + outerR, pivot.y + outerR),
                            sectorStart,
                            sweep,
                            false,
                        )
                        arcTo(
                            Rect(pivot.x - innerR, pivot.y - innerR, pivot.x + innerR, pivot.y + innerR),
                            sectorStart + sweep,
                            -sweep,
                            false,
                        )
                        close()
                    }
                    val lit = highlightIndex == i
                    drawPath(path, item.color.copy(alpha = (if (lit) 1f else 0.9f) * expandProgress))
                    if (lit) {
                        drawPath(path, Color.White.copy(alpha = 0.4f * expandProgress), style = Stroke(4f))
                    }

                    val midDeg = sectorStart + sweep / 2f
                    val midRad = Math.toRadians(midDeg.toDouble())
                    val labelR = (innerR + outerR) / 2f
                    val lx = pivot.x + cos(midRad).toFloat() * labelR
                    val ly = pivot.y + sin(midRad).toFloat() * labelR
                    val measured = textMeasurer.measure(item.label.uppercase(), labelStyle)
                    val textRotation = midDeg + if (arcFromRight) 180f else 0f
                    val iconTopLeft = Offset(
                        lx - iconSize / 2f,
                        ly - iconSize * 1.15f - measured.size.height / 2f,
                    )
                    translate(left = iconTopLeft.x, top = iconTopLeft.y) {
                        with(iconPainters[i]) {
                            draw(
                                size = Size(iconSize, iconSize),
                                alpha = expandProgress,
                            )
                        }
                    }
                    rotate(degrees = textRotation, pivot = Offset(lx, ly)) {
                        drawText(
                            measured,
                            topLeft = Offset(
                                lx - measured.size.width / 2f,
                                ly - measured.size.height / 2f,
                            ),
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(edgePad),
            contentAlignment = if (defaultRight) Alignment.BottomEnd else Alignment.BottomStart,
        ) {
            Box(
                Modifier
                    .size(fabSize)
                    .background(if (expanded) palette.primary else palette.card, CircleShape)
                    .border(1.dp, palette.border, CircleShape)
                    .pointerInput(defaultRight) {
                        detectTapGestures {
                            arcFromRight = defaultRight
                            arcPivotYFraction = 0.5f
                            expanded = !expanded
                            highlightIndex = -1
                        }
                    }
                    .pointerInput(defaultRight) {
                        detectDragGestures(
                            onDragStart = {
                                arcFromRight = defaultRight
                                arcPivotYFraction = 0.5f
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
                                val dy = change.position.y - fabPx / 2f
                                val dx = change.position.x - fabPx / 2f
                                val score = if (defaultRight) -dx - dy else dx - dy
                                highlightIndex = when {
                                    score > fabPx * 1.4f -> 3
                                    score > fabPx * 0.7f -> 2
                                    score > 0f -> 1
                                    else -> 0
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

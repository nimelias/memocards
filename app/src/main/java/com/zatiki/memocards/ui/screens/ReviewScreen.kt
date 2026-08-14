package com.zatiki.memocards.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.ArcLabelMode
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.ClozeFormat
import com.zatiki.memocards.ui.components.AmbientGlowBackdrop
import com.zatiki.memocards.ui.components.memoGlass
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay
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
    advanceDays: Int = 0,
    queueFilter: String = "all",
    onDone: () -> Unit,
    onSessionEnd: () -> Unit = {},
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    val revealTapSource = remember { MutableInteractionSource() }
    var queue by remember { mutableStateOf<List<CardWithNote>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var revealedAt by remember { mutableStateOf(0L) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var pendingReviews by remember { mutableIntStateOf(0) }
    var deckReviewCount by remember { mutableIntStateOf(0) }

    suspend fun loadQueue() {
        val keepCardVisible = queue.isNotEmpty() && !finished
        if (!keepCardVisible) loading = true
        queue = repo.getDueCards(deckId, 50, advanceDays, queueFilter)
        deckReviewCount = repo.countDeckReviews(deckId)
        index = 0
        revealed = false
        revealedAt = 0L
        finished = queue.isEmpty()
        loading = false
    }

    LaunchedEffect(deckId, advanceDays, queueFilter, sessionKey) {
        loadQueue()
    }

    fun finishAndLeave() {
        scope.launch {
            var waited = 0
            while (pendingReviews > 0 && waited < 1000) {
                delay(20)
                waited += 20
            }
            onSessionEnd()
            onDone()
        }
    }

    BackHandler { finishAndLeave() }

    val current = queue.getOrNull(index)

    fun revealBack() {
        revealed = true
        revealedAt = System.currentTimeMillis()
    }

    fun advanceAfterRating(cardId: Long, rating: ReviewRating) {
        val elapsedMs = if (revealedAt > 0L) {
            (System.currentTimeMillis() - revealedAt).coerceAtLeast(0L)
        } else 0L
        revealed = false
        revealedAt = 0L
        if (index + 1 >= queue.size) {
            finished = true
        } else {
            index += 1
        }
        pendingReviews += 1
        scope.launch {
            try {
                repo.reviewCard(cardId, rating, elapsedMs)
            } catch (_: Exception) {
                // Evita cierre silencioso si falla el guardado del repaso.
            } finally {
                pendingReviews = (pendingReviews - 1).coerceAtLeast(0)
                // Refresco incremental tras cada feedback (no solo al salir).
                onSessionEnd()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        AmbientGlowBackdrop(theme = settings.theme, intensity = settings.glowIntensity) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
            ) {
                when {
                    loading -> Spacer(Modifier.weight(1f))
                    finished || current == null -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Great Job! ✨",
                                fontSize = scaledSp(28f),
                                fontWeight = FontWeight.Bold,
                                color = palette.text,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(deckName, color = palette.muted, fontSize = scaledSp(15f))
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { sessionKey += 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                modifier = Modifier.fillMaxWidth(0.7f),
                            ) {
                                Text("RESTART", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { finishAndLeave() },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                            ) {
                                Text("Volver al mazo", color = palette.text)
                            }
                        }
                    }
                    else -> {
                        val front = current.note.fields.front
                        val back = current.note.fields.back
                        val cloze = ClozeFormat.isCloze(front)
                        key(current.card.id) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    deckName,
                                    color = palette.text,
                                    fontSize = scaledSp(18f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${index + 1} / ${queue.size}",
                                    color = palette.muted,
                                    fontSize = scaledSp(14f),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratedStudyCard(
                                front = front,
                                back = back,
                                cloze = cloze,
                                revealed = revealed,
                                onReveal = { revealBack() },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                            Spacer(Modifier.height(2.dp))
                            if (revealed && settings.ratingLayout == RatingLayout.BAR) {
                                RatingBar(
                                    onRate = { rating -> advanceAfterRating(current.card.id, rating) },
                                )
                            } else {
                                Spacer(Modifier.height(36.dp))
                            }
                        }
                    }
                }
            }

            if (!loading && !finished && current != null && !revealed) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount < -36f) revealBack()
                            }
                        }
                        .clickable(
                            indication = null,
                            interactionSource = revealTapSource,
                            onClick = { revealBack() },
                        ),
                )
            }

            if (
                !loading &&
                !finished &&
                current != null &&
                settings.ratingLayout.isArc
            ) {
                RatingArcMenu(
                    layout = settings.ratingLayout,
                    labelMode = settings.arcLabelMode,
                    interactive = revealed,
                    hintDelayMs = if (deckReviewCount < 3) 2_500L else 8_000L,
                    onRate = { rating -> advanceAfterRating(current.card.id, rating) },
                )
            }
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
 * Abanico semicircular 180° anclado al lateral pulsado (centro en Y del toque).
 * Tamaño ≈ un tercio del diámetro anterior; iconos blancos y texto bold más grande.
 */
@Composable
private fun RatingArcMenu(
    layout: RatingLayout,
    labelMode: ArcLabelMode,
    interactive: Boolean,
    hintDelayMs: Long = 8_000L,
    onRate: (ReviewRating) -> Unit,
) {
    val defaultRight = layout == RatingLayout.ARC_RIGHT
    var arcFromRight by remember { mutableStateOf(defaultRight) }
    var arcPivotYFraction by remember { mutableFloatStateOf(0.5f) }
    var expanded by remember { mutableStateOf(false) }
    var arcInputEnabled by remember { mutableStateOf(false) }
    var highlightIndex by remember { mutableIntStateOf(-1) }
    var showSideHint by remember { mutableStateOf(false) }
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) {
            tween(durationMillis = 120)
        } else {
            tween(durationMillis = 100)
        },
        label = "arcExpand",
    )
    val edgePad = 12.dp
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = scaledSp(if (labelMode == ArcLabelMode.TEXT) 18f else 16f),
        fontWeight = FontWeight.Bold,
    )
    val refreshPainter = rememberVectorPainter(Icons.Outlined.Refresh)
    val thumbDownPainter = rememberVectorPainter(Icons.Outlined.ThumbDown)
    val thumbUpPainter = rememberVectorPainter(Icons.Outlined.ThumbUp)
    val starPainter = rememberVectorPainter(Icons.Outlined.Star)
    val iconPainters = listOf(refreshPainter, thumbDownPainter, thumbUpPainter, starPainter)
    val textLayouts = remember(labelMode, labelStyle) {
        if (labelMode == ArcLabelMode.TEXT) {
            RATINGS.map { textMeasurer.measure(it.label.uppercase(), labelStyle) }
        } else {
            emptyList()
        }
    }
    val shadowPaint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }
    val totalSweepAbs = 180f
    val sectorSweepAbs = totalSweepAbs / RATINGS.size
    val startAngleDeg = 270f
    val sweepSign = if (arcFromRight) -1f else 1f

    LaunchedEffect(interactive) {
        if (!interactive) {
            expanded = false
            arcInputEnabled = false
            highlightIndex = -1
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(130)
            if (expanded) arcInputEnabled = true
        } else {
            arcInputEnabled = false
        }
    }

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
        showSideHint = false
        highlightIndex = -1
    }

    LaunchedEffect(interactive, hintDelayMs, expanded) {
        showSideHint = false
        if (!interactive || expanded) return@LaunchedEffect
        delay(hintDelayMs)
        if (interactive && !expanded) showSideHint = true
    }

    var hintPulse by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(showSideHint, interactive, expanded) {
        hintPulse = 0f
        if (!showSideHint || !interactive || expanded) return@LaunchedEffect
        while (true) {
            hintPulse = 0.50f
            delay(1_000)
            hintPulse = 0f
            delay(2_500)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(edgePad)
                .graphicsLayer {
                    alpha = if (interactive) expandProgress.coerceIn(0f, 1f) else 0f
                }
                .then(
                    if (interactive && arcInputEnabled) {
                        Modifier.pointerInput(arcFromRight, arcPivotYFraction) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val pivot = Offset(
                                    if (arcFromRight) w else 0f,
                                    h * arcPivotYFraction,
                                )
                                val outerFull = minOf(w, h) * 0.92f / 3f * 1.2f
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
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (!interactive && expandProgress < 0.01f) {
                // Precalienta sombra y pintores en el primer frame sin mostrar el menú.
                drawIntoCanvas { canvas ->
                    shadowPaint.color = android.graphics.Color.TRANSPARENT
                    shadowPaint.setShadowLayer(1f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    canvas.nativeCanvas.drawCircle(0f, 0f, 1f, shadowPaint)
                }
                return@Canvas
            }
            if (expandProgress < 0.01f) return@Canvas
            val pivot = Offset(
                if (arcFromRight) size.width else 0f,
                size.height * arcPivotYFraction,
            )
            val outerFull = minOf(size.width, size.height) * 0.92f / 3f * 1.2f
            val outerR = outerFull * expandProgress
            val innerR = outerFull * 0.34f
            val iconSize = outerR * 0.2f
            val shadowAlpha = (0.58f * expandProgress).coerceIn(0f, 1f)
            val totalSweepDeg = sweepSign * totalSweepAbs

            val semicirclePath = Path().apply {
                val startRad = Math.toRadians(startAngleDeg.toDouble())
                moveTo(
                    pivot.x + cos(startRad).toFloat() * innerR,
                    pivot.y + sin(startRad).toFloat() * innerR,
                )
                arcTo(
                    Rect(pivot.x - outerR, pivot.y - outerR, pivot.x + outerR, pivot.y + outerR),
                    startAngleDeg,
                    totalSweepDeg,
                    false,
                )
                arcTo(
                    Rect(pivot.x - innerR, pivot.y - innerR, pivot.x + innerR, pivot.y + innerR),
                    startAngleDeg + totalSweepDeg,
                    -totalSweepDeg,
                    false,
                )
                close()
            }

            drawIntoCanvas { canvas ->
                shadowPaint.color = android.graphics.Color.TRANSPARENT
                shadowPaint.setShadowLayer(
                    36f * expandProgress,
                    8f,
                    12f,
                    android.graphics.Color.argb((shadowAlpha * 255).toInt(), 0, 0, 0),
                )
                shadowPaint.color = Color.DarkGray.copy(alpha = 0.72f * expandProgress).toArgb()
                canvas.nativeCanvas.drawPath(semicirclePath.asAndroidPath(), shadowPaint)
            }

            drawCircle(
                color = Color.Black.copy(alpha = 0.18f * expandProgress),
                radius = innerR * 0.9f,
                center = pivot,
            )

            RATINGS.forEachIndexed { i, item ->
                val visualIndex = RATINGS.lastIndex - i
                val sectorStart = startAngleDeg + sweepSign * visualIndex * sectorSweepAbs
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
                val fillColor = item.color.copy(alpha = (if (lit) 1f else 0.9f) * expandProgress)
                drawPath(path, fillColor)
                if (lit) {
                    drawPath(path, Color.White.copy(alpha = 0.35f * expandProgress), style = Stroke(3f))
                }
            }

            RATINGS.forEachIndexed { i, item ->
                val visualIndex = RATINGS.lastIndex - i
                val sectorStart = startAngleDeg + sweepSign * visualIndex * sectorSweepAbs
                val sweep = sweepSign * sectorSweepAbs
                val midDeg = sectorStart + sweep / 2f
                val midRad = Math.toRadians(midDeg.toDouble())
                val labelR = (innerR + outerR) / 2f
                val lx = pivot.x + cos(midRad).toFloat() * labelR
                val ly = pivot.y + sin(midRad).toFloat() * labelR
                val textRotation = midDeg + if (arcFromRight) 180f else 0f
                rotate(degrees = textRotation, pivot = Offset(lx, ly)) {
                    when (labelMode) {
                        ArcLabelMode.ICONS -> {
                            val iconDrawSize = iconSize * 1.15f
                            translate(left = lx - iconDrawSize / 2f, top = ly - iconDrawSize / 2f) {
                                with(iconPainters[i]) {
                                    draw(
                                        size = Size(iconDrawSize, iconDrawSize),
                                        alpha = expandProgress,
                                        colorFilter = ColorFilter.tint(Color.White),
                                    )
                                }
                            }
                        }
                        ArcLabelMode.TEXT -> {
                            val measured = textLayouts[i]
                            translate(
                                left = lx - measured.size.width / 2f,
                                top = ly - measured.size.height / 2f,
                            ) {
                                drawText(measured)
                            }
                        }
                    }
                }
            }
        }

        if (interactive && !expanded) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth(0.25f)
                        .fillMaxHeight()
                        .background(Color(0xFF4A7AFF).copy(alpha = hintPulse))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                openArc(false, offset.y / size.height.toFloat())
                            }
                        },
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .fillMaxWidth(0.25f)
                        .fillMaxHeight()
                        .background(Color(0xFF4A7AFF).copy(alpha = hintPulse))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                openArc(true, offset.y / size.height.toFloat())
                            }
                        },
                )
            }
        }
    }
}

/**
 * Carta Emich: anverso y reverso en un solo contenedor (sin paneles partidos).
 * Cloze revela in-place; Q&A muestra pregunta arriba y respuesta debajo.
 */
@Composable
private fun IntegratedStudyCard(
    front: String,
    back: String,
    cloze: Boolean,
    revealed: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier
            .memoGlass(palette, shape, alpha = 0.78f, elevation = 10.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                cloze && revealed -> {
                    Text(
                        ClozeFormat.revealed(front, back, palette.primary),
                        color = palette.text,
                        fontSize = scaledSp(22f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                cloze -> {
                    Text(
                        ClozeFormat.prompt(front).ifBlank { "—" },
                        color = palette.text,
                        fontSize = scaledSp(22f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                revealed -> {
                    Text(
                        front.ifBlank { "—" },
                        color = palette.muted,
                        fontSize = scaledSp(17f),
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        back.ifBlank { "—" },
                        color = palette.text,
                        fontSize = scaledSp(22f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(
                        front.ifBlank { "—" },
                        color = palette.text,
                        fontSize = scaledSp(22f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (!revealed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onReveal,
                    ),
                shape = RoundedCornerShape(28.dp),
                color = palette.primary.copy(alpha = 0.92f),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "SHOW ANSWER",
                        fontSize = scaledSp(14f),
                        fontWeight = FontWeight.Bold,
                        color = palette.onPrimary,
                    )
                }
            }
        } else {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
        }
    }
}

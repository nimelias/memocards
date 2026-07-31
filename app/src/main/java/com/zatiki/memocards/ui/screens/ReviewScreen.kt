package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlin.math.cos
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

@Composable
private fun RatingArcMenu(
    layout: RatingLayout,
    onRate: (ReviewRating) -> Unit,
) {
    val right = layout == RatingLayout.ARC_RIGHT
    val alignment = if (right) Alignment.BottomEnd else Alignment.BottomStart
    val radius = 118.dp
    val buttonSize = 52.dp

    Box(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = alignment,
    ) {
        Box(Modifier.size(radius + buttonSize / 2)) {
            RATINGS.forEachIndexed { i, item ->
                val t = if (RATINGS.size == 1) 0.0 else i / (RATINGS.size - 1).toDouble()
                // Derecha: de izquierda (π) a arriba (π/2). Izquierda: de derecha (0) a arriba (π/2).
                val angle = if (right) {
                    Math.PI - (Math.PI / 2.0) * t
                } else {
                    (Math.PI / 2.0) * t
                }
                val x = (cos(angle) * radius.value).dp
                val y = (-sin(angle) * radius.value).dp
                val pivotX = if (right) radius else 0.dp
                val pivotY = radius
                ArcRatingButton(
                    item = item,
                    size = buttonSize,
                    modifier = Modifier.offset(
                        x = pivotX + x - buttonSize / 2,
                        y = pivotY + y - buttonSize / 2,
                    ),
                    onClick = { onRate(item.rating) },
                )
            }
        }
    }
}

@Composable
private fun ArcRatingButton(
    item: RatingUi,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(item.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            item.shortLabel,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(16f),
            textAlign = TextAlign.Center,
        )
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

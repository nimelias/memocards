package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

private data class RatingUi(val rating: ReviewRating, val label: String, val color: Color)
private enum class CardSide { FRONT, BACK }

private val RATINGS = listOf(
    RatingUi(1, "Otra vez", Color(0xFFDC2626)),
    RatingUi(2, "Difícil", Color(0xFFEA580C)),
    RatingUi(3, "Bien", Color(0xFF16A34A)),
    RatingUi(4, "Fácil", Color(0xFF2563EB)),
)

@Composable
fun ReviewScreen(
    repo: MemoRepository,
    deckId: Long,
    deckName: String,
    onDone: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<CardWithNote>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var focusedSide by remember { mutableStateOf(CardSide.FRONT) }
    var loading by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        loading = true
        queue = repo.getDueCards(deckId, 50)
        index = 0
        focusedSide = CardSide.FRONT
        finished = queue.isEmpty()
        loading = false
    }

    val current = queue.getOrNull(index)

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 36f) focusedSide = CardSide.FRONT
                                if (dragAmount < -36f) focusedSide = CardSide.BACK
                            }
                        },
                ) {
                    CardFacePanel(
                        title = "Anverso",
                        text = current.note.fields.front.ifBlank { "—" },
                        focused = focusedSide == CardSide.FRONT,
                        onFocus = { focusedSide = CardSide.FRONT },
                    )
                    Spacer(Modifier.height(12.dp))
                    CardFacePanel(
                        title = "Reverso",
                        text = current.note.fields.back.ifBlank { "—" },
                        focused = focusedSide == CardSide.BACK,
                        onFocus = { focusedSide = CardSide.BACK },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Desliza horizontalmente o toca un panel para cambiar el foco.",
                    color = palette.muted,
                    fontSize = scaledSp(12f),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RATINGS.forEach { item ->
                        Button(
                            onClick = {
                                scope.launch {
                                    repo.reviewCard(current.card.id, item.rating)
                                    if (index + 1 >= queue.size) {
                                        finished = true
                                    } else {
                                        index += 1
                                        focusedSide = CardSide.FRONT
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = item.color),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                        ) {
                            Text(item.label, fontSize = scaledSp(12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardFacePanel(
    title: String,
    text: String,
    focused: Boolean,
    onFocus: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val borderColor = if (focused) palette.primary else palette.border

    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(palette.card, RoundedCornerShape(16.dp))
            .clickable(onClick = onFocus)
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            title,
            color = if (focused) palette.primary else palette.muted,
            fontSize = scaledSp(12f),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text,
            color = palette.text,
            fontSize = scaledSp(if (focused) 21f else 19f),
        )
    }
}

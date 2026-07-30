package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

private data class RatingUi(val rating: ReviewRating, val label: String, val color: Color)

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

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp),
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
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                        .background(palette.card, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        if (revealed) "Reverso" else "Frente",
                        color = palette.muted,
                        fontSize = scaledSp(12f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    val text = if (revealed) current.note.fields.back else current.note.fields.front
                    Text(
                        text.ifBlank { "—" },
                        color = palette.text,
                        fontSize = scaledSp(22f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (!revealed) {
                    Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mostrar respuesta")
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                            revealed = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = item.color),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            ) {
                                Text(item.label, fontSize = scaledSp(11f))
                            }
                        }
                    }
                }
            }
        }
    }
}

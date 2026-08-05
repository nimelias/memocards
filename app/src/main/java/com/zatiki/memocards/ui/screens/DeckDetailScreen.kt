package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSettings
import com.zatiki.memocards.domain.DeckStats
import com.zatiki.memocards.domain.Note
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun DeckDetailScreen(
    repo: MemoRepository,
    deckId: Long,
    deckName: String,
    onBack: () -> Unit,
    onReview: () -> Unit,
    onPreviewReview: (advanceDays: Int) -> Unit,
    onAddNote: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var deck by remember { mutableStateOf<Deck?>(null) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var stats by remember { mutableStateOf(DeckStats(0, 0, 0)) }
    var studyDaysText by remember { mutableStateOf("") }
    var minRepText by remember { mutableStateOf("1") }

    suspend fun reload() {
        deck = repo.getDeck(deckId)
        notes = repo.listNotes(deckId)
        stats = repo.getDeckStats(deckId)
        studyDaysText = deck?.studyDays?.toString().orEmpty()
        minRepText = (deck?.minRepetitions ?: 1).toString()
    }

    LaunchedEffect(deckId) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = palette.text)
            }
            Text(
                deckName,
                fontSize = scaledSp(22f),
                fontWeight = FontWeight.Bold,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Nuevas ${stats.newCount} · Pendientes ${stats.dueCount} · Total ${stats.total}",
            color = palette.muted,
            fontSize = scaledSp(13f),
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReview, modifier = Modifier.weight(1f)) { Text("Repasar") }
            OutlinedButton(onClick = onAddNote, modifier = Modifier.weight(1f)) { Text("+ Tarjeta") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onPreviewReview(7) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Vista previa (+7 días)")
        }

        Spacer(Modifier.height(12.dp))
        Text("Ajustes del mazo", fontWeight = FontWeight.SemiBold, color = palette.text, fontSize = scaledSp(15f))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = studyDaysText,
                onValueChange = { studyDaysText = it.filter { ch -> ch.isDigit() } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Días estudio") },
            )
            OutlinedTextField(
                value = minRepText,
                onValueChange = { minRepText = it.filter { ch -> ch.isDigit() } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Min. reps") },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repo.updateDeckSettings(
                            deckId,
                            DeckSettings(
                                studyDays = studyDaysText.toIntOrNull(),
                                minRepetitions = minRepText.toIntOrNull() ?: 1,
                            ),
                        )
                        reload()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Guardar ajustes") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repo.resetDeck(deckId)
                        reload()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Reiniciar progreso") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Tarjetas", fontWeight = FontWeight.SemiBold, color = palette.text, fontSize = scaledSp(15f))
        Spacer(Modifier.height(8.dp))

        if (notes.isEmpty()) {
            Text("Sin tarjetas en este mazo.", color = palette.muted)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                            .background(palette.card, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        Text(note.fields.front.ifBlank { "(sin frente)" }, color = palette.text, fontSize = scaledSp(15f))
                        if (note.fields.back.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(note.fields.back, color = palette.muted, fontSize = scaledSp(13f))
                        }
                    }
                }
            }
        }
    }
}

package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.NoteSearchHit
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    repo: MemoRepository,
    onOpenDeck: (Deck) -> Unit,
    onOpenNoteDeck: (deckId: Long) -> Unit,
) {
    val palette = LocalMemoPalette.current
    var query by remember { mutableStateOf("") }
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteSearchHit>>(emptyList()) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            decks = emptyList()
            notes = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        decks = repo.searchDecks(q)
        notes = repo.searchNotes(q)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            "Buscar",
            fontSize = scaledSp(26f),
            fontWeight = FontWeight.Bold,
            color = palette.text,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Mazos o texto de cartas", color = palette.muted) },
        )
        Spacer(Modifier.height(16.dp))

        when {
            query.trim().length < 2 -> {
                Text(
                    "Escribe al menos 2 caracteres.",
                    color = palette.muted,
                    fontSize = scaledSp(14f),
                )
            }
            decks.isEmpty() && notes.isEmpty() -> {
                Text(
                    "Sin resultados.",
                    color = palette.muted,
                    fontSize = scaledSp(14f),
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (decks.isNotEmpty()) {
                        item {
                            Text(
                                "Mazos",
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = scaledSp(13f),
                            )
                        }
                        items(decks, key = { "d-${it.id}" }) { deck ->
                            ResultRow(
                                title = deck.name,
                                subtitle = "Mazo",
                                onClick = { onOpenDeck(deck) },
                            )
                        }
                    }
                    if (notes.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cartas",
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = scaledSp(13f),
                            )
                        }
                        items(notes, key = { "n-${it.noteId}" }) { hit ->
                            ResultRow(
                                title = hit.frontPreview.ifBlank { "(sin texto)" },
                                subtitle = hit.deckName,
                                onClick = { onOpenNoteDeck(hit.deckId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, color = palette.text, fontSize = scaledSp(15f), fontWeight = FontWeight.Medium)
        Text(subtitle, color = palette.muted, fontSize = scaledSp(12f))
    }
}

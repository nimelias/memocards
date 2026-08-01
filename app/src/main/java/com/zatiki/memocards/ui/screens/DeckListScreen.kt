package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun DeckListScreen(
    repo: MemoRepository,
    settings: UiSettings,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDeck: (Deck) -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var name by remember { mutableStateOf("") }

    suspend fun reload() {
        decks = repo.listDecks()
    }

    LaunchedEffect(Unit) {
        repo.ensureDemoDeckIfNeeded()
        reload()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mazos",
                fontSize = scaledSp(28f),
                fontWeight = FontWeight.Bold,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (settings.theme == ThemeName.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Tema",
                    tint = palette.text,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = palette.text)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Nombre del mazo", color = palette.muted) },
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) return@Button
                    scope.launch {
                        repo.createDeck(trimmed)
                        name = ""
                        reload()
                    }
                },
            ) { Text("Crear") }
        }

        Spacer(Modifier.height(16.dp))

        if (decks.isEmpty()) {
            Text("Sin mazos todavía. Crea el primero.", color = palette.muted, fontSize = scaledSp(15f))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(decks, key = { it.id }) { deck ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, shape)
                            .background(palette.card, shape)
                            .clickable { onOpenDeck(deck) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            deck.name,
                            color = palette.text,
                            fontSize = scaledSp(17f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

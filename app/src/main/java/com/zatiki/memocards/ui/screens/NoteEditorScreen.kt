package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.NoteFields
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    repo: MemoRepository,
    deckId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var cardType by remember { mutableStateOf("basic") }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "", "", "")) }
    var correctIndex by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    val canSave = if (cardType == "mcq") {
        front.isNotBlank() && options.count { it.isNotBlank() } >= 2
    } else {
        front.isNotBlank() || back.isNotBlank()
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = palette.text)
            }
            Text(
                "Nueva tarjeta",
                fontSize = scaledSp(22f),
                fontWeight = FontWeight.Bold,
                color = palette.text,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = cardType == "basic",
                onClick = { cardType = "basic" },
                label = { Text("Anverso / reverso") },
            )
            FilterChip(
                selected = cardType == "mcq",
                onClick = { cardType = "mcq" },
                label = { Text("Tipo test") },
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (cardType == "mcq") {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enunciado") },
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                Text("Opciones (marca la correcta)", color = palette.muted, fontSize = scaledSp(13f))
                Spacer(Modifier.height(8.dp))
                options.forEachIndexed { idx, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = correctIndex == idx,
                            onClick = { correctIndex = idx },
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { next ->
                                options = options.toMutableList().also { it[idx] = next }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Opción ${idx + 1}") },
                            singleLine = true,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Frente") },
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reverso") },
                    minLines = 3,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cloze: usa [...] en el frente y la respuesta en el reverso, o {{c1::texto}}.",
                    color = palette.muted,
                    fontSize = scaledSp(12f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !saving && canSave,
            onClick = {
                saving = true
                scope.launch {
                    val fields = if (cardType == "mcq") {
                        val cleaned = options.map { it.trim() }.filter { it.isNotEmpty() }
                        val idx = correctIndex.coerceIn(0, (cleaned.size - 1).coerceAtLeast(0))
                        NoteFields(
                            front = front.trim(),
                            back = cleaned.getOrElse(idx) { "" },
                            type = "mcq",
                            options = cleaned,
                            correctIndex = idx,
                        )
                    } else {
                        NoteFields(front = front.trim(), back = back.trim())
                    }
                    repo.createNote(deckId, fields)
                    saving = false
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (saving) "Guardando…" else "Crear tarjeta")
        }
    }
}

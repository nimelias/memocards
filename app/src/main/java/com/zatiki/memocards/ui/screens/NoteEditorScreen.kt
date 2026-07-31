package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

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

        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !saving && (front.isNotBlank() || back.isNotBlank()),
            onClick = {
                saving = true
                scope.launch {
                    repo.createNote(deckId, NoteFields(front = front.trim(), back = back.trim()))
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

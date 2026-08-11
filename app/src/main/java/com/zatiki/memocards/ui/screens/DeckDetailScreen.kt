package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.CardQueue
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckBucketStats
import com.zatiki.memocards.domain.DeckSettings
import com.zatiki.memocards.domain.Note
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.ClozeFormat
import com.zatiki.memocards.ui.components.AmbientGlowBackdrop
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import kotlinx.coroutines.launch

@Composable
fun DeckDetailScreen(
    repo: MemoRepository,
    deckId: Long,
    deckName: String,
    settings: UiSettings,
    refreshKey: Int = 0,
    onReview: (queueFilter: String) -> Unit,
    onPreviewReview: (advanceDays: Int) -> Unit,
    onAddNote: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var deck by remember { mutableStateOf<Deck?>(null) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var buckets by remember { mutableStateOf(DeckBucketStats(0, 0, 0, 0)) }
    var studyDaysText by remember { mutableStateOf("") }
    var minRepText by remember { mutableStateOf("1") }

    suspend fun reload() {
        deck = repo.getDeck(deckId)
        notes = repo.listNotes(deckId)
        buckets = repo.getDeckBucketStats(deckId)
        studyDaysText = deck?.studyDays?.toString().orEmpty()
        minRepText = (deck?.minRepetitions ?: 1).toString()
    }

    LaunchedEffect(deckId, refreshKey) { reload() }
    DisposableEffect(lifecycleOwner, deckId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val left = buckets.leftToStudy
    val progress = if (buckets.total <= 0) {
        0f
    } else {
        ((buckets.total - left).toFloat() / buckets.total).coerceIn(0f, 1f)
    }
    val clozeCount = notes.count { ClozeFormat.isCloze(it.fields.front) }
    val qaCount = notes.size - clozeCount

    AmbientGlowBackdrop(theme = settings.theme, intensity = settings.glowIntensity) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Text(
                deckName,
                fontSize = scaledSp(22f),
                fontWeight = FontWeight.Bold,
                color = palette.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background.copy(alpha = 0.72f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )

            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val heroShape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(palette.surface, heroShape)
                            .padding(16.dp),
                    ) {
                        Text(
                            "${buckets.total - left}/${buckets.total} cartas al día",
                            color = palette.muted,
                            fontSize = scaledSp(13f),
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = palette.primary,
                            trackColor = palette.border,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeChip(label = "Cloze", count = clozeCount)
                            TypeChip(label = "Q&A", count = qaCount)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddNote, modifier = Modifier.weight(1f)) {
                            Text("+ Tarjeta")
                        }
                        OutlinedButton(
                            onClick = { onPreviewReview(7) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("+7 días")
                        }
                    }
                }

                item {
                    Text(
                        "Por estado",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                        fontSize = scaledSp(15f),
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BucketCard(
                            title = "Nuevas",
                            subtitle = "Por aprender",
                            count = buckets.newCount,
                            percent = buckets.percentOf(buckets.newCount),
                            enabled = buckets.newCount > 0,
                            onStudy = { onReview(CardQueue.NEW.value) },
                            modifier = Modifier.weight(1f),
                        )
                        BucketCard(
                            title = "Aprendizaje",
                            subtitle = "En curso",
                            count = buckets.learningCount,
                            percent = buckets.percentOf(buckets.learningCount),
                            enabled = buckets.learningCount > 0,
                            onStudy = { onReview(CardQueue.LEARNING.value) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    BucketCard(
                        title = "Repaso",
                        subtitle = "Pendientes hoy",
                        count = buckets.reviewDueCount,
                        percent = buckets.percentOf(buckets.reviewDueCount),
                        enabled = buckets.reviewDueCount > 0,
                        onStudy = { onReview(CardQueue.REVIEW.value) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ajustes del mazo",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                        fontSize = scaledSp(15f),
                    )
                }

                item {
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
                }

                item {
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
                        ) { Text("Guardar") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repo.resetDeck(deckId)
                                    reload()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Reiniciar") }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tarjetas",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                        fontSize = scaledSp(15f),
                    )
                }

                if (notes.isEmpty()) {
                    item {
                        Text("Sin tarjetas en este mazo.", color = palette.muted)
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        val cloze = ClozeFormat.isCloze(note.fields.front)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                                .background(palette.card, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .background(palette.primary.copy(alpha = 0.14f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    if (cloze) "Cloze" else "Q&A",
                                    color = palette.primary,
                                    fontSize = scaledSp(11f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                note.fields.front.ifBlank { "(sin frente)" },
                                color = palette.text,
                                fontSize = scaledSp(15f),
                            )
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
}

@Composable
private fun TypeChip(label: String, count: Int) {
    val palette = LocalMemoPalette.current
    Box(
        Modifier
            .background(palette.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "$label · $count",
            color = palette.primary,
            fontSize = scaledSp(12f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BucketCard(
    title: String,
    subtitle: String,
    count: Int,
    percent: Int,
    enabled: Boolean,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .background(palette.surface, shape)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .align(Alignment.CenterHorizontally)
                .background(palette.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$percent%",
                color = palette.primary,
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(14f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = palette.text, fontWeight = FontWeight.Bold, fontSize = scaledSp(15f))
        Text(subtitle, color = palette.muted, fontSize = scaledSp(12f))
        Text(
            "$count cartas",
            color = palette.primary,
            fontSize = scaledSp(12f),
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onStudy,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
        ) {
            Text("STUDY")
        }
    }
}

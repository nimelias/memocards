package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zatiki.memocards.data.CrashLog
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.data.toPostmortem
import com.zatiki.memocards.data.toUserCause
import com.zatiki.memocards.domain.Book
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSummary
import com.zatiki.memocards.domain.EstudiaBookSummary
import com.zatiki.memocards.domain.EstudiaDeckSummary
import com.zatiki.memocards.domain.EstudiaProject
import com.zatiki.memocards.domain.HomeStats
import com.zatiki.memocards.domain.SyncSettings
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.components.AmbientGlowBackdrop
import com.zatiki.memocards.ui.components.BottomBarContentGap
import com.zatiki.memocards.ui.components.memoGlass
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DeckListScreen(
    repo: MemoRepository,
    settings: UiSettings,
    onToggleTheme: () -> Unit,
    onOpenDeck: (Deck) -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    val palette = LocalMemoPalette.current
    val scope = rememberCoroutineScope()
    var decks by remember { mutableStateOf<List<DeckSummary>>(emptyList()) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var homeStats by remember { mutableStateOf(HomeStats(0, 0)) }
    var showImport by remember { mutableStateOf(false) }
    var remoteDecks by remember { mutableStateOf<List<EstudiaDeckSummary>>(emptyList()) }
    var remoteBooks by remember { mutableStateOf<List<EstudiaBookSummary>>(emptyList()) }
    var remoteProjects by remember { mutableStateOf<List<EstudiaProject>>(emptyList()) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var importLoading by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importLog by remember { mutableStateOf<List<String>>(emptyList()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    fun appendImportLog(line: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        importLog = (importLog + "[$ts] $line").takeLast(200)
        CrashLog.record(context, line)
    }

    fun copyImportLog() {
        val persisted = CrashLog.read(context)
        val body = persisted ?: importLog.joinToString("\n")
        clipboard.setText(AnnotatedString(body.ifBlank { "(log vacío)" }))
        importMessage = "Log copiado al portapapeles"
    }

    suspend fun loadCatalogFor(sync: SyncSettings) {
        selectedProjectId = sync.projectId
        if (sync.projectId == null) {
            remoteDecks = emptyList()
            remoteBooks = emptyList()
            return
        }
        remoteDecks = repo.listEstudiaDecks(sync)
        remoteBooks = repo.listEstudiaBooks(sync)
    }

    suspend fun reload() {
        decks = repo.listDeckSummaries()
        books = repo.listBooks()
        homeStats = repo.getHomeStats()
    }

    val dataVersion by repo.dataVersion.collectAsState()
    LaunchedEffect(Unit) {
        repo.ensureDemoDeckIfNeeded()
        reload()
        repo.syncEstudiaIfDue()
        reload()
    }
    LaunchedEffect(dataVersion) {
        if (dataVersion > 0) reload()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        AmbientGlowBackdrop(theme = settings.theme, intensity = settings.glowIntensity) {
            PerspectiveGrid(
                color = palette.primary.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "MEMOCARDS",
                        color = palette.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(16f),
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (settings.theme.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Tema",
                            tint = palette.primary,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                val statsShape = RoundedCornerShape(18.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .memoGlass(palette, statsShape, alpha = 0.70f)
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatBlock(
                        value = homeStats.cardsDone.toString(),
                        label = "Cards Done",
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(palette.border),
                    )
                    StatBlock(
                        value = homeStats.leftToAnswer.toString(),
                        label = "Left to Answer",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(18.dp))

                TextButton(
                    onClick = {
                        scope.launch {
                            importLoading = true
                            importMessage = null
                            appendImportLog("Inicio de carga de catálogo estudIA")
                            try {
                                val sync = repo.getSyncSettings()
                                appendImportLog(
                                    "sync url=${sync.baseUrl} project=${sync.projectId} (${sync.projectName ?: "sin asignatura"}) keyLen=${sync.apiKey.length}",
                                )
                                remoteProjects = repo.listEstudiaProjects(sync)
                                var next = sync
                                if (next.projectId != null && next.projectName.isNullOrBlank()) {
                                    val match = remoteProjects.find { it.id == next.projectId }
                                    if (match != null) {
                                        next = next.copy(projectName = match.name)
                                        repo.saveSyncSettings(next)
                                    }
                                }
                                loadCatalogFor(next)
                                if (remoteProjects.isEmpty()) {
                                    importMessage = "Conectado, pero estudIA no devolvió asignaturas"
                                    appendImportLog("Sin asignaturas en /api/projects")
                                } else if (next.projectId == null) {
                                    importMessage = "Elige la asignatura para ver barajas y libros"
                                    appendImportLog("Catálogo: ${remoteProjects.size} asignaturas, falta contexto")
                                } else {
                                    appendImportLog(
                                        "Catálogo ${next.projectName}: ${remoteDecks.size} barajas, ${remoteBooks.size} libros",
                                    )
                                }
                                showImport = true
                            } catch (e: Exception) {
                                val cause = e.toUserCause()
                                importMessage = "Error al cargar catálogo: $cause"
                                appendImportLog("Error cargando catálogo:\n${e.toPostmortem()}")
                                CrashLog.record(context, e.toPostmortem())
                            } finally {
                                importLoading = false
                            }
                        }
                    },
                    enabled = !importLoading,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    if (importLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                            color = palette.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Importar estudIA", color = palette.primary, fontSize = scaledSp(13f))
                }
                importMessage?.let {
                    Text(it, color = palette.muted, fontSize = scaledSp(12f))
                }
                if (importLog.isNotEmpty() || CrashLog.read(context) != null) {
                    TextButton(
                        onClick = { copyImportLog() },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("Copiar log de importación", color = palette.primary, fontSize = scaledSp(13f))
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = BottomBarContentGap),
                    modifier = Modifier.weight(1f),
                ) {
                    if (books.isNotEmpty()) {
                        item {
                            SectionChip(label = "LIBROS")
                        }
                        val bookSubjects = groupedSubjectKeys(books.map { it.subject })
                        val showBookSubjects = bookSubjects.size > 1 || bookSubjects.first() != "Otros"
                        bookSubjects.forEach { subject ->
                            if (showBookSubjects) {
                                item(key = "book-subject-$subject") {
                                    SubjectHeader(subject)
                                }
                            }
                            items(
                                books.filter { subjectOf(it.subject) == subject },
                                key = { "book-${it.id}" },
                            ) { book ->
                                val shape = RoundedCornerShape(14.dp)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .memoGlass(palette, shape, alpha = 0.68f, elevation = 4.dp)
                                        .clickable { onOpenBook(book.id) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        book.title,
                                        color = palette.text,
                                        fontSize = scaledSp(16f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "Leer",
                                        color = palette.primary,
                                        fontSize = scaledSp(14f),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }

                    item {
                        SectionChip(label = "DECKS")
                    }

                    if (decks.isEmpty()) {
                        item {
                            Text(
                                "Sin mazos todavía. Importa desde estudIA para comenzar.",
                                color = palette.muted,
                                fontSize = scaledSp(15f),
                            )
                        }
                    } else {
                        val deckSubjects = groupedSubjectKeys(decks.map { it.deck.subject })
                        val showDeckSubjects = deckSubjects.size > 1 || deckSubjects.first() != "Otros"
                        deckSubjects.forEach { subject ->
                            if (showDeckSubjects) {
                                item(key = "deck-subject-$subject") {
                                    SubjectHeader(subject)
                                }
                            }
                            items(
                                decks.filter { subjectOf(it.deck.subject) == subject },
                                key = { it.deck.id },
                            ) { summary ->
                                val shape = RoundedCornerShape(14.dp)
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .memoGlass(palette, shape, alpha = 0.68f, elevation = 4.dp)
                                        .clickable { onOpenDeck(summary.deck) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            summary.deck.name,
                                            color = palette.text,
                                            fontSize = scaledSp(16f),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            summary.cardCount.toString(),
                                            color = palette.primary,
                                            fontSize = scaledSp(16f),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        typeSummaryLabel(summary),
                                        color = palette.muted,
                                        fontSize = scaledSp(12f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Importar desde estudIA") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    importMessage?.let { msg ->
                        Text(msg, color = palette.muted, fontSize = scaledSp(12f))
                    }
                    if (importLog.isNotEmpty()) {
                        TextButton(
                            onClick = { copyImportLog() },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("Copiar log de importación", color = palette.primary, fontSize = scaledSp(13f))
                        }
                    }
                    Text("Asignatura", fontWeight = FontWeight.SemiBold)
                    Text(
                        "El contexto de importación es la asignatura. Mazos y libros se agrupan con ese nombre.",
                        color = palette.muted,
                        fontSize = scaledSp(12f),
                    )
                    if (remoteProjects.isEmpty()) {
                        Text(
                            "No hay asignaturas. Revisa URL y X-KEY en Ajustes.",
                            color = palette.muted,
                            fontSize = scaledSp(12f),
                        )
                    } else {
                        remoteProjects.forEach { project ->
                            FilterChip(
                                selected = selectedProjectId == project.id,
                                onClick = {
                                    scope.launch {
                                        importLoading = true
                                        try {
                                            val next = repo.getSyncSettings().copy(
                                                projectId = project.id,
                                                projectName = project.name,
                                            )
                                            repo.saveSyncSettings(next)
                                            selectedProjectId = project.id
                                            appendImportLog("Asignatura: ${project.name} (${project.id})")
                                            loadCatalogFor(next)
                                            importMessage = "${project.name}: ${remoteDecks.size} barajas, ${remoteBooks.size} libros"
                                        } catch (e: Exception) {
                                            importMessage = "Error al cargar ${project.name}: ${e.toUserCause()}"
                                            appendImportLog("Fallo catálogo ${project.id}:\n${e.toPostmortem()}")
                                            CrashLog.record(context, e.toPostmortem())
                                        } finally {
                                            importLoading = false
                                        }
                                    }
                                },
                                label = { Text(project.name) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (selectedProjectId == null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Selecciona una asignatura para listar barajas y libros.",
                            color = palette.muted,
                            fontSize = scaledSp(12f),
                        )
                    }
                    if (remoteBooks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Libros", fontWeight = FontWeight.SemiBold)
                        remoteBooks.forEach { remote ->
                            Button(
                                onClick = {
                                    scope.launch {
                                        importLoading = true
                                        appendImportLog("Importando libro ${remote.id}: ${remote.title}")
                                        try {
                                            val bookId = repo.importEstudiaBook(remote.id)
                                            appendImportLog("Libro importado OK idLocal=$bookId")
                                            showImport = false
                                            reload()
                                            onOpenBook(bookId)
                                        } catch (e: Exception) {
                                            val cause = e.toUserCause()
                                            importMessage = "Error importando libro: $cause"
                                            appendImportLog("Fallo importando libro ${remote.id}:\n${e.toPostmortem()}")
                                            CrashLog.record(context, e.toPostmortem())
                                        } finally {
                                            importLoading = false
                                        }
                                    }
                                },
                                enabled = !importLoading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(remote.title)
                            }
                        }
                    }
                    if (remoteDecks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Barajas", fontWeight = FontWeight.SemiBold)
                        remoteDecks.forEach { remote ->
                            Button(
                                onClick = {
                                    scope.launch {
                                        importLoading = true
                                        appendImportLog("Importando baraja ${remote.id}: ${remote.title}")
                                        try {
                                            val deckId = repo.importEstudiaDeck(remote.id)
                                            appendImportLog(
                                                "Baraja importada OK idLocal=$deckId remote=${remote.id} cards=${remote.cardCount}",
                                            )
                                            repo.lastImportWarnings.takeIf { it.isNotBlank() }?.let {
                                                appendImportLog(it)
                                            }
                                            showImport = false
                                            reload()
                                            decks.find { it.deck.id == deckId }?.let { onOpenDeck(it.deck) }
                                        } catch (e: Exception) {
                                            val cause = e.toUserCause()
                                            importMessage = "Error importando baraja: $cause"
                                            appendImportLog("Fallo importando baraja ${remote.id}:\n${e.toPostmortem()}")
                                            CrashLog.record(context, e.toPostmortem())
                                        } finally {
                                            importLoading = false
                                        }
                                    }
                                },
                                enabled = !importLoading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${remote.title} (${remote.cardCount} cartas)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImport = false }) { Text("Cerrar") }
            },
        )
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = palette.primary,
            fontSize = scaledSp(32f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            color = palette.muted,
            fontSize = scaledSp(13f),
        )
    }
}

private fun typeSummaryLabel(summary: DeckSummary): String {
    val parts = buildList {
        if (summary.clozeCount > 0) add("${summary.clozeCount} cloze")
        if (summary.qaCount > 0) add("${summary.qaCount} Q&A")
    }
    return if (parts.isEmpty()) "${summary.cardCount} cartas" else parts.joinToString(" · ")
}

private fun subjectOf(name: String?): String = name?.takeIf { it.isNotBlank() } ?: "Otros"

private fun groupedSubjectKeys(names: List<String?>): List<String> {
    val keys = names.map { subjectOf(it) }.distinct()
    return keys.sortedWith(compareBy<String> { if (it == "Otros") 1 else 0 }.thenBy { it.lowercase() })
}

@Composable
private fun SectionChip(label: String) {
    val palette = LocalMemoPalette.current
    Column {
        Box(
            Modifier
                .memoGlass(palette, RoundedCornerShape(50), alpha = 0.62f, elevation = 2.dp)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                label,
                color = palette.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = scaledSp(12f),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SubjectHeader(subject: String) {
    val palette = LocalMemoPalette.current
    Text(
        subject,
        color = palette.muted,
        fontWeight = FontWeight.SemiBold,
        fontSize = scaledSp(12f),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun PerspectiveGrid(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cols = 12
        val rows = 8
        val horizonY = size.height * 0.05f
        val bottomY = size.height
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        for (i in 0..cols) {
            val t = i / cols.toFloat()
            val topX = size.width * (0.35f + t * 0.3f)
            val bottomX = size.width * t
            drawLine(
                color = color,
                start = Offset(topX, horizonY),
                end = Offset(bottomX, bottomY),
                strokeWidth = 1.5f,
                pathEffect = pathEffect,
            )
        }
        for (r in 1..rows) {
            val p = (r / rows.toFloat()).let { it * it }
            val y = horizonY + (bottomY - horizonY) * p
            val inset = (1f - p) * size.width * 0.28f
            drawLine(
                color = color,
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = 1.2f,
                pathEffect = pathEffect,
            )
        }
    }
}

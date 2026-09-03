package com.zatiki.memocards.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.BookAnnotationCodec
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Book
import com.zatiki.memocards.domain.BookAnnotation
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.AnnotatedBookText
import com.zatiki.memocards.ui.BookAnnotationSpans
import com.zatiki.memocards.ui.MarkdownBlocks
import com.zatiki.memocards.ui.MdBlock
import com.zatiki.memocards.ui.MdKind
import androidx.compose.material3.Text
import com.zatiki.memocards.ui.components.memoGlass
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp

private enum class BookPane { Reading, Annotations }

@Composable
fun BookReaderScreen(
    repo: MemoRepository,
    bookId: Long,
    settings: UiSettings,
    onBack: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    var book by remember { mutableStateOf<Book?>(null) }
    var pane by remember { mutableStateOf(BookPane.Reading) }
    val listState = rememberLazyListState()
    var targetBlock by remember { mutableIntStateOf(-1) }
    var selectedAnnotation by remember { mutableStateOf<BookAnnotation?>(null) }

    LaunchedEffect(bookId) {
        book = repo.getBook(bookId)
    }

    val readingMarkdown = remember(book?.markdown) {
        BookAnnotationCodec.stripAppendix(book?.markdown.orEmpty())
    }
    val blocks = remember(readingMarkdown) {
        MarkdownBlocks.parse(readingMarkdown)
    }
    val annotations = remember(book) {
        book?.annotations.orEmpty().ifEmpty {
            BookAnnotationCodec.fromMarkdown(book?.markdown.orEmpty())
        }
    }

    val sectionTitle by remember(blocks, book?.title) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            MarkdownBlocks.headingTitleAt(blocks, firstVisible, book?.title ?: "Libro")
        }
    }

    LaunchedEffect(targetBlock, pane) {
        if (pane == BookPane.Reading && targetBlock >= 0) {
            listState.animateScrollToItem(targetBlock)
            targetBlock = -1
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = palette.text,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    book?.title ?: "Libro",
                    color = palette.muted,
                    fontSize = scaledSp(12f),
                    maxLines = 1,
                )
                Text(
                    if (pane == BookPane.Annotations) "Anotaciones" else sectionTitle,
                    color = palette.text,
                    fontSize = scaledSp(17f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = pane == BookPane.Reading,
                onClick = { pane = BookPane.Reading },
                label = { Text("Lectura") },
            )
            FilterChip(
                selected = pane == BookPane.Annotations,
                onClick = { pane = BookPane.Annotations },
                label = { Text("Anotaciones (${annotations.size})") },
            )
        }

        if (book == null) {
            Text(
                "Cargando…",
                color = palette.muted,
                modifier = Modifier.padding(24.dp),
            )
        } else if (pane == BookPane.Annotations) {
            if (annotations.isEmpty()) {
                Text(
                    "Este libro no trae anotaciones, subrayados ni fragmentos.",
                    color = palette.muted,
                    modifier = Modifier.padding(24.dp),
                    fontSize = scaledSp(15f),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(annotations, key = { "${it.quote}-${it.note}-${it.fragment}" }) { item ->
                        AnnotationCard(
                            item = item,
                            onOpen = {
                                val needle = item.quote.ifBlank { item.fragment }
                                val index = blocks.indexOfFirst { block ->
                                    needle.isNotBlank() && block.text.contains(needle.take(48), ignoreCase = true)
                                }
                                pane = BookPane.Reading
                                if (index >= 0) {
                                    targetBlock = index
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        } else if (blocks.isEmpty()) {
            Text(
                "Libro vacío",
                color = palette.muted,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(blocks, key = { index, block -> "$index-${block.kind}-${block.text.take(24)}" }) { _, block ->
                    MdBlockView(
                        block = block,
                        settings = settings,
                        annotations = annotations,
                        onAnnotationClick = { selectedAnnotation = it },
                    )
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    selectedAnnotation?.let { item ->
        AnnotationDetailDialog(
            item = item,
            onDismiss = { selectedAnnotation = null },
        )
    }
}

@Composable
private fun AnnotationDetailDialog(
    item: BookAnnotation,
    onDismiss: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                item.chapter.ifBlank { "Anotación" },
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.quote.isNotBlank()) {
                    Text(
                        item.quote,
                        color = palette.text,
                        fontSize = scaledSp(15f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (item.fragment.isNotBlank() && item.fragment != item.quote) {
                    Text(
                        item.fragment,
                        color = palette.muted,
                        fontSize = scaledSp(13f),
                    )
                }
                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        color = palette.text,
                        fontSize = scaledSp(14f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = palette.primary)
            }
        },
    )
}

@Composable
private fun AnnotationCard(
    item: BookAnnotation,
    onOpen: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    val shape = RoundedCornerShape(16.dp)
    val mark = annotationColor(item.color)
    Row(
        Modifier
            .fillMaxWidth()
            .memoGlass(palette, shape, alpha = 0.74f, elevation = 4.dp)
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(64.dp)
                .background(mark, RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (item.chapter.isNotBlank()) {
                Text(
                    item.chapter,
                    color = palette.primary,
                    fontSize = scaledSp(12f),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (item.quote.isNotBlank()) {
                Text(
                    item.quote,
                    color = palette.text,
                    fontSize = scaledSp(15f),
                    fontWeight = FontWeight.Medium,
                )
            }
            if (item.fragment.isNotBlank() && item.fragment != item.quote) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.fragment,
                    color = palette.muted,
                    fontSize = scaledSp(13f),
                )
            }
            if (item.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    item.note,
                    color = palette.text,
                    fontSize = scaledSp(14f),
                )
            }
        }
    }
}

private fun annotationColor(raw: String): Color {
    val key = raw.trim().lowercase()
    return when {
        key.contains("yellow") || key.contains("gold") || key == "mark" -> Color(0xFFFBBF24)
        key.contains("green") -> Color(0xFF22C55E)
        key.contains("blue") || key.contains("cyan") -> Color(0xFF3B82F6)
        key.contains("red") || key.contains("pink") -> Color(0xFFEF4444)
        key.contains("purple") || key.contains("violet") -> Color(0xFF8B5CF6)
        key.contains("orange") -> Color(0xFFF97316)
        else -> Color(0xFFFBBF24)
    }
}

@Composable
private fun MdBlockView(
    block: MdBlock,
    settings: UiSettings,
    annotations: List<BookAnnotation> = emptyList(),
    onAnnotationClick: (BookAnnotation) -> Unit = {},
) {
    val palette = LocalMemoPalette.current
    val line = settings.lineHeight
    val blockAnnotations = remember(block.text, annotations) {
        annotations.filter { BookAnnotationSpans.quoteRange(block.text, it.quote) != null }
    }
    val markColor: (BookAnnotation) -> Color = { annotationColor(it.color) }
    when (block.kind) {
        MdKind.H1 -> {
            Spacer(Modifier.height(12.dp))
            Text(
                block.text,
                color = palette.text,
                fontSize = scaledSp(28f),
                fontWeight = FontWeight.Bold,
                lineHeight = scaledSp(28f * line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            )
        }
        MdKind.H2 -> {
            Spacer(Modifier.height(10.dp))
            Text(
                block.text,
                color = palette.text,
                fontSize = scaledSp(22f),
                fontWeight = FontWeight.SemiBold,
                lineHeight = scaledSp(22f * line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
        MdKind.H3 -> {
            Spacer(Modifier.height(8.dp))
            Text(
                block.text,
                color = palette.primary,
                fontSize = scaledSp(18f),
                fontWeight = FontWeight.SemiBold,
                lineHeight = scaledSp(18f * line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }
        MdKind.PARAGRAPH -> {
            if (blockAnnotations.isEmpty()) {
                Text(
                    block.text,
                    color = palette.text,
                    fontSize = scaledSp(17f),
                    lineHeight = scaledSp(17f * line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            } else {
                AnnotatedBookText(
                    text = block.text,
                    annotations = blockAnnotations,
                    color = palette.text,
                    fontSize = scaledSp(17f),
                    lineHeight = scaledSp(17f * line),
                    markColor = markColor,
                    modifier = Modifier.padding(bottom = 12.dp),
                    onAnnotationClick = onAnnotationClick,
                )
            }
        }
        MdKind.LIST_ITEM -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    "•  ",
                    color = palette.primary,
                    fontSize = scaledSp(17f),
                    lineHeight = scaledSp(17f * line),
                )
                if (blockAnnotations.isEmpty()) {
                    Text(
                        block.text,
                        color = palette.text,
                        fontSize = scaledSp(17f),
                        lineHeight = scaledSp(17f * line),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    AnnotatedBookText(
                        text = block.text,
                        annotations = blockAnnotations,
                        color = palette.text,
                        fontSize = scaledSp(17f),
                        lineHeight = scaledSp(17f * line),
                        markColor = markColor,
                        modifier = Modifier.weight(1f),
                        onAnnotationClick = onAnnotationClick,
                    )
                }
            }
        }
    }
}

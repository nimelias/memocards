package com.zatiki.memocards.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.data.MemoRepository
import com.zatiki.memocards.domain.Book
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.ui.MarkdownBlocks
import com.zatiki.memocards.ui.MdBlock
import com.zatiki.memocards.ui.MdKind
import com.zatiki.memocards.ui.theme.LocalMemoPalette
import com.zatiki.memocards.ui.theme.scaledSp

@Composable
fun BookReaderScreen(
    repo: MemoRepository,
    bookId: Long,
    settings: UiSettings,
    onBack: () -> Unit,
) {
    val palette = LocalMemoPalette.current
    var book by remember { mutableStateOf<Book?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(bookId) {
        book = repo.getBook(bookId)
    }

    val blocks = remember(book?.markdown) {
        MarkdownBlocks.parse(book?.markdown.orEmpty())
    }

    val sectionTitle by remember(blocks, book?.title) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            MarkdownBlocks.headingTitleAt(blocks, firstVisible, book?.title ?: "Libro")
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
                    sectionTitle,
                    color = palette.text,
                    fontSize = scaledSp(17f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border.copy(alpha = 0.35f)),
        )

        if (book == null) {
            Text(
                "Cargando…",
                color = palette.muted,
                modifier = Modifier.padding(24.dp),
            )
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
                    MdBlockView(block = block, settings = settings)
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
private fun MdBlockView(
    block: MdBlock,
    settings: UiSettings,
) {
    val palette = LocalMemoPalette.current
    val line = settings.lineHeight
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
            Text(
                block.text,
                color = palette.text,
                fontSize = scaledSp(17f),
                lineHeight = scaledSp(17f * line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
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
                Text(
                    block.text,
                    color = palette.text,
                    fontSize = scaledSp(17f),
                    lineHeight = scaledSp(17f * line),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

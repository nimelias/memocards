package com.zatiki.memocards.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.zatiki.memocards.domain.BookAnnotation

private const val ANNOTATION_TAG = "book_annotation"

object BookAnnotationSpans {
    fun quoteRange(text: String, quote: String): IntRange? {
        if (quote.isBlank()) return null
        val idx = text.indexOf(quote, ignoreCase = true)
        if (idx >= 0) return idx until (idx + quote.length)
        val needle = quote.trim().take(64)
        if (needle.length < 8) return null
        val partial = text.indexOf(needle, ignoreCase = true)
        if (partial >= 0) return partial until (partial + needle.length)
        return null
    }

    fun build(
        text: String,
        annotations: List<BookAnnotation>,
        markColor: (BookAnnotation) -> Color,
        textColor: Color,
    ): AnnotatedString {
        val hits = annotations.mapNotNull { ann ->
            val range = quoteRange(text, ann.quote) ?: return@mapNotNull null
            Triple(range, ann, markColor(ann))
        }.sortedBy { it.first.first }

        if (hits.isEmpty()) return AnnotatedString(text)

        return buildAnnotatedString {
            var cursor = 0
            for ((range, ann, color) in hits) {
                if (range.first < cursor) continue
                if (range.first > cursor) {
                    append(text.substring(cursor, range.first))
                }
                pushStringAnnotation(ANNOTATION_TAG, ann.quote)
                withStyle(
                    SpanStyle(
                        color = textColor,
                        background = color.copy(alpha = 0.28f),
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(text.substring(range.first, range.last + 1))
                }
                pop()
                cursor = range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
}

@Composable
fun AnnotatedBookText(
    text: String,
    annotations: List<BookAnnotation>,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    markColor: (BookAnnotation) -> Color,
    modifier: Modifier = Modifier,
    onAnnotationClick: (BookAnnotation) -> Unit,
) {
    val annotated = remember(text, annotations) {
        BookAnnotationSpans.build(text, annotations, markColor, color)
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(annotated) {
                detectTapGestures { pos ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(pos)
                    val hit = annotated.getStringAnnotations(ANNOTATION_TAG, offset, offset)
                        .firstOrNull()
                        ?: return@detectTapGestures
                    val ann = annotations.firstOrNull { it.quote == hit.item }
                        ?: annotations.firstOrNull { ann ->
                            BookAnnotationSpans.quoteRange(text, ann.quote) != null
                        }
                    if (ann != null) onAnnotationClick(ann)
                }
            },
    )
}

package com.zatiki.memocards.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Cloze local:
 * - Embebido: `{{c1::respuesta}}` en el frente
 * - Hueco: frente con `[...]` y reverso = respuesta(s) (`|` separa varios)
 */
object ClozeFormat {
    private val EMBEDDED = Regex("""\{\{c\d+::((?:[^}]|\}(?!\}))+)\}\}""")
    private const val BLANK = "[...]"

    fun isCloze(front: String): Boolean =
        front.contains(BLANK) || EMBEDDED.containsMatchIn(front)

    fun prompt(front: String): String =
        EMBEDDED.replace(front) { BLANK }.ifBlank { front }

    fun revealed(
        front: String,
        back: String,
        highlight: Color,
    ): AnnotatedString {
        if (EMBEDDED.containsMatchIn(front)) {
            return buildAnnotatedString {
                var last = 0
                for (match in EMBEDDED.findAll(front)) {
                    append(front.substring(last, match.range.first))
                    withStyle(
                        SpanStyle(color = highlight, fontWeight = FontWeight.Bold),
                    ) {
                        append(match.groupValues[1])
                    }
                    last = match.range.last + 1
                }
                append(front.substring(last))
            }
        }

        if (front.contains(BLANK)) {
            val answers = back.split('|', '；', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return buildAnnotatedString {
                var remaining = front
                var answerIndex = 0
                while (remaining.contains(BLANK)) {
                    val at = remaining.indexOf(BLANK)
                    append(remaining.substring(0, at))
                    val answer = answers.getOrNull(answerIndex)
                        ?: answers.lastOrNull()
                        ?: back.ifBlank { "…" }
                    withStyle(
                        SpanStyle(color = highlight, fontWeight = FontWeight.Bold),
                    ) {
                        append(answer)
                    }
                    remaining = remaining.substring(at + BLANK.length)
                    answerIndex += 1
                }
                append(remaining)
            }
        }

        return AnnotatedString(back.ifBlank { front })
    }
}

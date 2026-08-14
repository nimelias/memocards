package com.zatiki.memocards.data

import com.zatiki.memocards.domain.BookAnnotation
import org.json.JSONArray
import org.json.JSONObject

object BookAnnotationCodec {
    fun toJson(list: List<BookAnnotation>): String {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject()
                    .put("quote", item.quote)
                    .put("note", item.note)
                    .put("fragment", item.fragment)
                    .put("color", item.color)
                    .put("chapter", item.chapter),
            )
        }
        return arr.toString()
    }

    fun fromJson(raw: String?): List<BookAnnotation> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val item = fromObject(o) ?: continue
                    add(item)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fromObject(o: JSONObject): BookAnnotation? {
        val quote = firstNonBlank(
            o,
            "quote", "text", "body", "excerpt", "highlightedText",
            "selection", "content", "underline",
        )
        val note = firstNonBlank(o, "note", "comment", "annotation", "remark", "memo").orEmpty()
        val fragment = firstNonBlank(o, "fragment", "context", "surrounding", "snippet", "passage").orEmpty()
        val color = firstNonBlank(o, "color", "markColor", "highlightColor", "tint") ?: "mark"
        val chapter = firstNonBlank(o, "chapter", "heading", "section", "title", "theme").orEmpty()
        if (quote.isNullOrBlank() && note.isBlank() && fragment.isBlank()) return null
        return BookAnnotation(
            quote = quote.orEmpty(),
            note = note,
            fragment = fragment,
            color = color,
            chapter = chapter,
        )
    }

    fun fromMarkdown(markdown: String): List<BookAnnotation> {
        val idx = markdown.indexOf("## Anotaciones")
        if (idx < 0) return emptyList()
        val section = markdown.substring(idx)
        val out = mutableListOf<BookAnnotation>()
        var pending: BookAnnotation? = null
        for (raw in section.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("> ") -> {
                    pending?.let { out += it }
                    val body = line.removePrefix("> ").trim()
                    val colorMatch = Regex("""^\[([^\]]+)]\s*(.*)$""").find(body)
                    pending = if (colorMatch != null) {
                        BookAnnotation(
                            quote = colorMatch.groupValues[2].trim(),
                            color = colorMatch.groupValues[1].trim().ifBlank { "mark" },
                        )
                    } else {
                        BookAnnotation(quote = body)
                    }
                }
                line.startsWith("Nota:", ignoreCase = true) || line.startsWith("Nota :") -> {
                    val note = line.substringAfter(':').trim()
                    pending = pending?.copy(note = note) ?: BookAnnotation(quote = "", note = note)
                }
                line.startsWith("Fragmento:", ignoreCase = true) -> {
                    val fragment = line.substringAfter(':').trim()
                    pending = pending?.copy(fragment = fragment)
                        ?: BookAnnotation(quote = "", fragment = fragment)
                }
            }
        }
        pending?.let { out += it }
        return out.filter { it.quote.isNotBlank() || it.note.isNotBlank() || it.fragment.isNotBlank() }
    }

    fun stripAppendix(markdown: String): String {
        val idx = markdown.indexOf("\n## Anotaciones")
        val cut = if (idx >= 0) markdown.substring(0, idx) else markdown
        return cut.replace(Regex("\n---\\s*$"), "").trimEnd()
    }

    private fun firstNonBlank(o: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val value = o.optString(key).trim()
            if (value.isNotBlank() && value != "null") return value
        }
        return null
    }
}

package com.zatiki.memocards.ui

enum class MdKind {
    H1,
    H2,
    H3,
    PARAGRAPH,
    LIST_ITEM,
}

data class MdBlock(
    val kind: MdKind,
    val text: String,
)

object MarkdownBlocks {
    fun parse(markdown: String): List<MdBlock> {
        val lines = markdown.replace("\r\n", "\n").split('\n')
        val out = ArrayList<MdBlock>()
        val paragraph = StringBuilder()

        fun flushParagraph() {
            val t = paragraph.toString().trim()
            if (t.isNotEmpty()) out += MdBlock(MdKind.PARAGRAPH, t)
            paragraph.clear()
        }

        for (raw in lines) {
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> flushParagraph()
                trimmed.startsWith("### ") -> {
                    flushParagraph()
                    out += MdBlock(MdKind.H3, trimmed.removePrefix("### ").trim())
                }
                trimmed.startsWith("## ") -> {
                    flushParagraph()
                    out += MdBlock(MdKind.H2, trimmed.removePrefix("## ").trim())
                }
                trimmed.startsWith("# ") -> {
                    flushParagraph()
                    out += MdBlock(MdKind.H1, trimmed.removePrefix("# ").trim())
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushParagraph()
                    out += MdBlock(MdKind.LIST_ITEM, trimmed.drop(2).trim())
                }
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    flushParagraph()
                    val text = trimmed.replaceFirst(Regex("""^\d+\.\s+"""), "")
                    out += MdBlock(MdKind.LIST_ITEM, text)
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                }
            }
        }
        flushParagraph()
        return out
    }

    fun headingTitleAt(blocks: List<MdBlock>, index: Int, fallback: String): String {
        for (i in index downTo 0) {
            val b = blocks.getOrNull(i) ?: continue
            if (b.kind == MdKind.H1 || b.kind == MdKind.H2 || b.kind == MdKind.H3) {
                return b.text
            }
        }
        return fallback
    }
}

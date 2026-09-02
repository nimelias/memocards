package com.zatiki.memocards.ui

/**
 * Opciones MCQ listas para mostrar en repaso: sin textos duplicados
 * (p. ej. la respuesta correcta repetida entre distractores).
 */
object McqOptions {
    fun forDisplay(
        options: List<String>,
        correctIndex: Int,
    ): Pair<List<String>, Int> {
        if (options.size < 2) {
            val safe = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
            return options to safe
        }
        val seen = LinkedHashSet<String>()
        val display = ArrayList<String>(options.size)
        val originalIndices = ArrayList<Int>(options.size)
        options.forEachIndexed { idx, raw ->
            val key = raw.trim().lowercase()
            if (key.isEmpty() || key in seen) return@forEachIndexed
            seen.add(key)
            display.add(raw)
            originalIndices.add(idx)
        }
        if (display.size < 2) {
            val safe = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
            return options to safe
        }
        val safeCorrect = correctIndex.coerceIn(0, options.lastIndex)
        val mapped = originalIndices.indexOf(safeCorrect).coerceAtLeast(0)
        return display to mapped
    }
}

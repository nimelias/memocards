package com.zatiki.memocards.data

import com.zatiki.memocards.domain.BookAnnotation
import com.zatiki.memocards.domain.EstudiaDeckSummary
import com.zatiki.memocards.domain.EstudiaProject
import com.zatiki.memocards.domain.EstudiaRemoteCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EstudiaApi(
    private val baseUrl: String,
    private val apiKey: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = getJson("/api/health")
            body.optBoolean("ok", false)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun probeHealth(): String = withContext(Dispatchers.IO) {
        val body = getJson("/api/health")
        if (body.optBoolean("ok", false)) {
            "Conexión correcta"
        } else {
            val snippet = body.toString().take(120)
            "El servidor respondió pero health.ok no es true ($snippet)"
        }
    }

    suspend fun listProjects(): List<EstudiaProject> = withContext(Dispatchers.IO) {
        val body = getJson("/api/projects")
        val arr = body.optJSONArray("projects") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    EstudiaProject(
                        id = o.getLong("id"),
                        name = o.optString("name", "Proyecto ${o.getLong("id")}"),
                    ),
                )
            }
        }
    }

    suspend fun listDecks(projectId: Long): List<EstudiaDeckSummary> = withContext(Dispatchers.IO) {
        val body = getJson("/api/projects/$projectId/decks")
        val arr = body.optJSONArray("decks") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    EstudiaDeckSummary(
                        id = o.getLong("id"),
                        title = o.optString("title", "Baraja"),
                        cardCount = o.optInt("cardCount", 0),
                        description = o.optString("description").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    suspend fun fetchDeckCards(remoteDeckId: Long): Pair<String, List<EstudiaRemoteCard>> =
        withContext(Dispatchers.IO) {
            val body = getJson("/api/decks/$remoteDeckId")
            val title = body.optString("title", "Baraja estudIA")
            val arr = body.optJSONArray("cards") ?: JSONArray()
            val parseErrors = mutableListOf<String>()
            val cards = buildList {
                for (i in 0 until arr.length()) {
                    try {
                        val o = arr.getJSONObject(i)
                        add(
                            EstudiaRemoteCard(
                                id = o.getLong("id"),
                                front = o.optString("front", ""),
                                back = o.optString("back", ""),
                                updatedAt = o.optLong("updatedAt", 0L),
                            ),
                        )
                    } catch (e: Exception) {
                        parseErrors += "idx=$i ${e.toUserCause(80)}"
                    }
                }
            }
            val valid = cards.filter { it.front.isNotBlank() }
            if (valid.isEmpty() && arr.length() > 0) {
                throw EstudiaApiException(
                    "Ninguna carta válida en baraja $remoteDeckId. ${parseErrors.take(4).joinToString("; ")}",
                )
            }
            title to valid
        }

    /**
     * Lista libros del proyecto. Si el bridge aún no expone el endpoint, devuelve vacío.
     */
    suspend fun listBooks(projectId: Long): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        try {
            val body = getJson("/api/projects/$projectId/books")
            val arr = body.optJSONArray("books") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optLong("id", -1L)
                    val title = o.optString("title", "").ifBlank { "Libro $id" }
                    if (id > 0) add(id to title)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchBook(remoteBookId: Long): EstudiaBookPayload? = withContext(Dispatchers.IO) {
        try {
            val body = getJson("/api/books/$remoteBookId")
            val title = body.optString("title", "Libro estudIA")
            var markdown = body.optString("markdown")
                .ifBlank { body.optString("content") }
                .ifBlank { body.optString("body") }
            if (markdown.isBlank()) {
                markdown = getText("/api/books/$remoteBookId/export?format=md")
            }
            if (markdown.isBlank()) {
                markdown = markdownFromAtoms(body)
            }
            val annotations = collectAnnotations(body)
            if (markdown.isBlank() && annotations.isEmpty()) null
            else EstudiaBookPayload(title = title, markdown = markdown, annotations = annotations)
        } catch (e: Exception) {
            throw EstudiaApiException("Libro $remoteBookId: ${e.toUserCause()}")
        }
    }

    private fun collectAnnotations(body: JSONObject): List<BookAnnotation> {
        val out = mutableListOf<BookAnnotation>()
        fun addArray(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                BookAnnotationCodec.fromObject(o)?.let { out += it }
            }
        }
        addArray(body.optJSONArray("highlights"))
        addArray(body.optJSONArray("annotations"))
        addArray(body.optJSONArray("notes"))
        addArray(body.optJSONArray("underlines"))
        addArray(body.optJSONArray("quotes"))
        addArray(body.optJSONArray("fragments"))
        body.optJSONObject("data")?.let { data ->
            addArray(data.optJSONArray("highlights"))
            addArray(data.optJSONArray("annotations"))
            addArray(data.optJSONArray("notes"))
        }
        val atoms = body.optJSONArray("renderAtoms")
        if (atoms != null) {
            for (i in 0 until atoms.length()) {
                val atom = atoms.optJSONObject(i) ?: continue
                val type = atom.optString("compositionType").lowercase()
                if (type.contains("highlight") || type.contains("annotation") ||
                    type.contains("note") || type.contains("underline") || type.contains("quote")
                ) {
                    BookAnnotationCodec.fromObject(atom)?.let { out += it }
                }
            }
        }
        return out.distinctBy { Triple(it.quote, it.note, it.fragment) }
    }

    private fun markdownFromAtoms(body: JSONObject): String {
        val title = body.optString("title", "Libro")
        val atoms = body.optJSONArray("renderAtoms") ?: return ""
        val lines = mutableListOf("# $title", "")
        val desc = body.optString("description")
        if (desc.isNotBlank()) {
            lines += desc
            lines += ""
        }
        for (i in 0 until atoms.length()) {
            val atom = atoms.optJSONObject(i) ?: continue
            val text = atom.optString("body", "").trim()
            if (text.isBlank()) continue
            when (atom.optString("compositionType")) {
                "book_theme" -> {
                    lines += "## ${text.replace(Regex("^#+\\s*"), "")}"
                    lines += ""
                }
                "book_title" -> {
                    lines += "### ${text.replace(Regex("^#+\\s*"), "")}"
                    lines += ""
                }
                else -> {
                    lines += text
                    lines += ""
                }
            }
        }
        return lines.joinToString("\n").trim()
    }

    /**
     * Envía feedback de repaso. Si [fsrs] está presente, estudIA persiste ese estado
     * en lugar de recalcular intervalos localmente.
     */
    suspend fun postReview(
        remoteCardId: Long,
        rating: String,
        elapsedMs: Long,
        fsrs: CardFsrsSnapshot? = null,
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("rating", rating)
            .put("elapsedMs", elapsedMs)
        if (fsrs != null) {
            payload
                .put("stability", fsrs.stability)
                .put("difficulty", fsrs.difficulty)
                .put("intervalDays", fsrs.intervalDays)
                .put("phase", fsrs.phase)
                .put("due", fsrs.due)
                .put("repetitions", fsrs.repetitions)
                .put("lapses", fsrs.lapses)
            if (fsrs.lastReviewAt != null) {
                payload.put("lastReviewAt", fsrs.lastReviewAt)
            } else {
                payload.put("lastReviewAt", JSONObject.NULL)
            }
        }
        postJson("/api/cards/$remoteCardId/review", payload.toString())
    }

    private fun getJson(path: String): JSONObject {
        val request = Request.Builder()
            .url(normalizeUrl(path))
            .header("X-KEY", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw EstudiaApiException(
                    "HTTP ${response.code} GET ${normalizeUrl(path)}: ${text.take(400)}",
                )
            }
            return JSONObject(text)
        }
    }

    private fun getText(path: String): String {
        val request = Request.Builder()
            .url(normalizeUrl(path))
            .header("X-KEY", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw EstudiaApiException(
                    "HTTP ${response.code} GET ${normalizeUrl(path)}: ${text.take(400)}",
                )
            }
            return text
        }
    }

    private fun postJson(path: String, body: String): JSONObject {
        val request = Request.Builder()
            .url(normalizeUrl(path))
            .header("X-KEY", apiKey)
            .post(body.toRequestBody(jsonMedia))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw EstudiaApiException(
                    "HTTP ${response.code} POST ${normalizeUrl(path)}: ${text.take(400)}",
                )
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private fun normalizeUrl(path: String): String {
        val base = baseUrl.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }
}

data class EstudiaBookPayload(
    val title: String,
    val markdown: String,
    val annotations: List<BookAnnotation> = emptyList(),
)

class EstudiaApiException(message: String) : Exception(message)

fun Throwable.toUserCause(maxLen: Int = 220): String {
    val chain = generateSequence(this as Throwable?) { it.cause }
        .map { err ->
            val msg = err.message?.trim().orEmpty()
            if (msg.isNotEmpty()) msg else err.javaClass.simpleName
        }
        .filter { it.isNotBlank() }
        .distinct()
        .take(3)
        .joinToString(" → ")
    return chain.ifBlank { "Error desconocido" }.take(maxLen)
}

fun Throwable.toPostmortem(): String {
    val stack = stackTraceToString().lineSequence().take(20).joinToString("\n")
    return buildString {
        appendLine("type=${javaClass.name}")
        appendLine("message=${message ?: "(sin mensaje)"}")
        cause?.let { appendLine("cause=${it.javaClass.name}: ${it.message}") }
        appendLine("thread=${Thread.currentThread().name}")
        append(stack)
    }.trim()
}

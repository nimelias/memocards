package com.zatiki.memocards.data

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
            val cards = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        EstudiaRemoteCard(
                            id = o.getLong("id"),
                            front = o.optString("front", ""),
                            back = o.optString("back", ""),
                            updatedAt = o.optLong("updatedAt", 0L),
                        ),
                    )
                }
            }
            title to cards.filter { it.front.isNotBlank() }
        }

    suspend fun postReview(remoteCardId: Long, rating: String, elapsedMs: Long) =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("rating", rating)
                .put("elapsedMs", elapsedMs)
                .toString()
            postJson("/api/cards/$remoteCardId/review", payload)
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
                throw EstudiaApiException("HTTP ${response.code}: $text")
            }
            return JSONObject(text)
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
                throw EstudiaApiException("HTTP ${response.code}: $text")
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

class EstudiaApiException(message: String) : Exception(message)

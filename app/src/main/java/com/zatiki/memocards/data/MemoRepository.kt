package com.zatiki.memocards.data

import com.zatiki.memocards.domain.Card
import com.zatiki.memocards.domain.CardQueue
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSettings
import com.zatiki.memocards.domain.DeckStats
import com.zatiki.memocards.domain.Note
import com.zatiki.memocards.domain.NoteFields
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.domain.Sm2
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import org.json.JSONObject

class MemoRepository(private val dao: MemoDao) {

    fun parseFields(raw: String): NoteFields {
        return try {
            val o = JSONObject(raw)
            NoteFields(
                front = o.optString("front", ""),
                back = o.optString("back", ""),
                frontImage = o.optString("frontImage").takeIf { it.isNotBlank() },
                backImage = o.optString("backImage").takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            NoteFields()
        }
    }

    fun fieldsToJson(fields: NoteFields): String {
        val o = JSONObject()
        o.put("front", fields.front)
        o.put("back", fields.back)
        if (fields.frontImage != null) o.put("frontImage", fields.frontImage)
        if (fields.backImage != null) o.put("backImage", fields.backImage)
        return o.toString()
    }

    private fun DeckEntity.toDomain() = Deck(
        id = id,
        name = name,
        parentId = parentId,
        studyDays = studyDays,
        minRepetitions = minRepetitions,
        studyStartAt = studyStartAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun NoteEntity.toDomain() = Note(
        id = id,
        deckId = deckId,
        fields = parseFields(fieldsJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun CardEntity.toDomain() = Card(
        id = id,
        noteId = noteId,
        due = due,
        interval = interval,
        easeFactor = easeFactor,
        repetitions = repetitions,
        lapses = lapses,
        queue = CardQueue.from(queue),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    suspend fun listDecks(): List<Deck> = dao.listDecks().map { it.toDomain() }

    suspend fun getDeck(id: Long): Deck? = dao.getDeck(id)?.toDomain()

    suspend fun createDeck(name: String): Deck {
        val ts = System.currentTimeMillis()
        val id = dao.insertDeck(
            DeckEntity(name = name.trim(), createdAt = ts, updatedAt = ts),
        )
        return Deck(id = id, name = name.trim(), createdAt = ts, updatedAt = ts)
    }

    suspend fun updateDeckSettings(deckId: Long, settings: DeckSettings): Deck? {
        val existing = dao.getDeck(deckId) ?: return null
        val ts = System.currentTimeMillis()
        val studyDays = settings.studyDays?.takeIf { it > 0 }
        val minRep = settings.minRepetitions.coerceAtLeast(1)
        val studyStartAt = if (studyDays != null) {
            if (existing.studyStartAt != null && existing.studyDays == studyDays) existing.studyStartAt else ts
        } else null
        dao.updateDeck(
            existing.copy(
                studyDays = studyDays,
                minRepetitions = minRep,
                studyStartAt = studyStartAt,
                updatedAt = ts,
            ),
        )
        return getDeck(deckId)
    }

    suspend fun resetDeck(deckId: Long) {
        val ts = System.currentTimeMillis()
        dao.resetDeckCards(deckId, ts, ts)
        dao.clearDeckReviewLog(deckId)
        val deck = dao.getDeck(deckId) ?: return
        if (deck.studyDays != null) {
            dao.updateDeck(deck.copy(studyStartAt = ts, updatedAt = ts))
        }
    }

    suspend fun listNotes(deckId: Long): List<Note> = dao.listNotes(deckId).map { it.toDomain() }

    suspend fun createNote(deckId: Long, fields: NoteFields): Note {
        val ts = System.currentTimeMillis()
        val noteId = dao.insertNote(
            NoteEntity(
                deckId = deckId,
                fieldsJson = fieldsToJson(fields),
                createdAt = ts,
                updatedAt = ts,
            ),
        )
        dao.insertCard(
            CardEntity(
                noteId = noteId,
                due = ts,
                createdAt = ts,
                updatedAt = ts,
            ),
        )
        return Note(id = noteId, deckId = deckId, fields = fields, createdAt = ts, updatedAt = ts)
    }

    suspend fun getDeckStats(deckId: Long): DeckStats {
        val ts = System.currentTimeMillis()
        val endOfDay = Sm2.startOfDay(ts) + MS_PER_DAY - 1
        var newCount = 0
        var dueCount = 0
        var total = 0
        for (row in dao.deckQueueDueCounts(deckId)) {
            total += row.count
            when {
                row.queue == "new" -> newCount += row.count
                row.due <= endOfDay -> dueCount += row.count
            }
        }
        return DeckStats(newCount, dueCount, total)
    }

    suspend fun getDueCards(deckId: Long, limit: Int = 50): List<CardWithNote> {
        val deck = getDeck(deckId)
        val ts = System.currentTimeMillis()
        val endOfDay = Sm2.startOfDay(ts) + MS_PER_DAY - 1
        val studyEnd = if (deck?.studyDays != null && deck.studyStartAt != null) {
            Sm2.studyEndDate(deck.studyStartAt, deck.studyDays)
        } else null
        val inWindow = if (studyEnd != null && ts <= studyEnd) 1 else 0
        val minRep = deck?.minRepetitions ?: 1
        return dao.getDueCards(deckId, endOfDay, inWindow, minRep, limit).map { row ->
            CardWithNote(
                card = Card(
                    id = row.cardId,
                    noteId = row.noteId,
                    due = row.due,
                    interval = row.interval,
                    easeFactor = row.easeFactor,
                    repetitions = row.repetitions,
                    lapses = row.lapses,
                    queue = CardQueue.from(row.queue),
                    createdAt = row.cardCreatedAt,
                    updatedAt = row.cardUpdatedAt,
                ),
                note = Note(
                    id = row.noteRowId,
                    deckId = row.deckId,
                    fields = parseFields(row.fieldsJson),
                    createdAt = row.noteCreatedAt,
                    updatedAt = row.noteUpdatedAt,
                ),
            )
        }
    }

    suspend fun reviewCard(cardId: Long, rating: ReviewRating): Card? {
        val entity = dao.getCard(cardId) ?: return null
        val card = entity.toDomain()
        val note = dao.getNote(card.noteId)
        val deck = note?.let { dao.getDeck(it.deckId)?.toDomain() }
        val result = Sm2.scheduleReview(card, rating)
        val cappedDue = if (deck != null) {
            Sm2.capDueToStudyPeriod(result.due, deck.studyStartAt, deck.studyDays)
        } else result.due
        val ts = System.currentTimeMillis()
        dao.updateCard(
            entity.copy(
                due = cappedDue,
                interval = result.interval,
                easeFactor = result.easeFactor,
                repetitions = result.repetitions,
                lapses = result.lapses,
                queue = result.queue.value,
                updatedAt = ts,
            ),
        )
        dao.insertReviewLog(
            ReviewLogEntity(
                cardId = cardId,
                rating = rating,
                intervalBefore = card.interval,
                intervalAfter = result.interval,
                reviewedAt = ts,
            ),
        )
        return card.copy(
            due = cappedDue,
            interval = result.interval,
            easeFactor = result.easeFactor,
            repetitions = result.repetitions,
            lapses = result.lapses,
            queue = result.queue,
            updatedAt = ts,
        )
    }

    suspend fun getUiSettings(): UiSettings {
        val map = dao.getUiSettingsRows().associate { it.key to it.value }
        val theme = ThemeName.from(map["ui.theme"])
        val font = map["ui.fontScale"]?.toFloatOrNull()?.coerceIn(0.9f, 1.4f) ?: 1f
        return UiSettings(theme = theme, fontScale = font)
    }

    suspend fun saveUiSettings(next: UiSettings): UiSettings {
        dao.upsertSetting(AppSettingEntity("ui.theme", next.theme.value))
        dao.upsertSetting(AppSettingEntity("ui.fontScale", next.fontScale.toString()))
        return next
    }

    companion object {
        private const val MS_PER_DAY = 86_400_000L
    }
}

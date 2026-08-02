package com.zatiki.memocards.data

import com.zatiki.memocards.domain.ArcLabelMode
import com.zatiki.memocards.domain.Card
import com.zatiki.memocards.domain.CardQueue
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSettings
import com.zatiki.memocards.domain.DeckStats
import com.zatiki.memocards.domain.Note
import com.zatiki.memocards.domain.NoteFields
import com.zatiki.memocards.domain.RatingLayout
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
        val ratingLayout = RatingLayout.from(map["ui.ratingLayout"])
        val arcLabelMode = ArcLabelMode.from(map["ui.arcLabelMode"])
        return UiSettings(
            theme = theme,
            fontScale = font,
            ratingLayout = ratingLayout,
            arcLabelMode = arcLabelMode,
        )
    }

    suspend fun saveUiSettings(next: UiSettings): UiSettings {
        dao.upsertSetting(AppSettingEntity("ui.theme", next.theme.value))
        dao.upsertSetting(AppSettingEntity("ui.fontScale", next.fontScale.toString()))
        dao.upsertSetting(AppSettingEntity("ui.ratingLayout", next.ratingLayout.value))
        dao.upsertSetting(AppSettingEntity("ui.arcLabelMode", next.arcLabelMode.value))
        return next
    }

    /** Primera instalación vacía: mazo «Trivial» con preguntas tipo trivial. */
    suspend fun ensureDemoDeckIfNeeded() {
        val settings = dao.getUiSettingsRows()
        val legacySeeded = settings.any { it.key == DEMO_SEEDED_KEY }
        val version = settings.find { it.key == DEMO_TRIVIAL_VERSION_KEY }?.value?.toIntOrNull()
            ?: if (legacySeeded) 1 else 0
        if (version >= DEMO_TRIVIAL_VERSION) return

        val decks = listDecks()
        if (decks.isEmpty()) {
            seedTrivialCards(createDeck("Trivial").id)
        } else {
            decks.find { it.name == "Trivial" }?.let { topUpTrivialCards(it.id) }
        }
        dao.upsertSetting(AppSettingEntity(DEMO_SEEDED_KEY, "1"))
        dao.upsertSetting(
            AppSettingEntity(DEMO_TRIVIAL_VERSION_KEY, DEMO_TRIVIAL_VERSION.toString()),
        )
    }

    private suspend fun seedTrivialCards(deckId: Long) {
        for ((front, back) in TRIVIAL_CARDS) {
            createNote(deckId, NoteFields(front = front, back = back))
        }
    }

    private suspend fun topUpTrivialCards(deckId: Long) {
        val existing = listNotes(deckId).map { it.fields.front }.toSet()
        for ((front, back) in TRIVIAL_CARDS) {
            if (front !in existing) {
                createNote(deckId, NoteFields(front = front, back = back))
            }
        }
    }

    companion object {
        private const val MS_PER_DAY = 86_400_000L
        private const val DEMO_SEEDED_KEY = "demo.seeded"
        private const val DEMO_TRIVIAL_VERSION_KEY = "demo.trivial.version"
        private const val DEMO_TRIVIAL_VERSION = 2

        private val TRIVIAL_CARDS = listOf(
            "¿Cuál es la capital de Francia?" to "París",
            "¿En qué continente está Egipto?" to "África",
            "¿Quién escribió Don Quijote?" to "Miguel de Cervantes",
            "¿Cuántos lados tiene un hexágono?" to "Seis",
            "¿Cuál es el planeta más cercano al Sol?" to "Mercurio",
            "¿En qué año llegó el hombre a la Luna?" to "1969",
            "¿Cuál es el río más largo del mundo?" to "El Nilo (o el Amazonas, según criterio)",
            "¿Quién pintó la Mona Lisa?" to "Leonardo da Vinci",
            "¿Cuál es el símbolo químico del oro?" to "Au",
            "¿Cuántos jugadores tiene un equipo de fútbol en el campo?" to "Once",
            "¿Cuál es la montaña más alta del mundo?" to "El Everest",
            "¿En qué país nació Mozart?" to "Austria (Salzburgo)",
            "¿Cuál es la capital de Japón?" to "Tokio",
            "¿Cuántos huesos tiene el cuerpo humano adulto?" to "206",
            "¿Quién descubrió la penicilina?" to "Alexander Fleming",
            "¿En qué océano está la isla de Madagascar?" to "Índico",
            "¿Cuál es el animal terrestre más grande?" to "El elefante africano",
            "¿Qué gas respiramos principalmente?" to "Nitrógeno (78 %); oxígeno ~21 %",
            "¿Cuántos continentes hay tradicionalmente?" to "Siete",
            "¿Quién fue el primer presidente de EE. UU.?" to "George Washington",
            "¿Cuál es la capital de Australia?" to "Canberra",
            "¿En qué año cayó el Muro de Berlín?" to "1989",
            "¿Cuántos minutos tiene una hora?" to "60",
            "¿Qué planeta es conocido como el planeta rojo?" to "Marte",
            "¿Cuál es el océano más grande?" to "El Pacífico",
            "¿Quién escribió Cien años de soledad?" to "Gabriel García Márquez",
            "¿Cuántos días tiene un año bisiesto?" to "366",
            "¿Cuál es el metal más ligero?" to "El litio",
            "¿En qué país está la Torre Eiffel?" to "Francia",
            "¿Cuántas cuerdas tiene una guitarra estándar?" to "Seis",
            "¿Quién pintó El grito?" to "Edvard Munch",
            "¿Cuál es la capital de Italia?" to "Roma",
            "¿Qué vitamina produce el sol en la piel?" to "Vitamina D",
            "¿Cuántos lados tiene un triángulo?" to "Tres",
            "¿En qué deporte se usa un birdie?" to "Bádminton",
        )
    }
}

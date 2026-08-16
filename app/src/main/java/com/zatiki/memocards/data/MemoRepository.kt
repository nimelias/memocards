package com.zatiki.memocards.data

import com.zatiki.memocards.domain.ArcLabelMode
import com.zatiki.memocards.domain.Card
import com.zatiki.memocards.domain.CardQueue
import com.zatiki.memocards.domain.CardWithNote
import com.zatiki.memocards.domain.Deck
import com.zatiki.memocards.domain.DeckSettings
import com.zatiki.memocards.domain.ActivityStats
import com.zatiki.memocards.domain.DayActivity
import com.zatiki.memocards.domain.DeckBucketStats
import com.zatiki.memocards.domain.DeckStats
import com.zatiki.memocards.domain.DeckSummary
import com.zatiki.memocards.domain.HomeStats
import com.zatiki.memocards.domain.HourActivity
import com.zatiki.memocards.domain.NoteSearchHit
import com.zatiki.memocards.domain.Note
import com.zatiki.memocards.domain.NoteFields
import com.zatiki.memocards.domain.EstudiaDeckSummary
import com.zatiki.memocards.domain.EstudiaBookSummary
import com.zatiki.memocards.domain.EstudiaProject
import com.zatiki.memocards.domain.Book
import com.zatiki.memocards.domain.BookAnnotation
import com.zatiki.memocards.domain.RatingLayout
import com.zatiki.memocards.domain.ReviewRating
import com.zatiki.memocards.fsrs.FsrsCardState
import com.zatiki.memocards.fsrs.FsrsPhase
import com.zatiki.memocards.fsrs.FsrsRating
import com.zatiki.memocards.fsrs.FsrsScheduler
import com.zatiki.memocards.fsrs.StudyPeriod
import com.zatiki.memocards.domain.SyncResult
import com.zatiki.memocards.domain.SyncSettings
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.Calendar

class MemoRepository(private val dao: MemoDao) {

    private val fsrs = FsrsScheduler()
    var lastImportWarnings: String = ""
        private set

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    private fun notifyDataChanged() {
        _dataVersion.update { it + 1 }
    }

    private fun estudiaApi(settings: SyncSettings): EstudiaApi? {
        if (settings.baseUrl.isBlank() || settings.apiKey.isBlank()) return null
        return EstudiaApi(settings.baseUrl, settings.apiKey)
    }

    fun ratingToRemote(rating: ReviewRating): String = when (rating) {
        1 -> "again"
        2 -> "hard"
        3 -> "good"
        else -> "easy"
    }

    fun parseFields(raw: String): NoteFields {
        return try {
            val o = JSONObject(raw)
            val optionsArr = o.optJSONArray("options")
            val options = buildList {
                if (optionsArr != null) {
                    for (i in 0 until optionsArr.length()) {
                        val v = optionsArr.optString(i, "").trim()
                        if (v.isNotEmpty()) add(v)
                    }
                }
            }
            val type = o.optString("type", "basic").ifBlank { "basic" }
            NoteFields(
                front = o.optString("front", ""),
                back = o.optString("back", ""),
                frontImage = o.optString("frontImage").takeIf { it.isNotBlank() },
                backImage = o.optString("backImage").takeIf { it.isNotBlank() },
                type = if (type == "mcq" || options.size >= 2) "mcq" else "basic",
                options = options,
                correctIndex = o.optInt("correctIndex", 0).coerceAtLeast(0),
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
        if (fields.isMcq) {
            o.put("type", "mcq")
            val arr = org.json.JSONArray()
            fields.options.forEach { arr.put(it) }
            o.put("options", arr)
            o.put(
                "correctIndex",
                fields.correctIndex.coerceIn(0, (fields.options.size - 1).coerceAtLeast(0)),
            )
        } else {
            o.put("type", "basic")
        }
        return o.toString()
    }

    private fun DeckEntity.toDomain() = Deck(
        id = id,
        name = name,
        parentId = parentId,
        studyDays = studyDays,
        minRepetitions = minRepetitions,
        studyStartAt = studyStartAt,
        remoteDeckId = remoteDeckId,
        source = source,
        subject = subject,
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
        stability = stability,
        difficulty = difficulty,
        intervalDays = intervalDays,
        phase = FsrsPhase.from(phase),
        lastReviewAt = lastReviewAt,
        repetitions = repetitions,
        lapses = lapses,
        queue = CardQueue.from(queue),
        remoteCardId = remoteCardId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Card.toFsrsState(): FsrsCardState {
        val resolvedPhase = when (phase) {
            FsrsPhase.Added -> when (queue) {
                CardQueue.REVIEW -> FsrsPhase.Review
                CardQueue.LEARNING -> FsrsPhase.ReLearning
                else -> FsrsPhase.Added
            }
            else -> phase
        }
        return FsrsCardState(
            stability = stability,
            difficulty = difficulty,
            intervalDays = intervalDays,
            phase = resolvedPhase,
            lastReviewAt = lastReviewAt,
            repetitions = repetitions,
            lapses = lapses,
        )
    }

    suspend fun listDecks(): List<Deck> = dao.listDecks().map { it.toDomain() }

    suspend fun getDeck(id: Long): Deck? = dao.getDeck(id)?.toDomain()

    suspend fun createDeck(name: String): Deck {
        val ts = System.currentTimeMillis()
        val id = dao.insertDeck(
            DeckEntity(name = name.trim(), createdAt = ts, updatedAt = ts),
        )
        notifyDataChanged()
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
        val deck = dao.getDeck(deckId)
        if (deck != null && deck.studyDays != null) {
            dao.updateDeck(deck.copy(studyStartAt = ts, updatedAt = ts))
        }
        notifyDataChanged()
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
        notifyDataChanged()
        return Note(id = noteId, deckId = deckId, fields = fields, createdAt = ts, updatedAt = ts)
    }

    suspend fun getDeckStats(deckId: Long): DeckStats {
        val ts = System.currentTimeMillis()
        val endOfDay = StudyPeriod.startOfDay(ts) + MS_PER_DAY - 1
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

    suspend fun getDeckBucketStats(deckId: Long): DeckBucketStats {
        dao.promoteReviewedNewCardsToLearning()
        val ts = System.currentTimeMillis()
        val dayStart = StudyPeriod.startOfDay(ts)
        val endOfDay = dayStart + MS_PER_DAY - 1
        var total = 0
        var newCount = 0
        var learningCount = 0
        var reviewDueCount = 0
        for (row in dao.deckQueueDueCounts(deckId)) {
            total += row.count
            when (row.queue) {
                "new" -> newCount += row.count
                "learning" -> learningCount += row.count
                "review" -> if (row.due <= endOfDay) reviewDueCount += row.count
            }
        }
        return DeckBucketStats(
            total = total,
            newCount = newCount,
            learningCount = learningCount,
            reviewDueCount = reviewDueCount,
            studiedToday = dao.countReviewsBetweenForDeck(deckId, dayStart, endOfDay),
        )
    }

    /** Stats del home: reseñas de hoy + cartas pendientes (new + due). */
    suspend fun getHomeStats(): HomeStats {
        dao.promoteReviewedNewCardsToLearning()
        val ts = System.currentTimeMillis()
        val dayStart = StudyPeriod.startOfDay(ts)
        val endOfDay = dayStart + MS_PER_DAY - 1
        val cardsDone = dao.countReviewsBetween(dayStart, endOfDay)
        var left = 0
        for (row in dao.allQueueDueCounts()) {
            when {
                row.queue == "new" -> left += row.count
                row.due <= endOfDay -> left += row.count
            }
        }
        return HomeStats(cardsDone = cardsDone, leftToAnswer = left)
    }

    suspend fun listDeckSummaries(): List<DeckSummary> {
        val counts = dao.cardCountsByDeck().associate { it.deckId to it.count }
        val typeCounts = mutableMapOf<Long, Triple<Int, Int, Int>>()
        for (row in dao.listNoteTypeRows()) {
            val (cloze, qa, mcq) = typeCounts[row.deckId] ?: Triple(0, 0, 0)
            val fields = parseFields(row.fieldsJson)
            when {
                fields.isMcq -> typeCounts[row.deckId] = Triple(cloze, qa, mcq + 1)
                isClozeFront(fields.front) -> typeCounts[row.deckId] = Triple(cloze + 1, qa, mcq)
                else -> typeCounts[row.deckId] = Triple(cloze, qa + 1, mcq)
            }
        }
        return listDecks().map { deck ->
            val types = typeCounts[deck.id] ?: Triple(0, 0, 0)
            DeckSummary(
                deck = deck,
                cardCount = counts[deck.id] ?: 0,
                clozeCount = types.first,
                qaCount = types.second,
                mcqCount = types.third,
            )
        }
    }

    suspend fun getActivityStats(heatmapBuckets: Int = 35, deckId: Long? = null): ActivityStats {
        dao.promoteReviewedNewCardsToLearning()
        val ts = System.currentTimeMillis()
        val dayStart = StudyPeriod.startOfDay(ts)
        val endOfDay = dayStart + MS_PER_DAY - 1
        val since = dayStart - (heatmapBuckets - 1L) * MS_PER_DAY
        val stamps = if (deckId != null) {
            dao.listReviewsSinceForDeck(deckId, since)
        } else {
            dao.listReviewsSince(since)
        }

        var cardsToday = 0
        var elapsedToday = 0L
        val byHour = IntArray(24)
        val ratingByHour = IntArray(24)
        val ratingBucketsByHour = Array(24) { IntArray(4) }
        val cal = Calendar.getInstance()
        for (stamp in stamps) {
            val normalized = stamp.rating.coerceIn(1, 4)
            if (stamp.reviewedAt in dayStart..endOfDay) {
                cardsToday += 1
                elapsedToday += stamp.elapsedMs.coerceAtLeast(0L)
                cal.timeInMillis = stamp.reviewedAt
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                byHour[hour] += 1
                ratingByHour[hour] += normalized
                ratingBucketsByHour[hour][normalized - 1] += 1
            }
        }

        val allStamps = if (deckId != null) dao.listAllReviewsForDeck(deckId) else dao.listReviewsSince(0L)
        val windowEnd = ts
        val windowStart = if (allStamps.isEmpty()) {
            dayStart - (heatmapBuckets - 1L) * MS_PER_DAY
        } else {
            val minTs = allStamps.minOf { it.reviewedAt }
            val span = (windowEnd - minTs).coerceAtLeast(1L)
            val bucketSize = kotlin.math.max(60L * 60L * 1000L, (span + heatmapBuckets - 1L) / heatmapBuckets)
            windowEnd - bucketSize * heatmapBuckets
        }
        val bucketSizeMs = kotlin.math.max(1L, (windowEnd - windowStart) / heatmapBuckets)
        val adaptiveCount = IntArray(heatmapBuckets)
        val adaptiveRatingSum = IntArray(heatmapBuckets)
        val adaptiveRatingBuckets = Array(heatmapBuckets) { IntArray(4) }
        for (stamp in allStamps) {
            if (stamp.reviewedAt < windowStart || stamp.reviewedAt > windowEnd) continue
            val idx =
                ((stamp.reviewedAt - windowStart) / bucketSizeMs)
                    .toInt()
                    .coerceIn(0, heatmapBuckets - 1)
            val normalized = stamp.rating.coerceIn(1, 4)
            adaptiveCount[idx] += 1
            adaptiveRatingSum[idx] += normalized
            adaptiveRatingBuckets[idx][normalized - 1] += 1
        }

        val heatmap = (0 until heatmapBuckets).map { index ->
            val d = windowStart + index * bucketSizeMs
            DayActivity(
                dayStart = d,
                reviewCount = adaptiveCount[index],
                ratingSum = adaptiveRatingSum[index],
                ratingBuckets = adaptiveRatingBuckets[index].toList(),
            )
        }
        val hourlyToday = (0 until 24).map { hour ->
            HourActivity(
                hour = hour,
                reviewCount = byHour[hour],
                ratingSum = ratingByHour[hour],
                ratingBuckets = ratingBucketsByHour[hour].toList(),
            )
        }

        var newCount = 0
        var learningCount = 0
        var reviewCount = 0
        val queueRows = if (deckId != null) dao.queueCountsForDeck(deckId) else dao.queueCounts()
        for (row in queueRows) {
            when (row.queue) {
                "new" -> newCount = row.count
                "learning" -> learningCount = row.count
                "review" -> reviewCount = row.count
            }
        }

        return ActivityStats(
            cardsStudiedToday = cardsToday,
            elapsedMsToday = elapsedToday,
            newCount = newCount,
            learningCount = learningCount,
            reviewCount = reviewCount,
            heatmap = heatmap,
            hourlyToday = hourlyToday,
        )
    }

    /** Último mazo estudiado por actividad de review_log. */
    suspend fun getLastReviewedDeckId(): Long? = dao.lastReviewedDeck()?.deckId

    suspend fun searchDecks(query: String): List<Deck> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return dao.searchDecks(q).map { it.toDomain() }
    }

    suspend fun searchNotes(query: String): List<NoteSearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return dao.searchNotes(q).map { row ->
            val fields = parseFields(row.fieldsJson)
            NoteSearchHit(
                noteId = row.id,
                deckId = row.deckId,
                deckName = row.deckName,
                frontPreview = fields.front.ifBlank { fields.back }.take(80),
            )
        }
    }

    suspend fun getDueCards(
        deckId: Long,
        limit: Int = 50,
        advanceDays: Int = 0,
        queueFilter: String? = null,
    ): List<CardWithNote> {
        val deck = getDeck(deckId)
        val ts = System.currentTimeMillis()
        val endOfDay = StudyPeriod.startOfDay(ts) + MS_PER_DAY - 1 + advanceDays * MS_PER_DAY
        val studyEnd = if (deck?.studyDays != null && deck.studyStartAt != null) {
            StudyPeriod.studyEndDate(deck.studyStartAt, deck.studyDays)
        } else null
        val inWindow = if (studyEnd != null && ts <= studyEnd) 1 else 0
        val minRep = deck?.minRepetitions ?: 1
        val rows = dao.getDueCards(deckId, endOfDay, inWindow, minRep, limit).map { row ->
            CardWithNote(
                card = Card(
                    id = row.cardId,
                    noteId = row.noteId,
                    due = row.due,
                    stability = row.stability,
                    difficulty = row.difficulty,
                    intervalDays = row.intervalDays,
                    phase = FsrsPhase.from(row.phase),
                    lastReviewAt = row.lastReviewAt,
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
        val filter = queueFilter?.takeIf { it.isNotBlank() && it != "all" } ?: return rows
        return rows.filter { it.card.queue.value == filter }
    }

    suspend fun reviewCard(
        cardId: Long,
        rating: ReviewRating,
        elapsedMs: Long = 0L,
    ): Card? {
        val entity = dao.getCard(cardId) ?: return null
        val card = entity.toDomain()
        val note = dao.getNote(card.noteId)
        val deck = note?.let { dao.getDeck(it.deckId)?.toDomain() }
        val result = fsrs.review(card.toFsrsState(), FsrsRating.from(rating))
        val cappedDue = if (deck != null) {
            StudyPeriod.capDueToStudyPeriod(result.due, deck.studyStartAt, deck.studyDays)
        } else result.due
        val queue = CardQueue.fromPhase(result.phase)
        val ts = System.currentTimeMillis()
        dao.updateCard(
            entity.copy(
                due = cappedDue,
                stability = result.stability,
                difficulty = result.difficulty,
                intervalDays = result.intervalDays,
                phase = result.phase.value,
                lastReviewAt = result.lastReviewAt,
                repetitions = result.repetitions,
                lapses = result.lapses,
                queue = queue.value,
                updatedAt = ts,
            ),
        )
        dao.insertReviewLog(
            ReviewLogEntity(
                cardId = cardId,
                rating = rating,
                intervalBefore = card.intervalDays.toDouble(),
                intervalAfter = result.intervalDays.toDouble(),
                elapsedMs = elapsedMs,
                reviewedAt = ts,
            ),
        )
        notifyDataChanged()
        val fsrsSnapshot = CardFsrsSnapshot(
            due = cappedDue,
            stability = result.stability,
            difficulty = result.difficulty,
            intervalDays = result.intervalDays,
            phase = result.phase.value,
            lastReviewAt = result.lastReviewAt,
            repetitions = result.repetitions,
            lapses = result.lapses,
        )
        entity.remoteCardId?.let { remoteId ->
            pushReview(remoteId, rating, elapsedMs, fsrsSnapshot)
        }
        return card.copy(
            due = cappedDue,
            stability = result.stability,
            difficulty = result.difficulty,
            intervalDays = result.intervalDays,
            phase = result.phase,
            lastReviewAt = result.lastReviewAt,
            repetitions = result.repetitions,
            lapses = result.lapses,
            queue = queue,
            updatedAt = ts,
        )
    }

    private suspend fun pushReview(
        remoteCardId: Long,
        rating: ReviewRating,
        elapsedMs: Long,
        fsrs: CardFsrsSnapshot,
    ) {
        val settings = getSyncSettings()
        val api = estudiaApi(settings) ?: run {
            queuePendingReview(remoteCardId, rating, elapsedMs, fsrs)
            return
        }
        try {
            api.postReview(remoteCardId, ratingToRemote(rating), elapsedMs, fsrs)
        } catch (_: Exception) {
            queuePendingReview(remoteCardId, rating, elapsedMs, fsrs)
        }
    }

    private suspend fun queuePendingReview(
        remoteCardId: Long,
        rating: ReviewRating,
        elapsedMs: Long,
        fsrs: CardFsrsSnapshot,
    ) {
        dao.insertPendingReview(
            PendingReviewEntity(
                remoteCardId = remoteCardId,
                rating = ratingToRemote(rating),
                elapsedMs = elapsedMs,
                due = fsrs.due,
                stability = fsrs.stability,
                difficulty = fsrs.difficulty,
                intervalDays = fsrs.intervalDays,
                phase = fsrs.phase,
                lastReviewAt = fsrs.lastReviewAt,
                repetitions = fsrs.repetitions,
                lapses = fsrs.lapses,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Nº de calificaciones históricas del mazo (para ayudas UX). */
    suspend fun countDeckReviews(deckId: Long): Int = dao.countReviewsForDeck(deckId)

    suspend fun getUiSettings(): UiSettings {
        val map = dao.getUiSettingsRows().associate { it.key to it.value }
        val theme = ThemeName.from(map["ui.theme"])
        val font = map["ui.fontScale"]?.toFloatOrNull()?.coerceIn(0.9f, 1.4f) ?: 1f
        val lineHeight = map["ui.lineHeight"]?.toFloatOrNull()?.coerceIn(1.15f, 2.0f) ?: 1.45f
        val ratingLayout = RatingLayout.from(map["ui.ratingLayout"])
        val arcLabelMode = ArcLabelMode.from(map["ui.arcLabelMode"])
        val glowIntensity = map["ui.glowIntensity"]?.toFloatOrNull()?.coerceIn(0f, 2.0f) ?: 1f
        return UiSettings(
            theme = theme,
            fontScale = font,
            lineHeight = lineHeight,
            ratingLayout = ratingLayout,
            arcLabelMode = arcLabelMode,
            glowIntensity = glowIntensity,
        )
    }

    suspend fun saveUiSettings(next: UiSettings): UiSettings {
        dao.upsertSetting(AppSettingEntity("ui.theme", next.theme.value))
        dao.upsertSetting(AppSettingEntity("ui.fontScale", next.fontScale.toString()))
        dao.upsertSetting(AppSettingEntity("ui.lineHeight", next.lineHeight.toString()))
        dao.upsertSetting(AppSettingEntity("ui.ratingLayout", next.ratingLayout.value))
        dao.upsertSetting(AppSettingEntity("ui.arcLabelMode", next.arcLabelMode.value))
        dao.upsertSetting(AppSettingEntity("ui.glowIntensity", next.glowIntensity.toString()))
        return next
    }

    suspend fun getSyncSettings(): SyncSettings {
        val map = dao.getSyncSettingsRows().associate { it.key to it.value }
        val defaults = SyncSettings()
        val storedUrl = map[SYNC_BASE_URL_KEY]
        // Migrar instalaciones que aún apuntan al bridge LAN :30004.
        val baseUrl = when {
            storedUrl.isNullOrBlank() -> defaults.baseUrl
            storedUrl.trimEnd('/') == LEGACY_ESTUDIA_BRIDGE_URL -> defaults.baseUrl
            else -> storedUrl
        }
        if (storedUrl != null && storedUrl.trimEnd('/') == LEGACY_ESTUDIA_BRIDGE_URL) {
            dao.upsertSetting(AppSettingEntity(SYNC_BASE_URL_KEY, defaults.baseUrl))
        }
        return SyncSettings(
            baseUrl = baseUrl,
            apiKey = map[SYNC_API_KEY_KEY] ?: defaults.apiKey,
            projectId = map[SYNC_PROJECT_ID_KEY]?.toLongOrNull(),
            projectName = map[SYNC_PROJECT_NAME_KEY]?.takeIf { it.isNotBlank() },
            autoSyncEnabled = map[SYNC_AUTO_KEY] == "1",
            autoSyncIntervalMinutes = map[SYNC_INTERVAL_KEY]?.toIntOrNull()?.coerceIn(5, 24 * 60) ?: 30,
            lastSyncAt = map[SYNC_LAST_AT_KEY]?.toLongOrNull() ?: 0L,
        )
    }

    suspend fun saveSyncSettings(next: SyncSettings): SyncSettings {
        dao.upsertSetting(AppSettingEntity(SYNC_BASE_URL_KEY, next.baseUrl.trim()))
        dao.upsertSetting(AppSettingEntity(SYNC_API_KEY_KEY, next.apiKey))
        dao.upsertSetting(
            AppSettingEntity(
                SYNC_PROJECT_ID_KEY,
                next.projectId?.toString().orEmpty(),
            ),
        )
        dao.upsertSetting(AppSettingEntity(SYNC_PROJECT_NAME_KEY, next.projectName.orEmpty()))
        dao.upsertSetting(AppSettingEntity(SYNC_AUTO_KEY, if (next.autoSyncEnabled) "1" else "0"))
        dao.upsertSetting(AppSettingEntity(SYNC_INTERVAL_KEY, next.autoSyncIntervalMinutes.toString()))
        dao.upsertSetting(AppSettingEntity(SYNC_LAST_AT_KEY, next.lastSyncAt.toString()))
        return next
    }

    suspend fun testEstudiaConnectionMessage(settings: SyncSettings): String {
        val api = estudiaApi(settings) ?: return "Configura URL y X-KEY"
        return try {
            api.probeHealth()
        } catch (e: Exception) {
            e.toUserCause()
        }
    }

    suspend fun listEstudiaProjects(settings: SyncSettings): List<EstudiaProject> {
        val api = estudiaApi(settings) ?: throw IllegalStateException("Configura URL y X-KEY")
        return api.listProjects()
    }

    suspend fun listEstudiaDecks(settings: SyncSettings): List<EstudiaDeckSummary> {
        val projectId = settings.projectId ?: return emptyList()
        val api = estudiaApi(settings) ?: return emptyList()
        return api.listDecks(projectId)
    }

    suspend fun listEstudiaBooks(settings: SyncSettings): List<EstudiaBookSummary> {
        val projectId = settings.projectId ?: return emptyList()
        val api = estudiaApi(settings) ?: return emptyList()
        return api.listBooks(projectId).map { (id, title) -> EstudiaBookSummary(id, title) }
    }

    private fun BookEntity.toDomain() = Book(
        id = id,
        title = title,
        markdown = markdown,
        remoteBookId = remoteBookId,
        source = source,
        subject = subject,
        annotations = BookAnnotationCodec.fromJson(annotationsJson).ifEmpty {
            BookAnnotationCodec.fromMarkdown(markdown)
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    suspend fun listBooks(): List<Book> = dao.listBooks().map { it.toDomain() }

    suspend fun getBook(id: Long): Book? = dao.getBook(id)?.toDomain()

    suspend fun upsertLocalBook(
        title: String,
        markdown: String,
        remoteBookId: Long? = null,
        source: String? = null,
        subject: String? = null,
        annotations: List<BookAnnotation> = emptyList(),
    ): Long {
        val ts = System.currentTimeMillis()
        val annotationsJson = BookAnnotationCodec.toJson(annotations)
        val id = if (remoteBookId != null) {
            val existing = dao.getBookByRemoteId(remoteBookId)
            if (existing != null) {
                dao.updateBook(
                    existing.copy(
                        title = title,
                        markdown = markdown,
                        updatedAt = ts,
                        source = source ?: existing.source,
                        subject = subject ?: existing.subject,
                        annotationsJson = annotationsJson,
                    ),
                )
                existing.id
            } else {
                null
            }
        } else {
            null
        } ?: dao.insertBook(
            BookEntity(
                title = title,
                markdown = markdown,
                remoteBookId = remoteBookId,
                source = source,
                subject = subject,
                annotationsJson = annotationsJson,
                createdAt = ts,
                updatedAt = ts,
            ),
        )
        notifyDataChanged()
        return id
    }

    suspend fun importEstudiaBook(remoteBookId: Long): Long {
        val settings = getSyncSettings()
        val api = estudiaApi(settings) ?: throw EstudiaApiException("Configura la conexión con estudIA")
        val fetched = api.fetchBook(remoteBookId)
            ?: throw EstudiaApiException("Libro no disponible en estudIA")
        return upsertLocalBook(
            title = fetched.title,
            markdown = fetched.markdown,
            remoteBookId = remoteBookId,
            source = SOURCE_ESTUDIA,
            subject = settings.projectName,
            annotations = fetched.annotations.ifEmpty {
                BookAnnotationCodec.fromMarkdown(fetched.markdown)
            },
        )
    }

    suspend fun importEstudiaDeck(remoteDeckId: Long): Long {
        val settings = getSyncSettings()
        val api = estudiaApi(settings) ?: throw EstudiaApiException("Configura la conexión con estudIA")
        val (title, remoteCards) = api.fetchDeckCards(remoteDeckId)
        val ts = System.currentTimeMillis()
        val subject = settings.projectName
        val existing = dao.getDeckByRemoteId(remoteDeckId)
        val deckId = if (existing != null) {
            dao.updateDeck(
                existing.copy(
                    name = title,
                    updatedAt = ts,
                    subject = subject ?: existing.subject,
                ),
            )
            existing.id
        } else {
            dao.insertDeck(
                DeckEntity(
                    name = title,
                    remoteDeckId = remoteDeckId,
                    source = SOURCE_ESTUDIA,
                    subject = subject,
                    createdAt = ts,
                    updatedAt = ts,
                ),
            )
        }
        var updated = 0
        val failures = mutableListOf<String>()
        for (remote in remoteCards) {
            try {
                val fields = NoteFields(
                    front = remote.front,
                    back = remote.back,
                    type = if (remote.cardType == "mcq" || remote.options.size >= 2) "mcq" else "basic",
                    options = remote.options,
                    correctIndex = remote.correctIndex,
                )
                val localCard = dao.getCardByRemoteId(remote.id)
                if (localCard != null) {
                    val note = dao.getNote(localCard.noteId) ?: continue
                    dao.updateNote(
                        note.copy(
                            fieldsJson = fieldsToJson(fields),
                            updatedAt = ts,
                        ),
                    )
                    updated += 1
                } else {
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
                            remoteCardId = remote.id,
                            createdAt = ts,
                            updatedAt = ts,
                        ),
                    )
                    updated += 1
                }
            } catch (e: Exception) {
                failures += "carta ${remote.id}: ${e.toUserCause(80)}"
            }
        }
        if (updated == 0 && remoteCards.isNotEmpty()) {
            lastImportWarnings = failures.joinToString(" | ")
            throw EstudiaApiException(
                "No se importó ninguna carta. ${failures.take(3).joinToString("; ")}",
            )
        }
        lastImportWarnings = if (failures.isNotEmpty()) {
            "Avisos (${failures.size}/${remoteCards.size}): ${failures.joinToString(" | ")}"
        } else {
            "cartas escritas=$updated de ${remoteCards.size}"
        }
        saveSyncSettings(getSyncSettings().copy(lastSyncAt = ts))
        notifyDataChanged()
        return deckId
    }

    suspend fun syncEstudiaIfDue(): SyncResult {
        val settings = getSyncSettings()
        if (!settings.autoSyncEnabled || settings.projectId == null) {
            return SyncResult(message = "Sincronización automática desactivada")
        }
        val intervalMs = settings.autoSyncIntervalMinutes * 60_000L
        val now = System.currentTimeMillis()
        if (settings.lastSyncAt > 0 && now - settings.lastSyncAt < intervalMs) {
            return SyncResult(message = "Sincronización reciente")
        }
        return syncAllEstudiaDecks(settings)
    }

    suspend fun syncAllEstudiaDecks(settings: SyncSettings): SyncResult {
        val api = estudiaApi(settings) ?: return SyncResult(message = "Sin conexión configurada")
        val projectId = settings.projectId ?: return SyncResult(message = "Sin proyecto seleccionado")
        var imported = 0
        var updatedCards = 0
        for (summary in api.listDecks(projectId)) {
            val before = dao.getDeckByRemoteId(summary.id)?.id
            val deckId = importEstudiaDeck(summary.id)
            if (before == null) imported += 1
            updatedCards += summary.cardCount
            if (deckId <= 0) continue
        }
        val pushed = flushPendingReviews(settings)
        val ts = System.currentTimeMillis()
        saveSyncSettings(settings.copy(lastSyncAt = ts))
        return SyncResult(
            importedDecks = imported,
            updatedCards = updatedCards,
            pushedReviews = pushed,
            message = "Sincronizado",
        )
    }

    suspend fun flushPendingReviews(settings: SyncSettings): Int {
        val api = estudiaApi(settings) ?: return 0
        var pushed = 0
        for (pending in dao.listPendingReviews()) {
            try {
                val fsrs = CardFsrsSnapshot(
                    due = pending.due,
                    stability = pending.stability,
                    difficulty = pending.difficulty,
                    intervalDays = pending.intervalDays,
                    phase = pending.phase,
                    lastReviewAt = pending.lastReviewAt,
                    repetitions = pending.repetitions,
                    lapses = pending.lapses,
                )
                api.postReview(pending.remoteCardId, pending.rating, pending.elapsedMs, fsrs)
                dao.deletePendingReview(pending.id)
                pushed += 1
            } catch (_: Exception) {
                break
            }
        }
        return pushed
    }

    /** Demostración local: Trivial + mazos de ejemplo (cloze, Q&A largo). */
    suspend fun ensureDemoDeckIfNeeded() {
        val settings = dao.getUiSettingsRows()
        val legacySeeded = settings.any { it.key == DEMO_SEEDED_KEY }
        val version = settings.find { it.key == DEMO_TRIVIAL_VERSION_KEY }?.value?.toIntOrNull()
            ?: if (legacySeeded) 1 else 0
        if (version >= DEMO_CONTENT_VERSION) return

        val decks = listDecks()
        if (decks.isEmpty() || version < 2) {
            // Primera instalación o upgrade temprano: asegurar Trivial completo.
            ensureNamedDeckCards("Trivial", TRIVIAL_CARDS)
        } else {
            decks.find { it.name == "Trivial" }?.let { topUpDeckCards(it.id, TRIVIAL_CARDS) }
                ?: ensureNamedDeckCards("Trivial", TRIVIAL_CARDS)
        }

        if (version < 3) {
            ensureNamedDeckCards("Cloze — Biología", CLOZE_BIO_CARDS)
            ensureNamedDeckCards("Q&A — Historia", HISTORY_QA_CARDS)
        }

        if (version < 5) {
            ensureDemoBookIfNeeded(force = true)
            val demoNames = setOf("Trivial", "Cloze — Biología", "Q&A — Historia")
            val ts = System.currentTimeMillis()
            for (deck in dao.listDecks()) {
                if (deck.name in demoNames && deck.subject.isNullOrBlank()) {
                    dao.updateDeck(deck.copy(subject = "Demo", updatedAt = ts))
                }
            }
        }

        dao.upsertSetting(AppSettingEntity(DEMO_SEEDED_KEY, "1"))
        dao.upsertSetting(
            AppSettingEntity(DEMO_TRIVIAL_VERSION_KEY, DEMO_CONTENT_VERSION.toString()),
        )
        notifyDataChanged()
    }

    private suspend fun ensureDemoBookIfNeeded(force: Boolean = false) {
        val title = "MemoCards — Guía de lectura"
        val existing = dao.listBooks().find { it.title == title }
        if (existing != null && !force) return
        if (existing != null) {
            val ts = System.currentTimeMillis()
            dao.updateBook(
                existing.copy(
                    markdown = DEMO_BOOK_MARKDOWN,
                    source = existing.source ?: "demo",
                    subject = existing.subject ?: "Demo",
                    annotationsJson = BookAnnotationCodec.toJson(DEMO_BOOK_ANNOTATIONS),
                    updatedAt = ts,
                ),
            )
            notifyDataChanged()
            return
        }
        upsertLocalBook(
            title = title,
            markdown = DEMO_BOOK_MARKDOWN,
            source = "demo",
            subject = "Demo",
            annotations = DEMO_BOOK_ANNOTATIONS,
        )
    }

    private suspend fun ensureNamedDeckCards(name: String, cards: List<Pair<String, String>>) {
        val existing = listDecks().find { it.name == name }
        if (existing == null) {
            seedDeckCards(createDeck(name).id, cards)
        } else {
            topUpDeckCards(existing.id, cards)
        }
    }

    private suspend fun seedDeckCards(deckId: Long, cards: List<Pair<String, String>>) {
        for ((front, back) in cards) {
            createNote(deckId, NoteFields(front = front, back = back))
        }
    }

    private suspend fun topUpDeckCards(deckId: Long, cards: List<Pair<String, String>>) {
        val existing = listNotes(deckId).map { it.fields.front }.toSet()
        for ((front, back) in cards) {
            if (front !in existing) {
                createNote(deckId, NoteFields(front = front, back = back))
            }
        }
    }

    companion object {
        private const val MS_PER_DAY = 86_400_000L
        private val CLOZE_EMBEDDED = Regex("""\{\{c\d+::""")

        private fun isClozeFront(front: String): Boolean =
            front.contains("[...]") || CLOZE_EMBEDDED.containsMatchIn(front)

        private const val SOURCE_ESTUDIA = "estudia"
        private const val LEGACY_ESTUDIA_BRIDGE_URL = "http://10.10.10.1:30004"
        private const val SYNC_BASE_URL_KEY = "sync.baseUrl"
        private const val SYNC_API_KEY_KEY = "sync.apiKey"
        private const val SYNC_PROJECT_ID_KEY = "sync.projectId"
        private const val SYNC_PROJECT_NAME_KEY = "sync.projectName"
        private const val SYNC_AUTO_KEY = "sync.auto"
        private const val SYNC_INTERVAL_KEY = "sync.intervalMin"
        private const val SYNC_LAST_AT_KEY = "sync.lastAt"
        private const val DEMO_SEEDED_KEY = "demo.seeded"
        /** Clave histórica; el valor es la versión de contenido demo. */
        private const val DEMO_TRIVIAL_VERSION_KEY = "demo.trivial.version"
        private const val DEMO_CONTENT_VERSION = 5

        private val DEMO_BOOK_MARKDOWN = """
            # Memoria y repetición espaciada

            Esta guía acompaña MemoCards. El modo lectura muestra el título activo arriba mientras haces scroll.

            ## Por qué funciona

            El olvido no es fallo: es selección. Repasar justo cuando la huella se debilita consolida el recuerdo sin saturar.

            ### SM-2 y FSRS

            MemoCards usa FSRS-6 en el dispositivo. Las calificaciones (otra vez / difícil / bien / fácil) alimentan el intervalo siguiente.

            ## Cómo estudiar

            1. Elige un mazo y pulsa estudiar.
            2. Lee el anverso; revela cuando estés listo.
            3. Califica con honestidad — el algoritmo necesita señal real.

            ## Cloze

            Usa `[...]` en el frente y la respuesta en el reverso, o `{{c1::texto}}` embebido.

            ## Libros desde estudIA

            Cuando importas un libro, las anotaciones, subrayados y fragmentos llegan junto al texto y se listan en la pestaña Anotaciones.
        """.trimIndent()

        private val DEMO_BOOK_ANNOTATIONS = listOf(
            BookAnnotation(
                quote = "El olvido no es fallo: es selección.",
                note = "Idea central: el algoritmo aprovecha el olvido, no lo combate a ciegas.",
                fragment = "Repasar justo cuando la huella se debilita consolida el recuerdo sin saturar.",
                color = "yellow",
                chapter = "Por qué funciona",
            ),
            BookAnnotation(
                quote = "Califica con honestidad — el algoritmo necesita señal real.",
                note = "Una calificación inflada retrasa el repaso y genera falsos recuerdos de dominio.",
                fragment = "Lee el anverso; revela cuando estés listo.",
                color = "blue",
                chapter = "Cómo estudiar",
            ),
            BookAnnotation(
                quote = "Usa `[...]` en el frente y la respuesta en el reverso",
                note = "También vale la sintaxis Anki `{{c1::texto}}`.",
                color = "green",
                chapter = "Cloze",
            ),
        )

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

        private val CLOZE_BIO_CARDS = listOf(
            "[...], is considered as the power house of the cell?" to "The Mitochondria",
            "What is the net energy of the Krebs cycle? [...]" to
                "The net gain is 3 NADH, 1 FADH2 and 1 GTP (≈ ATP) per acetyl-CoA.",
            "La fotosíntesis ocurre principalmente en los [...] de las plantas." to "cloroplastos",
            "El ADN está formado por [...] nitrogenadas, azúcar y fosfato." to "bases",
            "{{c1::La mitocondria}} es el orgánulo encargado de la respiración celular." to "",
            "La unidad básica de la vida es la {{c1::célula}}." to "",
            "[...] transporta oxígeno en la sangre humana." to "La hemoglobina",
            "Los [...] son los vasos que llevan sangre desde el corazón a los tejidos." to "arterias",
            "El proceso de división celular que produce gametos es la {{c1::meiosis}}." to "",
            "La membrana plasmática es {{c1::semipermeable}}." to "",
            "En humanos, el intercambio de gases ocurre en los [...] pulmonares." to "alvéolos",
            "[...] es el azúcar simple más usado como combustible celular." to "La glucosa",
        )

        private val HISTORY_QA_CARDS = listOf(
            "¿Qué fue la Revolución Francesa?" to
                "Un proceso político y social (1789–1799) que acabó con la monarquía absoluta en Francia y difundió ideas de ciudadanía, derechos y soberanía popular.",
            "¿Quién fue Cleopatra VII?" to
                "Última reina activa del Egipto ptolemaico; aliada de Julio César y Marco Antonio; su reinado terminó con la conquista romana (30 a. C.).",
            "¿Qué fue la Ruta de la Seda?" to
                "Red de rutas comerciales que conectaba Asia, Oriente Medio y Europa, intercambiando seda, especias, tecnologías e ideas.",
            "¿Qué provocó la Primera Guerra Mundial?" to
                "Un complejo de alianzas, militarismo e imperialismo; el detonante inmediato fue el asesinato del archiduque Francisco Fernando en Sarajevo (1914).",
            "¿Quién fue Simón Bolívar?" to
                "Líder independentista sudamericano; impulsó la liberación de varios territorios del dominio español a inicios del s. XIX.",
            "¿Qué fue el Renacimiento?" to
                "Movimiento cultural europeo (s. XIV–XVI) que recuperó ideales clásicos, impulsó el humanismo y renovó arte, ciencia y pensamiento.",
            "¿Cuándo se firmó la Declaración de Independencia de EE. UU.?" to
                "El 4 de julio de 1776, en el Congreso Continental de Filadelfia.",
            "¿Qué fue la Guerra Fría?" to
                "Confrontación ideológica y geopolítica (aprox. 1947–1991) entre el bloque liderado por EE. UU. y el liderado por la URSS, sin guerra directa total entre ambos.",
        )
    }
}

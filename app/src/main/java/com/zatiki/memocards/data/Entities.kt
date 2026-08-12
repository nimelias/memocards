package com.zatiki.memocards.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "study_days") val studyDays: Int? = null,
    @ColumnInfo(name = "min_repetitions") val minRepetitions: Int = 1,
    @ColumnInfo(name = "study_start_at") val studyStartAt: Long? = null,
    @ColumnInfo(name = "remote_deck_id") val remoteDeckId: Long? = null,
    val source: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deck_id")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "fields_json") val fieldsJson: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id"), Index(value = ["queue", "due"])],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long,
    val due: Long,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 0,
    val phase: Int = 0,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long? = null,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val queue: String = "new",
    @ColumnInfo(name = "remote_card_id") val remoteCardId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("card_id")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "card_id") val cardId: Long,
    val rating: Int,
    @ColumnInfo(name = "interval_before") val intervalBefore: Double,
    @ColumnInfo(name = "interval_after") val intervalAfter: Double,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long = 0L,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long,
)

@Entity(tableName = "pending_reviews")
data class PendingReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_card_id") val remoteCardId: Long,
    val rating: String,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    val phase: Int,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long? = null,
    val repetitions: Int,
    val lapses: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Estado FSRS a enviar a estudIA tras calificar una carta. */
data class CardFsrsSnapshot(
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val intervalDays: Int,
    val phase: Int,
    val lastReviewAt: Long?,
    val repetitions: Int,
    val lapses: Int,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val markdown: String,
    @ColumnInfo(name = "remote_book_id") val remoteBookId: Long? = null,
    val source: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class CardNoteRow(
    @ColumnInfo(name = "id") val cardId: Long,
    @ColumnInfo(name = "note_id") val noteId: Long,
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    val phase: Int,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long?,
    val repetitions: Int,
    val lapses: Int,
    val queue: String,
    @ColumnInfo(name = "created_at") val cardCreatedAt: Long,
    @ColumnInfo(name = "updated_at") val cardUpdatedAt: Long,
    @ColumnInfo(name = "n_id") val noteRowId: Long,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "fields_json") val fieldsJson: String,
    @ColumnInfo(name = "n_created_at") val noteCreatedAt: Long,
    @ColumnInfo(name = "n_updated_at") val noteUpdatedAt: Long,
)

data class QueueDueCount(
    val queue: String,
    val due: Long,
    val count: Int,
)

data class DeckCardCount(
    val deckId: Long,
    val count: Int,
)

data class QueueCount(
    val queue: String,
    val count: Int,
)

data class ReviewStamp(
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    val rating: Int = 0,
)

data class NoteSearchRow(
    val id: Long,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "deck_name") val deckName: String,
    @ColumnInfo(name = "fields_json") val fieldsJson: String,
)

data class NoteTypeRow(
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "fields_json") val fieldsJson: String,
)

data class LastReviewedDeckRow(
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "last_reviewed_at") val lastReviewedAt: Long,
)

@Dao
interface MemoDao {
    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    suspend fun listDecks(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeck(id: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE remote_deck_id = :remoteId LIMIT 1")
    suspend fun getDeckByRemoteId(remoteId: Long): DeckEntity?

    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("SELECT * FROM notes WHERE deck_id = :deckId ORDER BY updated_at DESC")
    suspend fun listNotes(deckId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteEntity?

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM cards WHERE remote_card_id = :remoteId LIMIT 1")
    suspend fun getCardByRemoteId(remoteId: Long): CardEntity?

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCard(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE note_id = :noteId LIMIT 1")
    suspend fun getCardByNote(noteId: Long): CardEntity?

    @Update
    suspend fun updateCard(card: CardEntity)

    @Insert
    suspend fun insertReviewLog(log: ReviewLogEntity): Long

    @Query(
        """
        UPDATE cards
        SET due = :due, stability = 0, difficulty = 0, interval_days = 0, phase = 0,
            last_review_at = NULL, repetitions = 0, lapses = 0,
            queue = 'new', updated_at = :updatedAt
        WHERE note_id IN (SELECT id FROM notes WHERE deck_id = :deckId)
        """,
    )
    suspend fun resetDeckCards(deckId: Long, due: Long, updatedAt: Long)

    @Query(
        """
        DELETE FROM review_log
        WHERE card_id IN (
          SELECT c.id FROM cards c JOIN notes n ON n.id = c.note_id WHERE n.deck_id = :deckId
        )
        """,
    )
    suspend fun clearDeckReviewLog(deckId: Long)

    @Query(
        """
        SELECT c.queue AS queue, c.due AS due, COUNT(*) AS count
        FROM cards c
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
        GROUP BY c.queue, c.due
        """,
    )
    suspend fun deckQueueDueCounts(deckId: Long): List<QueueDueCount>

    @Query(
        """
        SELECT c.queue AS queue, c.due AS due, COUNT(*) AS count
        FROM cards c
        GROUP BY c.queue, c.due
        """,
    )
    suspend fun allQueueDueCounts(): List<QueueDueCount>

    @Query(
        """
        SELECT COUNT(*) FROM review_log
        WHERE reviewed_at >= :dayStart AND reviewed_at <= :dayEnd
        """,
    )
    suspend fun countReviewsBetween(dayStart: Long, dayEnd: Long): Int

    @Query(
        """
        SELECT n.deck_id AS deckId, COUNT(c.id) AS count
        FROM cards c
        JOIN notes n ON n.id = c.note_id
        GROUP BY n.deck_id
        """,
    )
    suspend fun cardCountsByDeck(): List<DeckCardCount>

    @Query("SELECT deck_id, fields_json FROM notes")
    suspend fun listNoteTypeRows(): List<NoteTypeRow>

    @Query("SELECT queue, COUNT(*) AS count FROM cards GROUP BY queue")
    suspend fun queueCounts(): List<QueueCount>

    @Query(
        """
        SELECT reviewed_at, elapsed_ms, rating FROM review_log
        WHERE reviewed_at >= :since
        ORDER BY reviewed_at ASC
        """,
    )
    suspend fun listReviewsSince(since: Long): List<ReviewStamp>

    @Query(
        """
        SELECT r.reviewed_at, r.elapsed_ms, r.rating
        FROM review_log r
        JOIN cards c ON c.id = r.card_id
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
          AND r.reviewed_at >= :since
        ORDER BY r.reviewed_at ASC
        """,
    )
    suspend fun listReviewsSinceForDeck(deckId: Long, since: Long): List<ReviewStamp>

    @Query(
        """
        SELECT r.reviewed_at, r.elapsed_ms, r.rating
        FROM review_log r
        JOIN cards c ON c.id = r.card_id
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
        ORDER BY r.reviewed_at ASC
        """,
    )
    suspend fun listAllReviewsForDeck(deckId: Long): List<ReviewStamp>

    @Query(
        """
        SELECT c.queue AS queue, COUNT(*) AS count
        FROM cards c
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
        GROUP BY c.queue
        """,
    )
    suspend fun queueCountsForDeck(deckId: Long): List<QueueCount>

    @Query(
        """
        SELECT COUNT(*) FROM review_log r
        JOIN cards c ON c.id = r.card_id
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
        """,
    )
    suspend fun countReviewsForDeck(deckId: Long): Int

    @Query(
        """
        SELECT n.deck_id AS deck_id, MAX(r.reviewed_at) AS last_reviewed_at
        FROM review_log r
        JOIN cards c ON c.id = r.card_id
        JOIN notes n ON n.id = c.note_id
        GROUP BY n.deck_id
        ORDER BY last_reviewed_at DESC
        LIMIT 1
        """,
    )
    suspend fun lastReviewedDeck(): LastReviewedDeckRow?

    @Query(
        """
        SELECT * FROM decks
        WHERE name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        LIMIT :limit
        """,
    )
    suspend fun searchDecks(query: String, limit: Int = 30): List<DeckEntity>

    @Query(
        """
        SELECT n.id, n.deck_id, d.name AS deck_name, n.fields_json
        FROM notes n
        JOIN decks d ON d.id = n.deck_id
        WHERE n.fields_json LIKE '%' || :query || '%'
        ORDER BY n.updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun searchNotes(query: String, limit: Int = 40): List<NoteSearchRow>

    @Query(
        """
        SELECT c.id, c.note_id, c.due, c.stability, c.difficulty, c.interval_days, c.phase,
               c.last_review_at, c.repetitions, c.lapses, c.queue,
               c.created_at, c.updated_at,
               n.id AS n_id, n.deck_id, n.fields_json, n.created_at AS n_created_at, n.updated_at AS n_updated_at
        FROM cards c
        JOIN notes n ON n.id = c.note_id
        WHERE n.deck_id = :deckId
          AND (
            c.queue = 'new'
            OR c.due <= :endOfDay
            OR (:inStudyWindow = 1 AND c.repetitions < :minRepetitions)
          )
        ORDER BY
          CASE c.queue WHEN 'learning' THEN 0 WHEN 'review' THEN 1 WHEN 'new' THEN 2 ELSE 3 END,
          c.due ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueCards(
        deckId: Long,
        endOfDay: Long,
        inStudyWindow: Int,
        minRepetitions: Int,
        limit: Int,
    ): List<CardNoteRow>

    @Query("SELECT * FROM app_settings WHERE `key` LIKE 'ui.%'")
    suspend fun getUiSettingsRows(): List<AppSettingEntity>

    @Query("SELECT * FROM app_settings WHERE `key` LIKE 'sync.%'")
    suspend fun getSyncSettingsRows(): List<AppSettingEntity>

    @Query("SELECT * FROM pending_reviews ORDER BY created_at ASC")
    suspend fun listPendingReviews(): List<PendingReviewEntity>

    @Insert
    suspend fun insertPendingReview(review: PendingReviewEntity): Long

    @Query("DELETE FROM pending_reviews WHERE id = :id")
    suspend fun deletePendingReview(id: Long)

    @Query("SELECT * FROM decks WHERE source = 'estudia' AND remote_deck_id IS NOT NULL")
    suspend fun listEstudiaDecks(): List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    suspend fun listBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE remote_book_id = :remoteId LIMIT 1")
    suspend fun getBookByRemoteId(remoteId: Long): BookEntity?

    @Insert
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)
}

@Database(
    entities = [
        DeckEntity::class,
        NoteEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        PendingReviewEntity::class,
        AppSettingEntity::class,
        BookEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun dao(): MemoDao
}

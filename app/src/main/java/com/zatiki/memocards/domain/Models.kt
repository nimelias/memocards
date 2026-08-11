package com.zatiki.memocards.domain

import com.zatiki.memocards.fsrs.FsrsPhase

enum class CardQueue(val value: String) {
    NEW("new"),
    LEARNING("learning"),
    REVIEW("review");

    companion object {
        fun from(value: String): CardQueue =
            entries.find { it.value == value } ?: NEW

        fun fromPhase(phase: FsrsPhase): CardQueue = when (phase) {
            FsrsPhase.Added -> NEW
            FsrsPhase.ReLearning -> LEARNING
            FsrsPhase.Review -> REVIEW
        }
    }
}

/** Calificaciones de repaso: 1=Again, 2=Hard, 3=Good, 4=Easy */
typealias ReviewRating = Int

data class NoteFields(
    val front: String = "",
    val back: String = "",
    val frontImage: String? = null,
    val backImage: String? = null,
)

data class Deck(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val studyDays: Int? = null,
    val minRepetitions: Int = 1,
    val studyStartAt: Long? = null,
    val remoteDeckId: Long? = null,
    val source: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Note(
    val id: Long = 0,
    val deckId: Long,
    val fields: NoteFields,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Card(
    val id: Long = 0,
    val noteId: Long,
    val due: Long,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val intervalDays: Int = 0,
    val phase: FsrsPhase = FsrsPhase.Added,
    val lastReviewAt: Long? = null,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val queue: CardQueue = CardQueue.NEW,
    val remoteCardId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CardWithNote(
    val card: Card,
    val note: Note,
)

data class DeckStats(
    val newCount: Int,
    val dueCount: Int,
    val total: Int,
)

/** Contadores por cola en un mazo (para buckets del detalle). */
data class DeckBucketStats(
    val total: Int,
    val newCount: Int,
    val learningCount: Int,
    val reviewDueCount: Int,
) {
    val leftToStudy: Int get() = newCount + learningCount + reviewDueCount

    fun percentOf(count: Int): Int =
        if (total <= 0) 0 else ((count * 100f) / total).toInt().coerceIn(0, 100)
}

/** Agregados del home (local, día actual). */
data class HomeStats(
    val cardsDone: Int,
    val leftToAnswer: Int,
)

data class DeckSummary(
    val deck: Deck,
    val cardCount: Int,
    val clozeCount: Int = 0,
    val qaCount: Int = 0,
)

data class DayActivity(
    val dayStart: Long,
    val reviewCount: Int,
    /** Suma de ratings (1–4) para colorear por feedback medio. */
    val ratingSum: Int = 0,
)

/** Actividad por hora del día actual (granularidad intradía). */
data class HourActivity(
    val hour: Int,
    val reviewCount: Int,
    val ratingSum: Int = 0,
)

data class ActivityStats(
    val cardsStudiedToday: Int,
    val elapsedMsToday: Long,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val heatmap: List<DayActivity>,
    val hourlyToday: List<HourActivity> = emptyList(),
)

data class NoteSearchHit(
    val noteId: Long,
    val deckId: Long,
    val deckName: String,
    val frontPreview: String,
)

data class DeckSettings(
    val studyDays: Int?,
    val minRepetitions: Int,
)

data class UiSettings(
    val theme: ThemeName = ThemeName.LIGHT,
    val fontScale: Float = 1f,
    /** Interlineado relativo para modo lectura libro. */
    val lineHeight: Float = 1.45f,
    val ratingLayout: RatingLayout = RatingLayout.ARC_RIGHT,
    val arcLabelMode: ArcLabelMode = ArcLabelMode.ICONS,
    /** Intensidad de los degradados de fondo (0 = apagado, 1 = normal). */
    val glowIntensity: Float = 1f,
)

data class Book(
    val id: Long = 0,
    val title: String,
    val markdown: String,
    val remoteBookId: Long? = null,
    val source: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class EstudiaBookSummary(
    val id: Long,
    val title: String,
)

/** Conexión con estudIA (URL pública; auth vía cabecera X-KEY). */
data class SyncSettings(
    val baseUrl: String = "https://estudia.zatiki.xyz:3005",
    val apiKey: String = "123",
    val projectId: Long? = null,
    val autoSyncEnabled: Boolean = false,
    val autoSyncIntervalMinutes: Int = 30,
    val lastSyncAt: Long = 0L,
)

data class EstudiaProject(
    val id: Long,
    val name: String,
)

data class EstudiaDeckSummary(
    val id: Long,
    val title: String,
    val cardCount: Int,
    val description: String? = null,
)

data class EstudiaRemoteCard(
    val id: Long,
    val front: String,
    val back: String,
    val updatedAt: Long,
)

data class SyncResult(
    val importedDecks: Int = 0,
    val updatedCards: Int = 0,
    val pushedReviews: Int = 0,
    val message: String = "",
)

enum class ThemeName(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SAND("sand"),
    /** Oscuro navy/púrpura inspirado en Emich; cartas de estudio integradas. */
    EMICH("emich");

    val isDark: Boolean
        get() = this == DARK || this == EMICH

    companion object {
        fun from(value: String?): ThemeName =
            entries.find { it.value == value } ?: LIGHT
    }
}

/** Contenido de cada sector del menú arco: solo iconos o solo texto. */
enum class ArcLabelMode(val value: String) {
    ICONS("icons"),
    TEXT("text");

    companion object {
        fun from(value: String?): ArcLabelMode =
            entries.find { it.value == value } ?: ICONS
    }
}

/** Disposición de los botones de calificación en estudio. */
enum class RatingLayout(val value: String) {
    BAR("bar"),
    ARC_RIGHT("arc_right"),
    ARC_LEFT("arc_left");

    val isArc: Boolean get() = this != BAR

    companion object {
        fun from(value: String?): RatingLayout =
            entries.find { it.value == value } ?: ARC_RIGHT
    }
}

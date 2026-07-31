package com.zatiki.memocards.domain

enum class CardQueue(val value: String) {
    NEW("new"),
    LEARNING("learning"),
    REVIEW("review");

    companion object {
        fun from(value: String): CardQueue =
            entries.find { it.value == value } ?: NEW
    }
}

/** SM-2 ratings: 1=Again, 2=Hard, 3=Good, 4=Easy */
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
    val interval: Double = 0.0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val queue: CardQueue = CardQueue.NEW,
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

data class DeckSettings(
    val studyDays: Int?,
    val minRepetitions: Int,
)

data class UiSettings(
    val theme: ThemeName = ThemeName.LIGHT,
    val fontScale: Float = 1f,
    val ratingLayout: RatingLayout = RatingLayout.BAR,
)

enum class ThemeName(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SAND("sand");

    companion object {
        fun from(value: String?): ThemeName =
            entries.find { it.value == value } ?: LIGHT
    }
}

/** Disposición de los botones de calificación en estudio. */
enum class RatingLayout(val value: String) {
    BAR("bar"),
    ARC_RIGHT("arc_right"),
    ARC_LEFT("arc_left");

    companion object {
        fun from(value: String?): RatingLayout =
            entries.find { it.value == value } ?: BAR
    }
}

data class ScheduleResult(
    val due: Long,
    val interval: Double,
    val easeFactor: Double,
    val repetitions: Int,
    val lapses: Int,
    val queue: CardQueue,
)

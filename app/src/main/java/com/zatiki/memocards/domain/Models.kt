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
    val interval: Double = 0.0,
    val easeFactor: Double = 2.5,
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

data class DeckSettings(
    val studyDays: Int?,
    val minRepetitions: Int,
)

/** Algoritmo de repetición espaciada. */
enum class SchedulerAlgorithm(val value: String) {
    SM2("sm2"),
    FSRS("fsrs");

    companion object {
        fun from(value: String?): SchedulerAlgorithm =
            entries.find { it.value == value } ?: SM2
    }
}

data class UiSettings(
    val theme: ThemeName = ThemeName.LIGHT,
    val fontScale: Float = 1f,
    val ratingLayout: RatingLayout = RatingLayout.ARC_RIGHT,
    val arcLabelMode: ArcLabelMode = ArcLabelMode.ICONS,
    val scheduler: SchedulerAlgorithm = SchedulerAlgorithm.SM2,
)

/** Conexión con el bridge estudIA (puerto 30004). */
data class SyncSettings(
    val baseUrl: String = "http://10.10.10.1:30004",
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
    SAND("sand");

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

data class ScheduleResult(
    val due: Long,
    val interval: Double,
    val easeFactor: Double,
    val repetitions: Int,
    val lapses: Int,
    val queue: CardQueue,
)

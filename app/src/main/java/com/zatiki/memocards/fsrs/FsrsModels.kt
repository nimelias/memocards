package com.zatiki.memocards.fsrs

/**
 * Modelos FSRS v6 basados en [FSRS-Kotlin](https://github.com/open-spaced-repetition/FSRS-Kotlin).
 */
enum class FsrsRating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4),
    ;

    companion object {
        fun from(value: Int): FsrsRating =
            entries.find { it.value == value } ?: Good
    }
}

enum class FsrsPhase(val value: Int) {
    Added(0),
    ReLearning(1),
    Review(2),
    ;

    companion object {
        fun from(value: Int): FsrsPhase =
            entries.find { it.value == value } ?: Added
    }
}

data class FsrsCardState(
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val intervalDays: Int = 0,
    val phase: FsrsPhase = FsrsPhase.Added,
    val lastReviewAt: Long? = null,
    val repetitions: Int = 0,
    val lapses: Int = 0,
)

data class FsrsReviewResult(
    val stability: Double,
    val difficulty: Double,
    val intervalDays: Int,
    val due: Long,
    val phase: FsrsPhase,
    val repetitions: Int,
    val lapses: Int,
    val lastReviewAt: Long,
)

internal data class FsrsGradePreview(
    val rating: FsrsRating,
    val stability: Double,
    val difficulty: Double,
    val intervalDays: Int,
    val durationMillis: Long,
)

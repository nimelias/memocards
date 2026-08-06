package com.zatiki.memocards.fsrs

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Motor FSRS v6 basado en [FSRS-Kotlin](https://github.com/open-spaced-repetition/FSRS-Kotlin).
 */
class FsrsScheduler(
    private val requestRetention: Double = DEFAULT_RETENTION,
    private val params: DoubleArray = DEFAULT_PARAMS,
) {
    private val decay = -params[20]
    private val factor = 0.9.pow(1.0 / decay) - 1
    private val enableFuzz = true

    fun review(
        card: FsrsCardState,
        rating: FsrsRating,
        nowMillis: Long = System.currentTimeMillis(),
    ): FsrsReviewResult {
        val safeCard = card.normalized()
        val preview = preview(safeCard, nowMillis).first { it.rating == rating }
        val newPhase = nextPhase(safeCard.phase, rating, preview.intervalDays)
        val due = computeDue(nowMillis, preview.intervalDays, preview.durationMillis)
        val lapses = safeCard.lapses + if (safeCard.phase == FsrsPhase.Review && rating == FsrsRating.Again) 1 else 0
        return FsrsReviewResult(
            stability = preview.stability.coerceFinite(MIN_STABILITY),
            difficulty = preview.difficulty.coerceFinite(DEFAULT_DIFFICULTY).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY),
            intervalDays = preview.intervalDays,
            due = due,
            phase = newPhase,
            repetitions = safeCard.repetitions + 1,
            lapses = lapses,
            lastReviewAt = nowMillis,
        )
    }

    private fun preview(card: FsrsCardState, nowMillis: Long = System.currentTimeMillis()): List<FsrsGradePreview> {
        return FsrsRating.entries.map { rating ->
            val outcome = computeOutcome(card, rating, nowMillis)
            FsrsGradePreview(
                rating = rating,
                stability = outcome.stability,
                difficulty = outcome.difficulty,
                intervalDays = outcome.intervalDays,
                durationMillis = outcome.durationMillis,
            )
        }
    }

    private data class Outcome(
        val stability: Double,
        val difficulty: Double,
        val intervalDays: Int,
        val durationMillis: Long,
    )

    private data class InitState(var difficulty: Double = 0.0, var stability: Double = 0.0)

    private fun computeOutcome(card: FsrsCardState, rating: FsrsRating, nowMillis: Long): Outcome {
        val dayMillis = 24 * 60 * 60 * 1000L
        return when (card.phase) {
            FsrsPhase.Added -> {
                val state = initState(rating)
                when (rating) {
                    FsrsRating.Again -> Outcome(
                        stability = state.stability,
                        difficulty = state.difficulty,
                        intervalDays = 0,
                        durationMillis = 3 * 60 * 1000L,
                    )
                    FsrsRating.Hard -> Outcome(
                        stability = state.stability,
                        difficulty = state.difficulty,
                        intervalDays = 0,
                        durationMillis = 5 * 60 * 1000L,
                    )
                    FsrsRating.Good -> Outcome(
                        stability = state.stability,
                        difficulty = state.difficulty,
                        intervalDays = 0,
                        durationMillis = 10 * 60 * 1000L,
                    )
                    FsrsRating.Easy -> Outcome(
                        stability = state.stability,
                        difficulty = state.difficulty,
                        intervalDays = 1,
                        durationMillis = dayMillis,
                    )
                }
            }

            FsrsPhase.ReLearning -> {
                val (stateAgain, stateHard, stateGood, stateEasy) = relearningStates(card)
                when (rating) {
                    FsrsRating.Again -> Outcome(
                        stability = stateAgain.stability,
                        difficulty = stateAgain.difficulty,
                        intervalDays = card.intervalDays,
                        durationMillis = 3 * 60 * 1000L,
                    )
                    FsrsRating.Hard -> Outcome(
                        stability = stateHard.stability,
                        difficulty = stateHard.difficulty,
                        intervalDays = card.intervalDays,
                        durationMillis = 10 * 60 * 1000L,
                    )
                    FsrsRating.Good -> {
                        val ivl = nextInterval(stateGood.stability, card.intervalDays)
                        Outcome(
                            stability = stateGood.stability,
                            difficulty = stateGood.difficulty,
                            intervalDays = ivl,
                            durationMillis = ivl * dayMillis,
                        )
                    }
                    FsrsRating.Easy -> {
                        val ivlGood = nextInterval(stateGood.stability, card.intervalDays)
                        val ivl = max(nextInterval(stateEasy.stability, card.intervalDays), ivlGood + 1)
                        Outcome(
                            stability = stateEasy.stability,
                            difficulty = stateEasy.difficulty,
                            intervalDays = ivl,
                            durationMillis = ivl * dayMillis,
                        )
                    }
                }
            }

            FsrsPhase.Review -> {
                val stability = card.stability.coerceAtLeast(MIN_STABILITY)
                val difficulty = card.difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
                val elapsedDays = elapsedDaysSince(card.lastReviewAt, nowMillis)
                val retrievability = forgettingCurve(elapsedDays, stability)
                val stateAgain = InitState(
                    difficulty = nextDifficulty(difficulty, FsrsRating.Again),
                    stability = nextForgetStability(difficulty, stability, retrievability),
                )
                val stateHard = InitState(
                    difficulty = nextDifficulty(difficulty, FsrsRating.Hard),
                    stability = nextRecallStability(
                        difficulty,
                        stability,
                        retrievability,
                        FsrsRating.Hard,
                    ),
                )
                val stateGood = InitState(
                    difficulty = nextDifficulty(difficulty, FsrsRating.Good),
                    stability = nextRecallStability(
                        difficulty,
                        stability,
                        retrievability,
                        FsrsRating.Good,
                    ),
                )
                val stateEasy = InitState(
                    difficulty = nextDifficulty(difficulty, FsrsRating.Easy),
                    stability = nextRecallStability(
                        difficulty,
                        stability,
                        retrievability,
                        FsrsRating.Easy,
                    ),
                )
                var ivlHard = nextInterval(stateHard.stability, card.intervalDays)
                var ivlGood = nextInterval(stateGood.stability, card.intervalDays)
                var ivlEasy = nextInterval(stateEasy.stability, card.intervalDays)
                ivlHard = min(ivlHard, ivlGood)
                ivlGood = min(ivlGood, ivlHard + 1)
                ivlEasy = min(ivlEasy, ivlGood + 1)
                when (rating) {
                    FsrsRating.Again -> Outcome(
                        stability = stateAgain.stability,
                        difficulty = stateAgain.difficulty,
                        intervalDays = card.intervalDays,
                        durationMillis = 3 * 60 * 1000L,
                    )
                    FsrsRating.Hard -> Outcome(
                        stability = stateHard.stability,
                        difficulty = stateHard.difficulty,
                        intervalDays = ivlHard,
                        durationMillis = ivlHard * dayMillis,
                    )
                    FsrsRating.Good -> Outcome(
                        stability = stateGood.stability,
                        difficulty = stateGood.difficulty,
                        intervalDays = ivlGood,
                        durationMillis = ivlGood * dayMillis,
                    )
                    FsrsRating.Easy -> Outcome(
                        stability = stateEasy.stability,
                        difficulty = stateEasy.difficulty,
                        intervalDays = ivlEasy,
                        durationMillis = ivlEasy * dayMillis,
                    )
                }
            }
        }
    }

    private fun relearningStates(card: FsrsCardState): List<InitState> {
        if (card.difficulty <= 0.0 || card.stability <= 0.0) {
            return FsrsRating.entries.map { initState(it) }
        }
        val lastD = card.difficulty
        val lastS = card.stability
        return FsrsRating.entries.map { rating ->
            InitState(
                difficulty = nextDifficulty(lastD, rating),
                stability = nextShortTermStability(lastS, rating),
            )
        }
    }

    private fun nextPhase(current: FsrsPhase, rating: FsrsRating, intervalDays: Int): FsrsPhase {
        return when (current) {
            FsrsPhase.Added -> when (rating) {
                FsrsRating.Easy -> FsrsPhase.Review
                FsrsRating.Again, FsrsRating.Hard -> FsrsPhase.Added
                FsrsRating.Good -> if (intervalDays >= 1) FsrsPhase.Review else FsrsPhase.Added
            }
            FsrsPhase.ReLearning -> when (rating) {
                FsrsRating.Again, FsrsRating.Hard -> FsrsPhase.ReLearning
                FsrsRating.Good, FsrsRating.Easy -> if (intervalDays >= 1) FsrsPhase.Review else FsrsPhase.ReLearning
            }
            FsrsPhase.Review -> when (rating) {
                FsrsRating.Again -> FsrsPhase.ReLearning
                else -> FsrsPhase.Review
            }
        }
    }

    private fun computeDue(nowMillis: Long, intervalDays: Int, durationMillis: Long): Long {
        return if (intervalDays < 1) {
            nowMillis + durationMillis
        } else {
            StudyPeriod.addDays(StudyPeriod.startOfDay(nowMillis), intervalDays.toDouble())
        }
    }

    private fun elapsedDaysSince(lastReviewAt: Long?, nowMillis: Long): Double {
        if (lastReviewAt == null) return cardFallbackDays
        return max(0.0, (nowMillis - lastReviewAt).toDouble() / DAY_MILLIS)
    }

    private fun applyFuzz(interval: Double, fuzzFactor: Double, scheduledDays: Int = 0): Double {
        if (!enableFuzz || interval < 2.5) return interval
        val ivl = interval.roundToInt()
        var minIvl = max(2, (ivl * 0.95 - 1).roundToInt())
        val maxIvl = (ivl * 1.05 + 1).roundToInt()
        if (scheduledDays > 0 && ivl > scheduledDays) {
            minIvl = max(minIvl, scheduledDays + 1)
        }
        return floor(fuzzFactor * (maxIvl - minIvl + 1) + minIvl)
    }

    private fun forgettingCurve(interval: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return exp(-interval / stability)
    }

    private fun generateFuzzFactor(): Double = Random(System.currentTimeMillis()).nextDouble()

    private fun initDifficulty(rating: FsrsRating): Double {
        val raw = params[4] - exp(params[5] * (rating.value - 1)) + 1
        return raw.coerceIn(1.0, 10.0).round2()
    }

    private fun initStability(rating: FsrsRating): Double {
        val value = params.getOrElse(rating.value - 1) { 0.1 }
        return value.coerceAtLeast(0.1).round2()
    }

    private fun initState(rating: FsrsRating): InitState {
        return InitState(
            difficulty = initDifficulty(rating),
            stability = initStability(rating),
        )
    }

    private fun linearDamping(delta: Double, oldD: Double): Double {
        return delta * (10 - oldD) / 9
    }

    private fun meanReversion(initD: Double, nextD: Double): Double {
        return params[7] * initD + (1 - params[7]) * nextD
    }

    private fun nextInterval(stability: Double, lastInterval: Int = 0, maxInterval: Int = 36500): Int {
        val fuzzFactor = generateFuzzFactor()
        val rawInterval = stability / factor * (requestRetention.pow(1 / decay) - 1)
        val fuzzed = applyFuzz(rawInterval, fuzzFactor, scheduledDays = lastInterval)
        return fuzzed.roundToInt().coerceIn(1, maxInterval)
    }

    private fun nextDifficulty(currentD: Double, rating: FsrsRating): Double {
        val deltaD = -params[6] * (rating.value - 3)
        val damped = linearDamping(deltaD, currentD)
        val nextD = currentD + damped
        val reverted = meanReversion(initDifficulty(FsrsRating.Easy), nextD)
        return reverted.coerceIn(1.0, 10.0).round2()
    }

    private fun nextShortTermStability(currentS: Double, rating: FsrsRating): Double {
        val safeS = currentS.coerceAtLeast(MIN_STABILITY)
        var sinc = exp(params[17] * (rating.value - 3 + params[18])) * safeS.pow(-params[19])
        if (rating.value >= 3) {
            sinc = max(sinc, 1.0)
        }
        return abs(safeS * sinc).coerceFinite(MIN_STABILITY)
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
    ): Double {
        val sMin = stability / exp(params[17] * params[18])
        val result = params[11] *
            difficulty.pow(-params[12]) *
            ((stability + 1).pow(params[13]) - 1) *
            exp((1 - retrievability) * params[14])
        return min(result, sMin).round2()
    }

    private fun nextRecallStability(
        d: Double,
        s: Double,
        r: Double,
        rating: FsrsRating,
    ): Double {
        val hardPenalty = if (rating == FsrsRating.Hard) params[15] else 1.0
        val easyBonus = if (rating == FsrsRating.Easy) params[16] else 1.0
        val factorTerm = exp(params[8]) *
            (11 - d) *
            s.pow(-params[9]) *
            (exp((1 - r) * params[10]) - 1) *
            hardPenalty *
            easyBonus
        return (s * (1 + factorTerm)).round2()
    }

    private fun Double.round2(): Double = round(this * 100.0) / 100.0

    private fun Double.coerceFinite(fallback: Double): Double =
        if (isNaN() || isInfinite()) fallback else this

    private fun FsrsCardState.normalized(): FsrsCardState {
        return when (phase) {
            FsrsPhase.Added -> this
            FsrsPhase.ReLearning, FsrsPhase.Review -> copy(
                stability = stability.coerceAtLeast(MIN_STABILITY),
                difficulty = if (difficulty <= 0.0) {
                    DEFAULT_DIFFICULTY
                } else {
                    difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
                },
            )
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val DEFAULT_RETENTION = 0.9
        private const val cardFallbackDays = 0.0
        private const val MIN_STABILITY = 0.1
        private const val MIN_DIFFICULTY = 1.0
        private const val MAX_DIFFICULTY = 10.0
        private const val DEFAULT_DIFFICULTY = 5.0

        val DEFAULT_PARAMS: DoubleArray = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
            0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
    }
}

/** Utilidades de fechas para el scheduler FSRS. */
object StudyPeriod {
    private const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_MINUTE = 60_000L

    fun startOfDay(ts: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun addDays(ts: Long, days: Double): Long =
        ts + (days * MS_PER_DAY).toLong()

    fun addMinutes(ts: Long, minutes: Int): Long =
        ts + minutes * MS_PER_MINUTE

    fun studyEndDate(studyStartAt: Long, studyDays: Int): Long =
        addDays(startOfDay(studyStartAt), studyDays.toDouble())

    fun capDueToStudyPeriod(due: Long, studyStartAt: Long?, studyDays: Int?): Long {
        if (studyStartAt == null || studyDays == null) return due
        return minOf(due, studyEndDate(studyStartAt, studyDays))
    }
}

package com.zatiki.memocards.domain

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * FSRS-4.5 simplificado para MemoCards.
 * Usa stability/difficulty en la carta (mapeados desde interval/easeFactor si aún no migró).
 */
object Fsrs {
    private val weights = doubleArrayOf(
        0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0234, 1.616,
        0.1544, 1.0, 1.9395, 0.11, 0.29, 2.2696, 0.231, 2.9898, 0.5165, 0.6621,
    )

    fun stabilityOf(card: Card): Double =
        if (card.interval > 0) card.interval else 0.0

    fun difficultyOf(card: Card): Double =
        if (card.easeFactor in 1.3..10.0) card.easeFactor else 5.0

    fun scheduleReview(card: Card, rating: ReviewRating, now: Long = System.currentTimeMillis()): ScheduleResult {
        var stability = stabilityOf(card)
        var difficulty = difficultyOf(card)
        var repetitions = card.repetitions
        var lapses = card.lapses
        var queue = card.queue

        if (rating == 1) {
            lapses += 1
            repetitions = 0
            stability = max(0.2, stability * 0.5)
            difficulty = min(10.0, difficulty + 1.0)
            queue = CardQueue.LEARNING
            val due = Sm2.addMinutes(now, 10)
            return ScheduleResult(due, stability, difficulty, repetitions, lapses, queue)
        }

        if (queue == CardQueue.NEW || stability <= 0) {
            stability = initStability(rating)
            difficulty = initDifficulty(rating)
            repetitions = 1
            queue = CardQueue.REVIEW
            val interval = max(1.0, stability)
            val due = Sm2.addDays(Sm2.startOfDay(now), interval)
            return ScheduleResult(due, stability, difficulty, repetitions, lapses, queue)
        }

        difficulty = updateDifficulty(difficulty, rating)
        stability = nextStability(stability, difficulty, rating)
        repetitions += 1
        queue = CardQueue.REVIEW
        val interval = max(1.0, stability)
        val due = Sm2.addDays(Sm2.startOfDay(now), interval)
        return ScheduleResult(due, stability, difficulty, repetitions, lapses, queue)
    }

    private fun initStability(rating: ReviewRating): Double = when (rating) {
        2 -> weights[0]
        3 -> weights[1]
        else -> weights[2]
    }

    private fun initDifficulty(rating: ReviewRating): Double {
        val raw = weights[3] - (rating - 3) * weights[4]
        return raw.coerceIn(1.0, 10.0)
    }

    private fun updateDifficulty(d: Double, rating: ReviewRating): Double {
        val delta = -weights[5] * (rating - 3)
        val next = d + delta * (10 - d) / 9
        return next.coerceIn(1.0, 10.0)
    }

    private fun nextStability(s: Double, d: Double, rating: ReviewRating): Double {
        val hardPenalty = if (rating == 2) weights[6] else 1.0
        val easyBonus = if (rating == 4) weights[7] else 1.0
        val base = s * (1 + exp(weights[8]) * (11 - d) * pow(s, -weights[9]) * hardPenalty * easyBonus)
        return max(0.2, base)
    }

    private fun pow(a: Double, b: Double): Double = exp(b * ln(a.coerceAtLeast(1e-6)))
}

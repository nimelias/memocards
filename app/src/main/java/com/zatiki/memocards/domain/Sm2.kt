package com.zatiki.memocards.domain

import java.util.Calendar

private const val MS_PER_DAY = 86_400_000L
private const val MS_PER_MINUTE = 60_000L

object Sm2 {
    fun startOfDay(ts: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun addDays(ts: Long, days: Double): Long =
        ts + (days * MS_PER_DAY).toLong()

    fun addMinutes(ts: Long, minutes: Int): Long =
        ts + minutes * MS_PER_MINUTE

    fun scheduleReview(card: Card, rating: ReviewRating, now: Long = System.currentTimeMillis()): ScheduleResult {
        var easeFactor = card.easeFactor
        var repetitions = card.repetitions
        var lapses = card.lapses
        var queue = card.queue
        var interval = card.interval
        var due = now

        if (rating == 1) {
            lapses += 1
            repetitions = 0
            interval = 0.0
            queue = CardQueue.LEARNING
            due = addMinutes(now, 10)
            return ScheduleResult(due, interval, easeFactor, repetitions, lapses, queue)
        }

        easeFactor = maxOf(
            1.3,
            easeFactor + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02)),
        )

        if (queue == CardQueue.NEW || queue == CardQueue.LEARNING) {
            repetitions = 1
            interval = if (rating == 4) 4.0 else 1.0
            queue = CardQueue.REVIEW
            due = addDays(startOfDay(now), interval)
            return ScheduleResult(due, interval, easeFactor, repetitions, lapses, queue)
        }

        interval = if (repetitions <= 1) {
            6.0
        } else {
            Math.round(interval * easeFactor).toDouble()
        }

        when (rating) {
            2 -> interval = maxOf(1.0, Math.round(interval * 1.2).toDouble())
            4 -> interval = Math.round(interval * 1.3).toDouble()
        }

        repetitions += 1
        queue = CardQueue.REVIEW
        due = addDays(startOfDay(now), interval)

        return ScheduleResult(due, interval, easeFactor, repetitions, lapses, queue)
    }

    fun studyEndDate(studyStartAt: Long, studyDays: Int): Long =
        addDays(startOfDay(studyStartAt), studyDays.toDouble())

    fun capDueToStudyPeriod(due: Long, studyStartAt: Long?, studyDays: Int?): Long {
        if (studyStartAt == null || studyDays == null) return due
        return minOf(due, studyEndDate(studyStartAt, studyDays))
    }
}

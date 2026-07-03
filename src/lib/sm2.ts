import type { Card, ReviewRating } from '../types';

const MS_PER_DAY = 86_400_000;
const MS_PER_MINUTE = 60_000;

export function startOfDay(ts = Date.now()): number {
  const d = new Date(ts);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

export function addDays(ts: number, days: number): number {
  return ts + days * MS_PER_DAY;
}

export function addMinutes(ts: number, minutes: number): number {
  return ts + minutes * MS_PER_MINUTE;
}

export interface ScheduleResult {
  due: number;
  interval: number;
  easeFactor: number;
  repetitions: number;
  lapses: number;
  queue: Card['queue'];
}

export function scheduleReview(card: Card, rating: ReviewRating, now = Date.now()): ScheduleResult {
  const intervalBefore = card.interval;
  let { easeFactor, repetitions, lapses, queue } = card;
  let interval = card.interval;
  let due = now;

  if (rating === 1) {
    lapses += 1;
    repetitions = 0;
    interval = 0;
    queue = 'learning';
    due = addMinutes(now, 10);
    return { due, interval, easeFactor, repetitions, lapses, queue };
  }

  easeFactor = Math.max(
    1.3,
    easeFactor + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02)),
  );

  if (queue === 'new' || queue === 'learning') {
    repetitions = 1;
    interval = rating === 4 ? 4 : 1;
    queue = 'review';
    due = addDays(startOfDay(now), interval);
    return { due, interval, easeFactor, repetitions, lapses, queue };
  }

  if (repetitions <= 1) {
    interval = 6;
  } else {
    interval = Math.round(interval * easeFactor);
  }

  if (rating === 2) {
    interval = Math.max(1, Math.round(interval * 1.2));
  } else if (rating === 4) {
    interval = Math.round(interval * 1.3);
  }

  repetitions += 1;
  queue = 'review';
  due = addDays(startOfDay(now), interval);

  return { due, interval, easeFactor, repetitions, lapses, queue };
}

export function isDue(card: Card, now = Date.now()): boolean {
  if (card.queue === 'new') return true;
  return card.due <= now;
}

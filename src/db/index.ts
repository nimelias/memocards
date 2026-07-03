import * as SQLite from 'expo-sqlite';
import type { Card, CardQueue, CardWithNote, Deck, Note, NoteFields, ReviewLog, ReviewRating } from '../types';
import { isDue, scheduleReview, startOfDay } from '../lib/sm2';
import { SCHEMA_SQL } from './schema';

const DB_NAME = 'memocards.db';

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

export async function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (!dbPromise) {
    dbPromise = (async () => {
      const db = await SQLite.openDatabaseAsync(DB_NAME);
      await db.execAsync(SCHEMA_SQL);
      return db;
    })();
  }
  return dbPromise;
}

function now() {
  return Date.now();
}

function parseNoteFields(raw: string): NoteFields {
  try {
    const parsed = JSON.parse(raw) as Partial<NoteFields>;
    return {
      front: parsed.front ?? '',
      back: parsed.back ?? '',
    };
  } catch {
    return { front: '', back: '' };
  }
}

function rowToDeck(row: Record<string, unknown>): Deck {
  return {
    id: row.id as number,
    name: row.name as string,
    parentId: (row.parent_id as number | null) ?? null,
    createdAt: row.created_at as number,
    updatedAt: row.updated_at as number,
  };
}

function rowToNote(row: Record<string, unknown>): Note {
  return {
    id: row.id as number,
    deckId: row.deck_id as number,
    fields: parseNoteFields(row.fields_json as string),
    createdAt: row.created_at as number,
    updatedAt: row.updated_at as number,
  };
}

function rowToCard(row: Record<string, unknown>): Card {
  return {
    id: row.id as number,
    noteId: row.note_id as number,
    due: row.due as number,
    interval: row.interval as number,
    easeFactor: row.ease_factor as number,
    repetitions: row.repetitions as number,
    lapses: row.lapses as number,
    queue: row.queue as CardQueue,
    createdAt: row.created_at as number,
    updatedAt: row.updated_at as number,
  };
}

export async function listDecks(): Promise<Deck[]> {
  const db = await getDatabase();
  const rows = await db.getAllAsync('SELECT * FROM decks ORDER BY name COLLATE NOCASE');
  return rows.map((row) => rowToDeck(row as Record<string, unknown>));
}

export async function createDeck(name: string): Promise<Deck> {
  const db = await getDatabase();
  const ts = now();
  const result = await db.runAsync(
    'INSERT INTO decks (name, created_at, updated_at) VALUES (?, ?, ?)',
    name.trim(),
    ts,
    ts,
  );
  return {
    id: result.lastInsertRowId,
    name: name.trim(),
    parentId: null,
    createdAt: ts,
    updatedAt: ts,
  };
}

export async function getDeck(deckId: number): Promise<Deck | null> {
  const db = await getDatabase();
  const row = await db.getFirstAsync('SELECT * FROM decks WHERE id = ?', deckId);
  return row ? rowToDeck(row as Record<string, unknown>) : null;
}

export async function listNotes(deckId: number): Promise<Note[]> {
  const db = await getDatabase();
  const rows = await db.getAllAsync(
    'SELECT * FROM notes WHERE deck_id = ? ORDER BY updated_at DESC',
    deckId,
  );
  return rows.map((row) => rowToNote(row as Record<string, unknown>));
}

export async function getNote(noteId: number): Promise<Note | null> {
  const db = await getDatabase();
  const row = await db.getFirstAsync('SELECT * FROM notes WHERE id = ?', noteId);
  return row ? rowToNote(row as Record<string, unknown>) : null;
}

export async function createNote(deckId: number, fields: NoteFields): Promise<Note> {
  const db = await getDatabase();
  const ts = now();
  const fieldsJson = JSON.stringify(fields);

  const noteResult = await db.runAsync(
    'INSERT INTO notes (deck_id, fields_json, created_at, updated_at) VALUES (?, ?, ?, ?)',
    deckId,
    fieldsJson,
    ts,
    ts,
  );

  const noteId = noteResult.lastInsertRowId;
  await db.runAsync(
    `INSERT INTO cards (note_id, due, interval, ease_factor, repetitions, lapses, queue, created_at, updated_at)
     VALUES (?, ?, 0, 2.5, 0, 0, 'new', ?, ?)`,
    noteId,
    ts,
    ts,
    ts,
  );

  return {
    id: noteId,
    deckId,
    fields,
    createdAt: ts,
    updatedAt: ts,
  };
}

export async function updateNote(noteId: number, fields: NoteFields): Promise<Note | null> {
  const db = await getDatabase();
  const ts = now();
  await db.runAsync(
    'UPDATE notes SET fields_json = ?, updated_at = ? WHERE id = ?',
    JSON.stringify(fields),
    ts,
    noteId,
  );
  return getNote(noteId);
}

export async function getDeckStats(deckId: number): Promise<{ newCount: number; dueCount: number; total: number }> {
  const db = await getDatabase();
  const ts = now();
  const endOfDay = startOfDay(ts) + 86_400_000 - 1;

  const rows = await db.getAllAsync<{ queue: CardQueue; due: number; count: number }>(
    `SELECT c.queue, c.due, COUNT(*) as count
     FROM cards c
     JOIN notes n ON n.id = c.note_id
     WHERE n.deck_id = ?
     GROUP BY c.queue, c.due`,
    deckId,
  );

  let newCount = 0;
  let dueCount = 0;
  let total = 0;

  for (const row of rows) {
    total += row.count;
    if (row.queue === 'new') {
      newCount += row.count;
    } else if (row.due <= endOfDay) {
      dueCount += row.count;
    }
  }

  return { newCount, dueCount, total };
}

export async function getDueCards(deckId: number, limit = 20): Promise<CardWithNote[]> {
  const db = await getDatabase();
  const ts = now();
  const endOfDay = startOfDay(ts) + 86_400_000 - 1;

  const rows = await db.getAllAsync(
    `SELECT c.*, n.id as n_id, n.deck_id, n.fields_json, n.created_at as n_created_at, n.updated_at as n_updated_at
     FROM cards c
     JOIN notes n ON n.id = c.note_id
     WHERE n.deck_id = ?
       AND (c.queue = 'new' OR c.due <= ?)
     ORDER BY
       CASE c.queue WHEN 'learning' THEN 0 WHEN 'review' THEN 1 WHEN 'new' THEN 2 ELSE 3 END,
       c.due ASC
     LIMIT ?`,
    deckId,
    endOfDay,
    limit,
  );

  return rows.map((row) => {
    const r = row as Record<string, unknown>;
    const card = rowToCard(r);
    const note: Note = {
      id: r.n_id as number,
      deckId: r.deck_id as number,
      fields: parseNoteFields(r.fields_json as string),
      createdAt: r.n_created_at as number,
      updatedAt: r.n_updated_at as number,
    };
    return { ...card, note };
  });
}

export async function reviewCard(cardId: number, rating: ReviewRating): Promise<Card | null> {
  const db = await getDatabase();
  const row = await db.getFirstAsync('SELECT * FROM cards WHERE id = ?', cardId);
  if (!row) return null;

  const card = rowToCard(row as Record<string, unknown>);
  const intervalBefore = card.interval;
  const result = scheduleReview(card, rating);
  const ts = now();

  await db.runAsync(
    `UPDATE cards
     SET due = ?, interval = ?, ease_factor = ?, repetitions = ?, lapses = ?, queue = ?, updated_at = ?
     WHERE id = ?`,
    result.due,
    result.interval,
    result.easeFactor,
    result.repetitions,
    result.lapses,
    result.queue,
    ts,
    cardId,
  );

  await db.runAsync(
    `INSERT INTO review_log (card_id, rating, interval_before, interval_after, reviewed_at)
     VALUES (?, ?, ?, ?, ?)`,
    cardId,
    rating,
    intervalBefore,
    result.interval,
    ts,
  );

  return {
    ...card,
    ...result,
    updatedAt: ts,
  };
}

export function cardIsDue(card: Card, at = Date.now()): boolean {
  return isDue(card, at);
}

export type { ReviewLog };

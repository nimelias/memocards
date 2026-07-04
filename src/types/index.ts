export type CardQueue = 'new' | 'learning' | 'review';

export type ReviewRating = 1 | 2 | 3 | 4;

export interface Deck {
  id: number;
  name: string;
  parentId: number | null;
  studyDays: number | null;
  minRepetitions: number;
  studyStartAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface DeckSettings {
  studyDays: number | null;
  minRepetitions: number;
}

export interface NoteFields {
  front: string;
  back: string;
}

export interface Note {
  id: number;
  deckId: number;
  fields: NoteFields;
  createdAt: number;
  updatedAt: number;
}

export interface Card {
  id: number;
  noteId: number;
  due: number;
  interval: number;
  easeFactor: number;
  repetitions: number;
  lapses: number;
  queue: CardQueue;
  createdAt: number;
  updatedAt: number;
}

export interface CardWithNote extends Card {
  note: Note;
}

export interface ReviewLog {
  id: number;
  cardId: number;
  rating: ReviewRating;
  intervalBefore: number;
  intervalAfter: number;
  reviewedAt: number;
}

export type RootStackParamList = {
  DeckList: undefined;
  DeckDetail: { deckId: number; deckName: string };
  NoteEditor: { deckId: number; noteId?: number };
  Review: { deckId: number; deckName: string };
};

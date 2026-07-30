import * as DocumentPicker from 'expo-document-picker';
import {
  cacheDirectory,
  readAsStringAsync,
  writeAsStringAsync,
  EncodingType,
} from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import { exportDecks, importDecks, type ExportPayload } from '../db';
import { deleteCardImage, readImageAsBase64, writeImageFromBase64 } from './card-images';
import type { CardQueue, NoteFields } from '../types';

const EXPORT_VERSION = 1;

type ExportNoteFields = NoteFields & {
  frontImageBase64?: string;
  frontImageMime?: string;
  backImageBase64?: string;
  backImageMime?: string;
};

type SerializablePayload = {
  version: number;
  exportedAt: number;
  decks: Array<{
    name: string;
    studyDays: number | null;
    minRepetitions: number;
    notes: Array<{
      fields: ExportNoteFields;
      card: {
        due: number;
        interval: number;
        easeFactor: number;
        repetitions: number;
        lapses: number;
        queue: CardQueue;
      };
    }>;
  }>;
};

async function fieldsToExport(fields: NoteFields): Promise<ExportNoteFields> {
  const out: ExportNoteFields = { front: fields.front, back: fields.back };
  if (fields.frontImage) {
    out.frontImageBase64 = await readImageAsBase64(fields.frontImage);
    out.frontImageMime = guessMime(fields.frontImage);
  }
  if (fields.backImage) {
    out.backImageBase64 = await readImageAsBase64(fields.backImage);
    out.backImageMime = guessMime(fields.backImage);
  }
  return out;
}

function guessMime(uri: string): string {
  if (uri.endsWith('.png')) return 'image/png';
  if (uri.endsWith('.webp')) return 'image/webp';
  return 'image/jpeg';
}

async function fieldsFromImport(raw: ExportNoteFields): Promise<NoteFields> {
  const fields: NoteFields = { front: raw.front ?? '', back: raw.back ?? '' };
  if (raw.frontImageBase64) {
    fields.frontImage = await writeImageFromBase64(raw.frontImageBase64, raw.frontImageMime ?? 'image/jpeg');
  }
  if (raw.backImageBase64) {
    fields.backImage = await writeImageFromBase64(raw.backImageBase64, raw.backImageMime ?? 'image/jpeg');
  }
  return fields;
}

export async function buildExportPayload(deckId?: number): Promise<SerializablePayload> {
  const data = await exportDecks(deckId);
  const decks = await Promise.all(
    data.decks.map(async (deck) => ({
      name: deck.name,
      studyDays: deck.studyDays,
      minRepetitions: deck.minRepetitions,
      notes: await Promise.all(
        deck.notes.map(async (note) => ({
          fields: await fieldsToExport(note.fields),
          card: note.card,
        })),
      ),
    })),
  );
  return { version: EXPORT_VERSION, exportedAt: Date.now(), decks };
}

export async function shareExportJson(deckId?: number) {
  const payload = await buildExportPayload(deckId);
  const json = JSON.stringify(payload, null, 2);
  const name = deckId ? `memocards-deck-${deckId}.json` : 'memocards-export.json';
  const path = `${cacheDirectory}${name}`;
  await writeAsStringAsync(path, json, { encoding: EncodingType.UTF8 });
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(path, { mimeType: 'application/json', dialogTitle: 'Exportar MemoCards' });
  }
}

export async function pickAndImportJson(): Promise<number> {
  const picked = await DocumentPicker.getDocumentAsync({
    type: 'application/json',
    copyToCacheDirectory: true,
  });
  if (picked.canceled || !picked.assets[0]) return 0;

  const raw = await readAsStringAsync(picked.assets[0].uri, {
    encoding: EncodingType.UTF8,
  });
  const payload = JSON.parse(raw) as SerializablePayload;
  if (!payload.decks?.length) return 0;

  const importPayload: ExportPayload = {
    decks: await Promise.all(
      payload.decks.map(async (deck) => ({
        name: deck.name,
        studyDays: deck.studyDays,
        minRepetitions: deck.minRepetitions,
        notes: await Promise.all(
          deck.notes.map(async (note) => ({
            fields: await fieldsFromImport(note.fields),
            card: note.card,
          })),
        ),
      })),
    ),
  };

  return importDecks(importPayload);
}

export async function cleanupNoteImages(fields: NoteFields) {
  await deleteCardImage(fields.frontImage);
  await deleteCardImage(fields.backImage);
}

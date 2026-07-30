import {
  copyAsync,
  deleteAsync,
  documentDirectory,
  getInfoAsync,
  makeDirectoryAsync,
  readAsStringAsync,
  writeAsStringAsync,
  EncodingType,
} from 'expo-file-system/legacy';
import * as ImagePicker from 'expo-image-picker';

const IMAGE_DIR = `${documentDirectory}card-images/`;

async function ensureImageDir() {
  const info = await getInfoAsync(IMAGE_DIR);
  if (!info.exists) {
    await makeDirectoryAsync(IMAGE_DIR, { intermediates: true });
  }
}

export async function pickCardImage(): Promise<string | null> {
  const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!perm.granted) return null;

  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    quality: 0.85,
  });
  if (result.canceled || !result.assets[0]) return null;

  return copyImageToStorage(result.assets[0].uri);
}

export async function copyImageToStorage(sourceUri: string): Promise<string> {
  await ensureImageDir();
  const ext = sourceUri.split('.').pop()?.split('?')[0] || 'jpg';
  const dest = `${IMAGE_DIR}${Date.now()}-${Math.random().toString(36).slice(2)}.${ext}`;
  await copyAsync({ from: sourceUri, to: dest });
  return dest;
}

export async function deleteCardImage(uri: string | undefined) {
  if (!uri || !uri.startsWith(IMAGE_DIR)) return;
  const info = await getInfoAsync(uri);
  if (info.exists) {
    await deleteAsync(uri, { idempotent: true });
  }
}

export async function readImageAsBase64(uri: string): Promise<string> {
  return readAsStringAsync(uri, { encoding: EncodingType.Base64 });
}

export async function writeImageFromBase64(base64: string, mime = 'image/jpeg'): Promise<string> {
  await ensureImageDir();
  const ext = mime.includes('png') ? 'png' : mime.includes('webp') ? 'webp' : 'jpg';
  const dest = `${IMAGE_DIR}import-${Date.now()}-${Math.random().toString(36).slice(2)}.${ext}`;
  await writeAsStringAsync(dest, base64, { encoding: EncodingType.Base64 });
  return dest;
}

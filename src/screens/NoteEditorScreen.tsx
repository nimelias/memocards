import { useCallback, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { createNote } from '../db';
import type { RootStackParamList } from '../types';
import { CardImage } from '../components/CardImage';
import { ScreenContainer, ScreenTitle } from '../components/ui';
import { deleteCardImage, pickCardImage } from '../lib/card-images';

type Props = NativeStackScreenProps<RootStackParamList, 'NoteEditor'>;

export function NoteEditorScreen({ navigation, route }: Props) {
  const { deckId } = route.params;
  const [front, setFront] = useState('');
  const [back, setBack] = useState('');
  const [frontImage, setFrontImage] = useState<string | undefined>();
  const [backImage, setBackImage] = useState<string | undefined>();
  const [saving, setSaving] = useState(false);

  async function attachImage(side: 'front' | 'back') {
    const uri = await pickCardImage();
    if (!uri) return;
    if (side === 'front') {
      if (frontImage) await deleteCardImage(frontImage);
      setFrontImage(uri);
    } else {
      if (backImage) await deleteCardImage(backImage);
      setBackImage(uri);
    }
  }

  async function removeImage(side: 'front' | 'back') {
    if (side === 'front') {
      await deleteCardImage(frontImage);
      setFrontImage(undefined);
    } else {
      await deleteCardImage(backImage);
      setBackImage(undefined);
    }
  }

  const save = useCallback(async () => {
    if (!front.trim() && !back.trim() && !frontImage && !backImage) {
      Alert.alert('Tarjeta vacía', 'Escribe texto o añade una imagen.');
      return;
    }
    setSaving(true);
    try {
      const fields = {
        front: front.trim(),
        back: back.trim(),
        frontImage,
        backImage,
      };
      await createNote(deckId, fields);
      navigation.goBack();
    } finally {
      setSaving(false);
    }
  }, [back, backImage, deckId, front, frontImage, navigation]);

  return (
    <ScreenContainer scroll>
      <ScreenTitle>Nueva tarjeta</ScreenTitle>

      <Text style={styles.label}>Frente</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        multiline
        value={front}
        onChangeText={setFront}
        placeholder="Pregunta o término"
      />
      <ImageActions
        hasImage={!!frontImage}
        onAttach={() => attachImage('front')}
        onRemove={() => removeImage('front')}
      />
      {frontImage && <CardImage uri={frontImage} maxHeight={160} />}

      <Text style={styles.label}>Reverso</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        multiline
        value={back}
        onChangeText={setBack}
        placeholder="Respuesta o definición"
      />
      <ImageActions
        hasImage={!!backImage}
        onAttach={() => attachImage('back')}
        onRemove={() => removeImage('back')}
      />
      {backImage && <CardImage uri={backImage} maxHeight={160} />}

      <Pressable style={styles.saveBtn} onPress={save} disabled={saving}>
        <Text style={styles.saveBtnText}>{saving ? 'Guardando…' : 'Guardar'}</Text>
      </Pressable>
    </ScreenContainer>
  );
}

function ImageActions({
  hasImage,
  onAttach,
  onRemove,
}: {
  hasImage: boolean;
  onAttach: () => void;
  onRemove: () => void;
}) {
  return (
    <View style={styles.imageActions}>
      <Pressable style={styles.imageBtn} onPress={onAttach}>
        <Text style={styles.imageBtnText}>{hasImage ? 'Cambiar imagen' : 'Añadir imagen'}</Text>
      </Pressable>
      {hasImage && (
        <Pressable style={styles.imageRemoveBtn} onPress={onRemove}>
          <Text style={styles.imageRemoveText}>Quitar</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#64748b',
    marginBottom: 6,
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 10,
    padding: 12,
    fontSize: 16,
    marginBottom: 8,
  },
  multiline: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  imageActions: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  imageBtn: {
    backgroundColor: '#f1f5f9',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  imageBtnText: {
    color: '#2563eb',
    fontWeight: '600',
    fontSize: 14,
  },
  imageRemoveBtn: {
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  imageRemoveText: {
    color: '#dc2626',
    fontWeight: '600',
    fontSize: 14,
  },
  saveBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  saveBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});

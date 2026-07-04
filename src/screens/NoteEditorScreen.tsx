import { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { createNote, getNote, updateNote } from '../db';
import type { RootStackParamList } from '../types';
import { ScreenContainer, ScreenTitle } from '../components/ui';

type Props = NativeStackScreenProps<RootStackParamList, 'NoteEditor'>;

export function NoteEditorScreen({ navigation, route }: Props) {
  const { deckId, noteId } = route.params;
  const isEdit = noteId != null;
  const [front, setFront] = useState('');
  const [back, setBack] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!noteId) return;
    getNote(noteId).then((note) => {
      if (!note) return;
      setFront(note.fields.front);
      setBack(note.fields.back);
    });
  }, [noteId]);

  const save = useCallback(async () => {
    if (!front.trim() && !back.trim()) {
      Alert.alert('Tarjeta vacía', 'Escribe al menos frente o reverso.');
      return;
    }
    setSaving(true);
    try {
      const fields = { front: front.trim(), back: back.trim() };
      if (isEdit && noteId) {
        await updateNote(noteId, fields);
      } else {
        await createNote(deckId, fields);
      }
      navigation.goBack();
    } finally {
      setSaving(false);
    }
  }, [back, deckId, front, isEdit, navigation, noteId]);

  return (
    <ScreenContainer scroll>
      <ScreenTitle>{isEdit ? 'Editar tarjeta' : 'Nueva tarjeta'}</ScreenTitle>

      <Text style={styles.label}>Frente</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        multiline
        value={front}
        onChangeText={setFront}
        placeholder="Pregunta o término"
      />

      <Text style={styles.label}>Reverso</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        multiline
        value={back}
        onChangeText={setBack}
        placeholder="Respuesta o definición"
      />

      <Pressable style={styles.saveBtn} onPress={save} disabled={saving}>
        <Text style={styles.saveBtnText}>{saving ? 'Guardando…' : 'Guardar'}</Text>
      </Pressable>
    </ScreenContainer>
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
    marginBottom: 16,
  },
  multiline: {
    minHeight: 100,
    textAlignVertical: 'top',
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

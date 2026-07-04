import { useCallback, useState } from 'react';
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { createDeck, listDecks } from '../db';
import type { Deck, RootStackParamList } from '../types';
import { EmptyState, ScreenContainer, ScreenTitle } from '../components/ui';
import { useFocusEffect } from '@react-navigation/native';
import { pickAndImportJson, shareExportJson } from '../lib/export-import';

type Props = NativeStackScreenProps<RootStackParamList, 'DeckList'>;

export function DeckListScreen({ navigation }: Props) {
  const [decks, setDecks] = useState<Deck[]>([]);
  const [name, setName] = useState('');

  const load = useCallback(async () => {
    setDecks(await listDecks());
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleCreate() {
    const trimmed = name.trim();
    if (!trimmed) return;
    await createDeck(trimmed);
    setName('');
    await load();
  }

  async function handleExport() {
    try {
      await shareExportJson();
    } catch {
      Alert.alert('Error', 'No se pudo exportar.');
    }
  }

  async function handleImport() {
    try {
      const count = await pickAndImportJson();
      await load();
      Alert.alert('Importación', count > 0 ? `Se importaron ${count} tarjetas.` : 'No se importó nada.');
    } catch {
      Alert.alert('Error', 'No se pudo importar el archivo JSON.');
    }
  }

  return (
    <ScreenContainer>
      <ScreenTitle>Mazos</ScreenTitle>

      <View style={styles.form}>
        <TextInput
          style={styles.input}
          placeholder="Nombre del mazo"
          value={name}
          onChangeText={setName}
          onSubmitEditing={handleCreate}
        />
        <Pressable style={styles.primaryBtn} onPress={handleCreate}>
          <Text style={styles.primaryBtnText}>Crear</Text>
        </Pressable>
      </View>

      <View style={styles.ioRow}>
        <Pressable style={styles.ioBtn} onPress={handleExport}>
          <Text style={styles.ioBtnText}>Exportar JSON</Text>
        </Pressable>
        <Pressable style={styles.ioBtn} onPress={handleImport}>
          <Text style={styles.ioBtnText}>Importar JSON</Text>
        </Pressable>
      </View>

      <FlatList
        data={decks}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={<EmptyState message="Aún no hay mazos. Crea el primero arriba." />}
        renderItem={({ item }) => (
          <Pressable
            style={styles.deckRow}
            onPress={() => navigation.navigate('DeckDetail', { deckId: item.id, deckName: item.name })}
          >
            <Text style={styles.deckName}>{item.name}</Text>
          </Pressable>
        )}
      />
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  form: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  input: {
    flex: 1,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  primaryBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 10,
    paddingHorizontal: 16,
    justifyContent: 'center',
  },
  primaryBtnText: {
    color: '#fff',
    fontWeight: '600',
  },
  ioRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  ioBtn: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  ioBtnText: {
    color: '#2563eb',
    fontWeight: '600',
    fontSize: 14,
  },
  deckRow: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  deckName: {
    fontSize: 17,
    fontWeight: '600',
    color: '#0f172a',
  },
});

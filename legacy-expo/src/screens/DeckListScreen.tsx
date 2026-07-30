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
import { useUiSettings } from '../context/UiSettingsProvider';

type Props = NativeStackScreenProps<RootStackParamList, 'DeckList'>;

export function DeckListScreen({ navigation }: Props) {
  const [decks, setDecks] = useState<Deck[]>([]);
  const [name, setName] = useState('');
  const { settings, theme, toggleDarkLight } = useUiSettings();

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
      <View style={styles.titleRow}>
        <ScreenTitle>Mazos</ScreenTitle>
        <View style={styles.titleActions}>
          <Pressable
            style={[styles.iconBtn, { borderColor: theme.border, backgroundColor: theme.card }]}
            onPress={() => void toggleDarkLight()}
          >
            <Text style={{ color: theme.text, fontSize: 14 }}>
              {settings.theme === 'dark' ? '☀' : '☾'}
            </Text>
          </Pressable>
          <Pressable
            style={[styles.iconBtn, { borderColor: theme.border, backgroundColor: theme.card }]}
            onPress={() => navigation.navigate('Settings')}
          >
            <Text style={{ color: theme.text, fontSize: 14 }}>⚙</Text>
          </Pressable>
        </View>
      </View>

      <View style={styles.form}>
        <TextInput
          style={[styles.input, { backgroundColor: theme.card, borderColor: theme.border, color: theme.text }]}
          placeholder="Nombre del mazo"
          placeholderTextColor={theme.muted}
          value={name}
          onChangeText={setName}
          onSubmitEditing={handleCreate}
        />
        <Pressable style={[styles.primaryBtn, { backgroundColor: theme.primary }]} onPress={handleCreate}>
          <Text style={styles.primaryBtnText}>Crear</Text>
        </Pressable>
      </View>

      <View style={styles.ioRow}>
        <Pressable style={[styles.ioBtn, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={handleExport}>
          <Text style={[styles.ioBtnText, { color: theme.primary }]}>Exportar JSON</Text>
        </Pressable>
        <Pressable style={[styles.ioBtn, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={handleImport}>
          <Text style={[styles.ioBtnText, { color: theme.primary }]}>Importar JSON</Text>
        </Pressable>
      </View>

      <FlatList
        data={decks}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={<EmptyState message="Aún no hay mazos. Crea el primero arriba." />}
        renderItem={({ item }) => (
          <Pressable
            style={[styles.deckRow, { backgroundColor: theme.card, borderColor: theme.border }]}
            onPress={() => navigation.navigate('DeckDetail', { deckId: item.id, deckName: item.name })}
          >
            <Text style={[styles.deckName, { color: theme.text }]}>{item.name}</Text>
          </Pressable>
        )}
      />
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  titleActions: {
    flexDirection: 'row',
    gap: 8,
  },
  iconBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  form: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  input: {
    flex: 1,
    borderWidth: 1,
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
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    borderWidth: 1,
  },
  ioBtnText: {
    color: '#2563eb',
    fontWeight: '600',
    fontSize: 14,
  },
  deckRow: {
    borderRadius: 12,
    padding: 16,
    marginBottom: 8,
    borderWidth: 1,
  },
  deckName: {
    fontSize: 17,
    fontWeight: '600',
  },
});

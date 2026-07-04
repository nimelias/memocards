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
import { useFocusEffect } from '@react-navigation/native';
import { getDeck, getDeckStats, listNotes, resetDeck, updateDeckSettings } from '../db';
import type { Note, RootStackParamList } from '../types';
import { EmptyState, ScreenContainer, ScreenTitle } from '../components/ui';

type Props = NativeStackScreenProps<RootStackParamList, 'DeckDetail'>;

export function DeckDetailScreen({ navigation, route }: Props) {
  const { deckId, deckName } = route.params;
  const [notes, setNotes] = useState<Note[]>([]);
  const [stats, setStats] = useState({ newCount: 0, dueCount: 0, total: 0 });
  const [studyDays, setStudyDays] = useState('');
  const [minRepetitions, setMinRepetitions] = useState('1');
  const [showSettings, setShowSettings] = useState(false);

  const load = useCallback(async () => {
    const deck = await getDeck(deckId);
    setNotes(await listNotes(deckId));
    setStats(await getDeckStats(deckId));
    if (deck) {
      setStudyDays(deck.studyDays ? String(deck.studyDays) : '');
      setMinRepetitions(String(deck.minRepetitions));
    }
  }, [deckId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const canReview = stats.newCount + stats.dueCount > 0;

  async function handleSaveSettings() {
    const days = studyDays.trim() ? parseInt(studyDays, 10) : null;
    const minRep = parseInt(minRepetitions, 10) || 1;
    if (days !== null && (days < 1 || days > 365)) {
      Alert.alert('Días inválidos', 'Indica un número entre 1 y 365, o déjalo vacío.');
      return;
    }
    await updateDeckSettings(deckId, { studyDays: days, minRepetitions: minRep });
    await load();
    Alert.alert('Guardado', 'Configuración del mazo actualizada.');
  }

  function handleReset() {
    Alert.alert(
      'Resetear mazo',
      'Todas las tarjetas volverán al estado inicial y se borrará el historial de repaso. ¿Continuar?',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Resetear',
          style: 'destructive',
          onPress: async () => {
            await resetDeck(deckId);
            await load();
          },
        },
      ],
    );
  }

  return (
    <ScreenContainer>
      <ScreenTitle>{deckName}</ScreenTitle>

      <View style={styles.stats}>
        <Stat label="Nuevas" value={stats.newCount} />
        <Stat label="Repaso" value={stats.dueCount} />
        <Stat label="Total" value={stats.total} />
      </View>

      <Pressable
        style={[styles.reviewBtn, !canReview && styles.reviewBtnDisabled]}
        disabled={!canReview}
        onPress={() => navigation.navigate('Review', { deckId, deckName })}
      >
        <Text style={styles.reviewBtnText}>Estudiar</Text>
      </Pressable>

      <Pressable
        style={styles.secondaryBtn}
        onPress={() => navigation.navigate('NoteEditor', { deckId })}
      >
        <Text style={styles.secondaryBtnText}>Nueva tarjeta</Text>
      </Pressable>

      <Pressable style={styles.settingsToggle} onPress={() => setShowSettings((v) => !v)}>
        <Text style={styles.settingsToggleText}>
          {showSettings ? 'Ocultar configuración' : 'Configuración del mazo'}
        </Text>
      </Pressable>

      {showSettings && (
        <View style={styles.settings}>
          <Text style={styles.settingsLabel}>Días de estudio (vacío = sin límite)</Text>
          <TextInput
            style={styles.input}
            placeholder="Ej. 30"
            keyboardType="number-pad"
            value={studyDays}
            onChangeText={setStudyDays}
          />
          <Text style={styles.settingsHint}>
            Las tarjetas no se programarán más allá del último día del periodo.
          </Text>

          <Text style={styles.settingsLabel}>Repeticiones mínimas por tarjeta</Text>
          <TextInput
            style={styles.input}
            placeholder="1"
            keyboardType="number-pad"
            value={minRepetitions}
            onChangeText={setMinRepetitions}
          />
          <Text style={styles.settingsHint}>
            Durante el periodo, las tarjetas con menos repeticiones seguirán en cola.
          </Text>

          <Pressable style={styles.saveBtn} onPress={handleSaveSettings}>
            <Text style={styles.saveBtnText}>Guardar configuración</Text>
          </Pressable>

          <Pressable style={styles.resetBtn} onPress={handleReset}>
            <Text style={styles.resetBtnText}>Resetear mazo</Text>
          </Pressable>
        </View>
      )}

      <Text style={styles.sectionTitle}>Tarjetas</Text>
      <FlatList
        data={notes}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={<EmptyState message="Este mazo no tiene tarjetas." />}
        renderItem={({ item }) => (
          <Pressable
            style={styles.noteRow}
            onPress={() => navigation.navigate('NoteEditor', { deckId, noteId: item.id })}
          >
            <Text style={styles.noteFront} numberOfLines={2}>{item.fields.front || '(sin frente)'}</Text>
            <Text style={styles.noteBack} numberOfLines={1}>{item.fields.back || '(sin reverso)'}</Text>
          </Pressable>
        )}
      />
    </ScreenContainer>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  stats: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  stat: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  statValue: {
    fontSize: 22,
    fontWeight: '700',
    color: '#2563eb',
  },
  statLabel: {
    fontSize: 12,
    color: '#64748b',
    marginTop: 2,
  },
  reviewBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginBottom: 8,
  },
  reviewBtnDisabled: {
    opacity: 0.5,
  },
  reviewBtnText: {
    color: '#fff',
    fontSize: 17,
    fontWeight: '700',
  },
  secondaryBtn: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    alignItems: 'center',
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  secondaryBtnText: {
    color: '#0f172a',
    fontSize: 16,
    fontWeight: '600',
  },
  settingsToggle: {
    marginBottom: 12,
  },
  settingsToggleText: {
    color: '#2563eb',
    fontSize: 15,
    fontWeight: '600',
  },
  settings: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  settingsLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0f172a',
    marginBottom: 6,
  },
  settingsHint: {
    fontSize: 12,
    color: '#64748b',
    marginBottom: 12,
  },
  input: {
    backgroundColor: '#f8fafc',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    marginBottom: 8,
  },
  saveBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 10,
    padding: 14,
    alignItems: 'center',
    marginBottom: 8,
  },
  saveBtnText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 15,
  },
  resetBtn: {
    backgroundColor: '#fef2f2',
    borderRadius: 10,
    padding: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#fecaca',
  },
  resetBtnText: {
    color: '#dc2626',
    fontWeight: '600',
    fontSize: 15,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#64748b',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  noteRow: {
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 14,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  noteFront: {
    fontSize: 16,
    fontWeight: '600',
    color: '#0f172a',
    marginBottom: 4,
  },
  noteBack: {
    fontSize: 14,
    color: '#64748b',
  },
});

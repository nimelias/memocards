import { useCallback, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useFocusEffect } from '@react-navigation/native';
import { getDeckStats, listNotes } from '../db';
import type { Note, RootStackParamList } from '../types';
import { EmptyState, ScreenContainer, ScreenTitle } from '../components/ui';

type Props = NativeStackScreenProps<RootStackParamList, 'DeckDetail'>;

export function DeckDetailScreen({ navigation, route }: Props) {
  const { deckId, deckName } = route.params;
  const [notes, setNotes] = useState<Note[]>([]);
  const [stats, setStats] = useState({ newCount: 0, dueCount: 0, total: 0 });

  const load = useCallback(async () => {
    setNotes(await listNotes(deckId));
    setStats(await getDeckStats(deckId));
  }, [deckId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const canReview = stats.newCount + stats.dueCount > 0;

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
    marginBottom: 20,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  secondaryBtnText: {
    color: '#0f172a',
    fontSize: 16,
    fontWeight: '600',
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

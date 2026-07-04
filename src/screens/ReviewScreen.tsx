import { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { getDueCards, reviewCard } from '../db';
import type { CardWithNote, ReviewRating, RootStackParamList } from '../types';
import { CardImage } from '../components/CardImage';
import { EmptyState, ScreenContainer } from '../components/ui';

type Props = NativeStackScreenProps<RootStackParamList, 'Review'>;

const RATINGS: { rating: ReviewRating; label: string; color: string }[] = [
  { rating: 1, label: 'Otra vez', color: '#dc2626' },
  { rating: 2, label: 'Difícil', color: '#ea580c' },
  { rating: 3, label: 'Bien', color: '#16a34a' },
  { rating: 4, label: 'Fácil', color: '#2563eb' },
];

export function ReviewScreen({ navigation, route }: Props) {
  const { deckId, deckName } = route.params;
  const [queue, setQueue] = useState<CardWithNote[]>([]);
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [finished, setFinished] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const cards = await getDueCards(deckId, 50);
    setQueue(cards);
    setIndex(0);
    setRevealed(false);
    setFinished(cards.length === 0);
    setLoading(false);
  }, [deckId]);

  useEffect(() => {
    load();
  }, [load]);

  const current = queue[index];

  async function handleRate(rating: ReviewRating) {
    if (!current) return;
    await reviewCard(current.id, rating);

    if (index + 1 >= queue.length) {
      setFinished(true);
      return;
    }

    setIndex((i) => i + 1);
    setRevealed(false);
  }

  if (loading) {
    return (
      <ScreenContainer>
        <EmptyState message="Cargando tarjetas…" />
      </ScreenContainer>
    );
  }

  if (finished || !current) {
    return (
      <ScreenContainer>
        <View style={styles.done}>
          <Text style={styles.doneTitle}>Sesión completada</Text>
          <Text style={styles.doneSubtitle}>{deckName}</Text>
          <Pressable style={styles.doneBtn} onPress={() => navigation.goBack()}>
            <Text style={styles.doneBtnText}>Volver al mazo</Text>
          </Pressable>
        </View>
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <View style={styles.layout}>
        <Text style={styles.progress}>
          {index + 1} / {queue.length}
        </Text>

        <View style={styles.card}>
          <ScrollView style={styles.cardScroll} contentContainerStyle={styles.cardContent}>
            <Text style={styles.cardLabel}>{revealed ? 'Reverso' : 'Frente'}</Text>
            {revealed ? (
              <>
                {current.note.fields.back ? (
                  <Text style={styles.cardText}>{current.note.fields.back}</Text>
                ) : null}
                {current.note.fields.backImage ? (
                  <CardImage uri={current.note.fields.backImage} maxHeight={240} />
                ) : null}
              </>
            ) : (
              <>
                {current.note.fields.front ? (
                  <Text style={styles.cardText}>{current.note.fields.front}</Text>
                ) : null}
                {current.note.fields.frontImage ? (
                  <CardImage uri={current.note.fields.frontImage} maxHeight={240} />
                ) : null}
              </>
            )}
          </ScrollView>
        </View>

        <View style={styles.footer}>
          {!revealed ? (
            <Pressable style={styles.revealBtn} onPress={() => setRevealed(true)}>
              <Text style={styles.revealBtnText}>Mostrar respuesta</Text>
            </Pressable>
          ) : (
            <View style={styles.ratingRow}>
              {RATINGS.map(({ rating, label, color }) => (
                <Pressable
                  key={rating}
                  style={[styles.ratingBtn, { backgroundColor: color }]}
                  onPress={() => handleRate(rating)}
                >
                  <Text style={styles.ratingBtnText}>{label}</Text>
                </Pressable>
              ))}
            </View>
          )}
        </View>
      </View>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  layout: {
    flex: 1,
  },
  progress: {
    textAlign: 'center',
    color: '#64748b',
    marginBottom: 12,
    fontSize: 14,
  },
  card: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    overflow: 'hidden',
  },
  cardScroll: {
    flex: 1,
  },
  cardContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },
  cardLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#94a3b8',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 12,
  },
  cardText: {
    fontSize: 22,
    lineHeight: 32,
    color: '#0f172a',
  },
  footer: {
    paddingTop: 16,
  },
  revealBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
  },
  revealBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  ratingRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  ratingBtn: {
    flexGrow: 1,
    flexBasis: '45%',
    borderRadius: 10,
    padding: 14,
    alignItems: 'center',
  },
  ratingBtnText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 14,
  },
  done: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  doneTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#0f172a',
    marginBottom: 8,
  },
  doneSubtitle: {
    fontSize: 16,
    color: '#64748b',
    marginBottom: 24,
  },
  doneBtn: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    paddingHorizontal: 24,
    paddingVertical: 14,
  },
  doneBtnText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 16,
  },
});

import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ScreenContainer, ScreenTitle } from '../components/ui';
import { useUiSettings } from '../context/UiSettingsProvider';
import type { ThemeName } from '../types';

const FONT_LEVELS = [
  { label: 'Pequeña', value: 0.95 },
  { label: 'Normal', value: 1.0 },
  { label: 'Grande', value: 1.15 },
  { label: 'Muy grande', value: 1.3 },
];

const THEMES: Array<{ id: ThemeName; label: string }> = [
  { id: 'light', label: 'Claro' },
  { id: 'dark', label: 'Oscuro' },
  { id: 'sand', label: 'Arena' },
];

export function SettingsScreen() {
  const { settings, theme, setTheme, setFontScale } = useUiSettings();
  return (
    <ScreenContainer scroll>
      <ScreenTitle>Ajustes</ScreenTitle>

      <Text style={[styles.sectionTitle, { color: theme.muted }]}>Tema</Text>
      <View style={styles.row}>
        {THEMES.map((t) => (
          <Pressable
            key={t.id}
            style={[
              styles.pill,
              { borderColor: theme.border, backgroundColor: theme.card },
              settings.theme === t.id && { borderColor: theme.primary },
            ]}
            onPress={() => void setTheme(t.id)}
          >
            <Text style={[styles.pillText, { color: theme.text }]}>{t.label}</Text>
          </Pressable>
        ))}
      </View>

      <Text style={[styles.sectionTitle, { color: theme.muted }]}>Tamaño de texto</Text>
      <View style={styles.row}>
        {FONT_LEVELS.map((f) => (
          <Pressable
            key={f.label}
            style={[
              styles.pill,
              { borderColor: theme.border, backgroundColor: theme.card },
              settings.fontScale === f.value && { borderColor: theme.primary },
            ]}
            onPress={() => void setFontScale(f.value)}
          >
            <Text style={[styles.pillText, { color: theme.text }]}>{f.label}</Text>
          </Pressable>
        ))}
      </View>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 18,
  },
  pill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  pillText: {
    fontSize: 14,
    fontWeight: '600',
  },
});

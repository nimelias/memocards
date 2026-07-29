import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useUiSettings } from '../context/UiSettingsProvider';

export function ScreenContainer({ children, scroll = false }: { children: React.ReactNode; scroll?: boolean }) {
  const insets = useSafeAreaInsets();
  const { theme } = useUiSettings();
  const style = [styles.container, { paddingBottom: Math.max(insets.bottom, 16), backgroundColor: theme.background }];

  if (scroll) {
    return (
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          style={styles.flex}
          contentContainerStyle={style}
          keyboardShouldPersistTaps="handled"
        >
          {children}
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  return <View style={[styles.flex, style]}>{children}</View>;
}

export function ScreenTitle({ children }: { children: string }) {
  const { settings, theme } = useUiSettings();
  return <Text style={[styles.title, { color: theme.text, fontSize: Math.round(24 * settings.fontScale) }]}>{children}</Text>;
}

export function EmptyState({ message }: { message: string }) {
  const { settings, theme } = useUiSettings();
  return (
    <View style={styles.empty}>
      <Text style={[styles.emptyText, { color: theme.muted, fontSize: Math.round(15 * settings.fontScale) }]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  container: {
    flexGrow: 1,
    padding: 16,
  },
  title: {
    fontWeight: '700',
    marginBottom: 16,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  emptyText: {
    textAlign: 'center',
  },
});

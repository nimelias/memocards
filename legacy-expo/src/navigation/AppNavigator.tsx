import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useUiSettings } from '../context/UiSettingsProvider';
import { DeckDetailScreen } from '../screens/DeckDetailScreen';
import { DeckListScreen } from '../screens/DeckListScreen';
import { NoteEditorScreen } from '../screens/NoteEditorScreen';
import { ReviewScreen } from '../screens/ReviewScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import type { RootStackParamList } from '../types';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigator() {
  const { theme, settings } = useUiSettings();
  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: theme.background },
          headerTintColor: theme.text,
          headerTitleStyle: { fontWeight: '600', color: theme.text, fontSize: Math.round(16 * settings.fontScale) },
          contentStyle: { backgroundColor: theme.background },
        }}
      >
        <Stack.Screen name="DeckList" component={DeckListScreen} options={{ title: 'MemoCards' }} />
        <Stack.Screen name="DeckDetail" component={DeckDetailScreen} options={{ title: 'Mazo' }} />
        <Stack.Screen name="NoteEditor" component={NoteEditorScreen} options={{ title: 'Tarjeta' }} />
        <Stack.Screen name="Settings" component={SettingsScreen} options={{ title: 'Ajustes' }} />
        <Stack.Screen name="Review" component={ReviewScreen} options={{ title: 'Repaso' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

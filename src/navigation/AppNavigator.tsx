import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { DeckDetailScreen } from '../screens/DeckDetailScreen';
import { DeckListScreen } from '../screens/DeckListScreen';
import { NoteEditorScreen } from '../screens/NoteEditorScreen';
import { ReviewScreen } from '../screens/ReviewScreen';
import type { RootStackParamList } from '../types';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: '#f8fafc' },
          headerTitleStyle: { fontWeight: '600' },
          contentStyle: { backgroundColor: '#f8fafc' },
        }}
      >
        <Stack.Screen name="DeckList" component={DeckListScreen} options={{ title: 'MemoCards' }} />
        <Stack.Screen name="DeckDetail" component={DeckDetailScreen} options={{ title: 'Mazo' }} />
        <Stack.Screen name="NoteEditor" component={NoteEditorScreen} options={{ title: 'Tarjeta' }} />
        <Stack.Screen name="Review" component={ReviewScreen} options={{ title: 'Repaso' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

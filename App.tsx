import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { DbProvider } from './src/context/DbProvider';
import { AppNavigator } from './src/navigation/AppNavigator';

export default function App() {
  return (
    <SafeAreaProvider>
      <DbProvider>
        <AppNavigator />
        <StatusBar style="auto" />
      </DbProvider>
    </SafeAreaProvider>
  );
}

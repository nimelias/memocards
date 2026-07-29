import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { DbProvider } from './src/context/DbProvider';
import { UiSettingsProvider, useUiSettings } from './src/context/UiSettingsProvider';
import { AppNavigator } from './src/navigation/AppNavigator';

function ThemedStatusBar() {
  const { settings } = useUiSettings();
  return <StatusBar style={settings.theme === 'dark' ? 'light' : 'dark'} />;
}

export default function App() {
  return (
    <SafeAreaProvider>
      <DbProvider>
        <UiSettingsProvider>
          <AppNavigator />
          <ThemedStatusBar />
        </UiSettingsProvider>
      </DbProvider>
    </SafeAreaProvider>
  );
}

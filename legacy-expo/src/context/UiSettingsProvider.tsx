import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { getUiSettings, saveUiSettings } from '../db';
import type { ThemeName, UiSettings } from '../types';

type ThemePalette = {
  name: ThemeName;
  background: string;
  card: string;
  text: string;
  muted: string;
  border: string;
  primary: string;
};

const THEMES: Record<ThemeName, ThemePalette> = {
  light: {
    name: 'light',
    background: '#f8fafc',
    card: '#ffffff',
    text: '#0f172a',
    muted: '#64748b',
    border: '#e2e8f0',
    primary: '#2563eb',
  },
  dark: {
    name: 'dark',
    background: '#0b1220',
    card: '#0f172a',
    text: '#e2e8f0',
    muted: '#94a3b8',
    border: '#1e293b',
    primary: '#60a5fa',
  },
  sand: {
    name: 'sand',
    background: '#f7f2e8',
    card: '#fffaf1',
    text: '#3a2f20',
    muted: '#7c6a50',
    border: '#e9dcc5',
    primary: '#b7791f',
  },
};

type UiSettingsContextValue = {
  settings: UiSettings;
  theme: ThemePalette;
  ready: boolean;
  setTheme: (theme: ThemeName) => Promise<void>;
  setFontScale: (fontScale: number) => Promise<void>;
  toggleDarkLight: () => Promise<void>;
};

const UiSettingsContext = createContext<UiSettingsContextValue | null>(null);

export function UiSettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<UiSettings>({ theme: 'light', fontScale: 1 });
  const [ready, setReady] = useState(false);

  useEffect(() => {
    getUiSettings()
      .then((saved) => setSettings(saved))
      .finally(() => setReady(true));
  }, []);

  const value = useMemo<UiSettingsContextValue>(() => ({
    settings,
    theme: THEMES[settings.theme],
    ready,
    async setTheme(theme) {
      const next = await saveUiSettings({ theme });
      setSettings(next);
    },
    async setFontScale(fontScale) {
      const clamped = Math.max(0.9, Math.min(1.4, Number(fontScale.toFixed(2))));
      const next = await saveUiSettings({ fontScale: clamped });
      setSettings(next);
    },
    async toggleDarkLight() {
      const theme: ThemeName = settings.theme === 'dark' ? 'light' : 'dark';
      const next = await saveUiSettings({ theme });
      setSettings(next);
    },
  }), [ready, settings]);

  return <UiSettingsContext.Provider value={value}>{children}</UiSettingsContext.Provider>;
}

export function useUiSettings() {
  const ctx = useContext(UiSettingsContext);
  if (!ctx) throw new Error('useUiSettings must be used within UiSettingsProvider');
  return ctx;
}

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { getDatabase } from '../db';

type DbContextValue = {
  ready: boolean;
};

const DbContext = createContext<DbContextValue>({ ready: false });

export function DbProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    getDatabase()
      .then(() => setReady(true))
      .catch(() => setReady(false));
  }, []);

  if (!ready) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color="#2563eb" />
      </View>
    );
  }

  return <DbContext.Provider value={{ ready }}>{children}</DbContext.Provider>;
}

export function useDbReady() {
  return useContext(DbContext).ready;
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f8fafc',
  },
});

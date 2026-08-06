import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ApiError } from '../api/client';
import * as authApi from '../api/auth';
import type { User } from '../api/auth';
import { demoUser, isDemoMode } from '../api/demo';

type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  signIn: (username: string, password: string, rememberMe?: boolean) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (isDemoMode) {
      setIsLoading(false);
      return;
    }
    authApi.getCurrentUser()
      .then(setUser)
      .catch((error: unknown) => {
        if (!(error instanceof ApiError) || error.status !== 401) console.info('No active StreamCell session.');
      })
      .finally(() => setIsLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isLoading,
    signIn: async (username, password, rememberMe) => {
      if (isDemoMode) {
        if (!username.trim() || !password) throw new Error('Demo credentials are required.');
        setUser(demoUser);
        return;
      }
      setUser(await authApi.login(username, password, rememberMe));
    },
    signOut: async () => {
      if (isDemoMode) {
        setUser(null);
        return;
      }
      await authApi.logout();
      setUser(null);
    },
  }), [user, isLoading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

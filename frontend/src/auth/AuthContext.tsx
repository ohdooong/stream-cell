import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ApiError } from '../api/client';
import * as authApi from '../api/auth';
import type { User } from '../api/auth';

type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  signIn: (username: string, password: string, rememberMe?: boolean) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const authEnabled = import.meta.env.VITE_AUTH_ENABLED === 'true';
const developmentUser: User = {
  userId: Number(import.meta.env.VITE_DEFAULT_USER_ID ?? 1),
  username: 'development-user',
  displayName: 'Development User',
  roles: ['ROLE_ADMIN'],
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(authEnabled ? null : developmentUser);
  const [isLoading, setIsLoading] = useState(authEnabled);

  useEffect(() => {
    if (!authEnabled) return;
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
    signIn: async (username, password, rememberMe) => setUser(await authApi.login(username, password, rememberMe)),
    signOut: async () => {
      if (!authEnabled) return;
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

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setUnauthorizedHandler } from '../api/client';
import * as authApi from '../api/auth';
import type { User } from '../api/auth';

type AuthContextValue = {
  user: User | null;
  authEnabled: boolean;
  signIn: (loginId: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const authEnabled = import.meta.env.VITE_AUTH_ENABLED !== 'false';
const developmentUser: User = {
  userId: Number(import.meta.env.VITE_DEFAULT_USER_ID ?? 1),
  username: 'development-user',
  displayName: 'Development User',
  roles: ['ROLE_ADMIN'],
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(authEnabled ? null : developmentUser);

  useEffect(() => {
    if (!authEnabled) return;
    setUnauthorizedHandler(() => setUser(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    authEnabled,
    signIn: async (loginId, password) => setUser(await authApi.login(loginId, password)),
    signOut: async () => {
      if (!authEnabled) return;
      authApi.logout();
      setUser(null);
    },
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

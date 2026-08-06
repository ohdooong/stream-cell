import { api, setAccessToken, unwrap } from './client';

const loginPath = import.meta.env.VITE_AUTH_LOGIN_PATH ?? '/api/v1/auth/login';
const mePath = import.meta.env.VITE_AUTH_ME_PATH ?? '/api/v1/auth/me';
const logoutPath = import.meta.env.VITE_AUTH_LOGOUT_PATH ?? '/api/v1/auth/logout';

export type User = {
  userId: number;
  username: string;
  displayName?: string;
  roles: string[];
};

type LoginResult = Partial<User> & {
  accessToken?: string;
  access_token?: string;
  token?: string;
  user?: User;
};

export async function login(username: string, password: string, rememberMe = false): Promise<User> {
  const raw = unwrap(await api<LoginResult | { data: LoginResult }>(loginPath, {
    method: 'POST',
    body: JSON.stringify({ username, password, rememberMe }),
  }));

  setAccessToken(raw.accessToken ?? raw.access_token ?? raw.token ?? null);
  if (raw.user) return raw.user;
  return {
    userId: raw.userId ?? 0,
    username: raw.username ?? username,
    displayName: raw.displayName,
    roles: raw.roles ?? [],
  };
}

export async function getCurrentUser(): Promise<User> {
  return unwrap(await api<User | { data: User }>(mePath));
}

export async function logout() {
  try {
    await api(logoutPath, { method: 'POST' });
  } finally {
    setAccessToken(null);
  }
}

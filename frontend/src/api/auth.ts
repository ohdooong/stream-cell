import { api, setAccessToken, unwrap } from './client';

const LOGIN_PATH = '/api/v1/web/auth/login';
const USERS_PATH = '/api/v1/web/user/items';

export type User = {
  userId: number;
  username: string;
  displayName: string;
  roles: string[];
};

type LoginToken = { accessToken: string; tokenType: string; expiration: number };
type UserRecord = { userId: number; loginId: string; name: string; role?: string; status?: string };

export async function login(loginId: string, password: string): Promise<User> {
  const credentials = { loginId: loginId.trim(), password };
  const token = unwrap(await api<{ body: LoginToken }>(LOGIN_PATH, {
    method: 'POST',
    body: JSON.stringify(credentials),
  }));

  if (!token || typeof token.accessToken !== 'string' || !token.accessToken.trim() || token.tokenType !== 'Bearer') {
    throw new Error('로그인 응답의 JWT를 확인할 수 없습니다.');
  }

  setAccessToken(token.accessToken);
  try {
    const users = await api<UserRecord[]>(USERS_PATH);
    const current = users.find((item) => item.loginId === credentials.loginId);
    if (!current || !Number.isSafeInteger(current.userId) || current.userId < 1) {
      throw new Error('로그인한 사용자 정보를 찾을 수 없습니다.');
    }
    return {
      userId: current.userId,
      username: current.loginId,
      displayName: current.name || current.loginId,
      roles: current.role ? [`ROLE_${current.role}`] : [],
    };
  } catch (error) {
    setAccessToken(null);
    throw error;
  }
}

export function logout() {
  setAccessToken(null);
}

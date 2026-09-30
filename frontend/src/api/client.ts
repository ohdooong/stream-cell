const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

let accessToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const requestToken = accessToken;
  headers.set('Accept', 'application/json');

  if (requestToken) headers.set('Authorization', `Bearer ${requestToken}`);
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  });

  if (!response.ok) {
    if (response.status === 401 && requestToken && requestToken === accessToken) {
      setAccessToken(null);
      onUnauthorized?.();
    }
    const body = await response.json().catch(() => null) as { message?: string; error?: string } | null;
    throw new ApiError(response.status, body?.message ?? body?.error ?? `요청을 처리하지 못했습니다. (${response.status})`);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function unwrap<T>(payload: T | { body?: T; data?: T }): T {
  if (payload && typeof payload === 'object' && 'body' in payload) {
    return (payload as { body?: T }).body as T;
  }
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as { data?: T }).data as T;
  }
  return payload as T;
}

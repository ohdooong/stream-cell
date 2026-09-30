import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({
  stdin: {
    contents: "export { login, logout } from './src/api/auth'; export { api, setUnauthorizedHandler } from './src/api/client';",
    resolveDir: process.cwd(),
    sourcefile: 'authHarness.ts',
    loader: 'ts',
  },
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false,
  define: { 'import.meta.env.VITE_API_BASE_URL': '""' },
});
const { login, logout, api, setUnauthorizedHandler } = await import(
  `data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`
);

const originalFetch = globalThis.fetch;

test.after(() => {
  globalThis.fetch = originalFetch;
  logout();
  setUnauthorizedHandler(null);
});

test('JWT login sends the backend DTO and authorizes later requests', async () => {
  const requests = [];
  globalThis.fetch = async (url, init) => {
    requests.push({ url, init });
    if (url.endsWith('/auth/login')) return Response.json({ body: { accessToken: 'jwt-value', tokenType: 'Bearer', expiration: 3600000 } });
    if (url.endsWith('/user/items')) return Response.json([{ userId: 7, loginId: 'alice', name: 'Alice', role: 'USER' }]);
    return Response.json({ ok: true });
  };

  const user = await login(' alice ', 'secret');
  assert.deepEqual(JSON.parse(requests[0].init.body), { loginId: 'alice', password: 'secret' });
  assert.equal(requests[0].url, '/api/v1/web/auth/login');
  assert.equal(requests[0].init.headers.get('Authorization'), null);
  assert.equal(requests[1].init.headers.get('Authorization'), 'Bearer jwt-value');
  assert.deepEqual(user, { userId: 7, username: 'alice', displayName: 'Alice', roles: ['ROLE_USER'] });

  await api('/api/v1/platform/topic/topics');
  assert.equal(requests[2].init.headers.get('Authorization'), 'Bearer jwt-value');
  logout();
  await api('/api/v1/platform/topic/topics');
  assert.equal(requests[3].init.headers.get('Authorization'), null);
});

test('missing account details do not leave a JWT active', async () => {
  const requests = [];
  globalThis.fetch = async (url, init) => {
    requests.push({ url, init });
    if (url.endsWith('/auth/login')) return Response.json({ body: { accessToken: 'jwt-value', tokenType: 'Bearer', expiration: 3600000 } });
    if (url.endsWith('/user/items')) return Response.json([]);
    return Response.json({ ok: true });
  };

  await assert.rejects(login('alice', 'secret'), /사용자 정보를 찾을 수 없습니다/);
  await api('/api/v1/platform/topic/topics');
  assert.equal(requests[2].init.headers.get('Authorization'), null);
});

test('a protected API 401 clears the JWT and reports expiry', async () => {
  let expired = false;
  setUnauthorizedHandler(() => { expired = true; });
  globalThis.fetch = async (url) => {
    if (url.endsWith('/auth/login')) return Response.json({ body: { accessToken: 'jwt-value', tokenType: 'Bearer', expiration: 3600000 } });
    if (url.endsWith('/user/items')) return Response.json([{ userId: 7, loginId: 'alice', name: 'Alice' }]);
    return Response.json({ message: 'Unauthorized' }, { status: 401 });
  };

  await login('alice', 'secret');
  await assert.rejects(api('/api/v1/platform/topic/topics'), /Unauthorized/);
  assert.equal(expired, true);
  setUnauthorizedHandler(null);
});

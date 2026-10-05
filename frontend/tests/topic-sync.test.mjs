import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({
  stdin: { contents: "export { platformApi } from './src/api/platform';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false,
  define: { 'import.meta.env.VITE_API_BASE_URL': '""' },
});
const { platformApi } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
const originalFetch = globalThis.fetch;
test.afterEach(() => { globalThis.fetch = originalFetch; });

test('topic sync POST returns the backend topic list rather than a notification string', async () => {
  const topics = [{ topicId: 1, topicName: 'orders', displayName: '주문', messageFormat: 'JSON' }];
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/platform/topic/sync');
    assert.equal(init.method, 'POST');
    assert.equal(init.body, undefined);
    return Response.json({ status: 200, message: 'Success', body: topics });
  };
  assert.deepEqual(await platformApi.syncTopics(), topics);
});

test('an empty synchronized topic list is a successful response', async () => {
  globalThis.fetch = async () => Response.json({ status: 200, body: [] });
  assert.deepEqual(await platformApi.syncTopics(), []);
});

test('sync failure exposes the server error for the UI to display', async () => {
  globalThis.fetch = async () => Response.json({ message: 'Kafka 동기화 실패' }, { status: 500 });
  await assert.rejects(platformApi.syncTopics(), (error) => error.status === 500 && error.message === 'Kafka 동기화 실패');
});

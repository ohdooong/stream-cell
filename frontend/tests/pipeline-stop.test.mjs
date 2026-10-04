import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({
  stdin: { contents: "export { platformApi } from './src/api/platform'; export { setAccessToken } from './src/api/client'; export { canStopPipeline, isStopSettled } from './src/pipelines/usePipelineStop';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false,
  define: { 'import.meta.env.VITE_API_BASE_URL': '""', 'process.env.NODE_ENV': '"production"' },
});
const { platformApi, setAccessToken, canStopPipeline, isStopSettled } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
const originalFetch = globalThis.fetch;
test.afterEach(() => { globalThis.fetch = originalFetch; setAccessToken(null); });

test('stop uses the Swagger POST path, JWT, and no request body; accepts 202 with null body', async () => {
  setAccessToken('fixture-jwt');
  const requests = [];
  globalThis.fetch = async (url, init) => {
    requests.push({ url, init });
    return Response.json({ status: 202, message: 'Job Cancel 성공', body: null }, { status: 202 });
  };
  assert.equal(await platformApi.stopPipeline(42), undefined);
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/api/v1/platform/pipeline/pipelines/deployment/42/stop');
  assert.equal(requests[0].init.method, 'POST');
  assert.equal(requests[0].init.body, undefined);
  assert.equal(requests[0].init.headers.get('Authorization'), 'Bearer fixture-jwt');
});

test('stop surfaces backend errors instead of reporting success', async () => {
  for (const status of [400, 404, 500]) {
    globalThis.fetch = async () => Response.json({ message: '중지 요청 거절' }, { status });
    await assert.rejects(platformApi.stopPipeline(42), (error) => error.status === status && error.message === '중지 요청 거절');
  }
});

test('only RUNNING can be stopped and transitional states do not mean completion', () => {
  const statuses = ['DRAFT', 'CREATED', 'ARTIFACT_UPLOADED', 'DEPLOYING', 'RUNNING', 'FAILED', 'STOPPING', 'STOPPED', 'FINISHED', 'SUSPENDED'];
  for (const status of statuses) assert.equal(canStopPipeline(status), status === 'RUNNING');
  for (const status of ['RUNNING', 'DEPLOYING', 'STOPPING']) assert.equal(isStopSettled(status), false);
  for (const status of ['STOPPED', 'FINISHED', 'FAILED', 'SUSPENDED']) assert.equal(isStopSettled(status), true);
});

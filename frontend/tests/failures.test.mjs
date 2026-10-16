import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const bundle = await build({
  stdin: { contents: "export * from './src/api/failures'; export { failureTime } from './src/pipelines/PipelineFailures'; export { setAccessToken } from './src/api/client';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false, loader: { '.css': 'empty' },
  define: { 'import.meta.env': '{}', 'process.env.NODE_ENV': '"production"' },
});
const { getPipelineFailures, parseFailures, failureTime, setAccessToken } = await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`);
const originalFetch = globalThis.fetch;
const failure = {
  errorExceptionName: 'org.apache.flink.runtime.JobException',
  errorMessage: 'org.apache.flink.runtime.JobException: Recovery is suppressed by NoRestartBackoffTimeStrategy\n\tat ExecutionFailureHandler.handleFailure(ExecutionFailureHandler.java:180)\nCaused by: java.io.IOException: Failed to deserialize consumer record\nCaused by: org.apache.flink.formats.json.JsonParseException: Fail to deserialize at field: event_time.',
  errorTimestamp: '2026-10-05 13:49:59.480000',
};
test.afterEach(() => { globalThis.fetch = originalFetch; setAccessToken(null); });

test('GET failures uses the supplied path, JWT and abort signal, preserving the complete exception', async () => {
  setAccessToken('fixture-jwt');
  const controller = new AbortController();
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/platform/pipeline/pipelines/42/failures');
    assert.ok(!init.method || init.method === 'GET');
    assert.equal(init.body, undefined);
    assert.equal(init.headers.get('Authorization'), 'Bearer fixture-jwt');
    assert.equal(init.signal, controller.signal);
    return Response.json({ status: 200, body: failure });
  };
  assert.deepEqual(await getPipelineFailures(42, controller.signal), [failure]);
});

test('direct, data and body envelopes work without fabricating AI analysis', async () => {
  for (const payload of [failure, { data: failure }, { body: failure }]) {
    globalThis.fetch = async () => Response.json(payload);
    assert.deepEqual(await getPipelineFailures(42), [failure]);
  }
});

test('null, empty lists and nullable failure fields are empty results', () => {
  for (const value of [null, [], { failure: null }, { errorExceptionName: null, errorMessage: null, errorTimestamp: null }]) assert.deepEqual(parseFailures(value, 42), []);
  assert.deepEqual(parseFailures([failure], 42), [failure]);
  assert.deepEqual(parseFailures({ pipelineId: 42, failure }, 42), [failure]);
});

test('malformed data and mismatched Pipeline IDs are errors, not empty success', () => {
  for (const value of [{}, undefined, 'failure', { errorMessage: {} }, { errorTimestamp: false }, { errorTimestamp: NaN }]) assert.throws(() => parseFailures(value, 42), /실패 응답/);
  assert.throws(() => parseFailures({ pipelineId: 43, failure }, 42), /ID가 다릅니다/);
});

test('timestamps preserve the backend local datetime precision and support epoch milliseconds', () => {
  assert.equal(failureTime(failure.errorTimestamp), '2026-10-05 13:49:59.480000');
  assert.match(failureTime(1791175799480), /2026.*Asia\/Seoul/);
  assert.equal(failureTime(null), '발생 시각 정보 없음');
  assert.equal(failureTime(''), '발생 시각 정보 없음');
});

test('backend failures and cancellations propagate for the UI to show or ignore appropriately', async () => {
  globalThis.fetch = async () => Response.json({ message: '실패 정보 조회 오류' }, { status: 500 });
  await assert.rejects(getPipelineFailures(42), (error) => error.status === 500 && error.message === '실패 정보 조회 오류');
  globalThis.fetch = async () => { throw new DOMException('Aborted', 'AbortError'); };
  await assert.rejects(getPipelineFailures(42), (error) => error.name === 'AbortError');
});

test('invalid Pipeline IDs do not make requests', async () => {
  globalThis.fetch = async () => assert.fail('must not fetch');
  for (const id of [0, -1, NaN, 1.5]) await assert.rejects(getPipelineFailures(id), /Pipeline ID/);
});

import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({
  stdin: { contents: "export { platformApi } from './src/api/platform'; export { setAccessToken } from './src/api/client'; export { parseInputTopics, assertInputTopic } from './src/api/inputTopics'; export { CustomJarFields } from './src/pipelines/CustomJarFields'; export { InputTopicsGate } from './src/pipelines/InputTopicsGate'; export { createElement } from 'react'; export { renderToStaticMarkup } from 'react-dom/server';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false, external: ['react', 'react-dom/server'],
  define: { 'import.meta.env': '{}' },
});
// React externals in a data URL need absolute URLs, rather than relative resolution.
const code = result.outputFiles[0].text.replaceAll('"react"', JSON.stringify(import.meta.resolve('react'))).replaceAll('"react/jsx-runtime"', JSON.stringify(import.meta.resolve('react/jsx-runtime'))).replaceAll('"react-dom/server"', JSON.stringify(import.meta.resolve('react-dom/server')));
const { platformApi, setAccessToken, parseInputTopics, assertInputTopic, CustomJarFields, InputTopicsGate, createElement, renderToStaticMarkup } = await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`);
const originalFetch = globalThis.fetch;
const topics = [{ topicId: 10, topicName: 'deploy-orders', displayName: '주문', messageFormat: 'JSON' }];
test.afterEach(() => { globalThis.fetch = originalFetch; setAccessToken(null); });

test('registration input list calls the new GET endpoint with JWT and cancellation, without a userId parameter', async () => {
  setAccessToken('fixture-jwt');
  const controller = new AbortController();
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/platform/topic/input-topics');
    assert.ok(!init.method || init.method === 'GET');
    assert.equal(init.headers.get('Authorization'), 'Bearer fixture-jwt');
    assert.equal(init.signal, controller.signal);
    assert.equal(init.body, undefined);
    return Response.json({ body: topics });
  };
  assert.deepEqual(await platformApi.getInputTopics(controller.signal), topics);
});

test('direct and wrapped lists, including no DEPLOY grants, are supported', async () => {
  for (const payload of [topics, { data: topics }, { body: topics }]) {
    globalThis.fetch = async () => Response.json(payload);
    assert.deepEqual(await platformApi.getInputTopics(), topics);
  }
  assert.deepEqual(parseInputTopics([]), []);
});

test('invalid permission list responses are errors, not a global Topic fallback', () => {
  for (const value of [null, {}, [{ topicId: 0, topicName: 'invalid' }], [{ topicId: 10 }], [{ topicId: '10', topicName: 'invalid' }]]) assert.throws(() => parseInputTopics(value), /목록 응답/);
});

test('a forbidden input-list request cannot fall back to the all-Topic endpoint', async () => {
  const calls = [];
  globalThis.fetch = async (url) => { calls.push(url); return Response.json({ message: '권한 조회 실패' }, { status: 403 }); };
  await assert.rejects(platformApi.getInputTopics(), (error) => error.status === 403);
  assert.deepEqual(calls, ['/api/v1/platform/topic/input-topics']);
});

test('selection guards reject unlisted IDs, including previously saved but no longer deployable topics', () => {
  assert.doesNotThrow(() => assertInputTopic(topics, 10));
  for (const id of [null, 0, 11, NaN, 1.5]) assert.throws(() => assertInputTopic(topics, id), /DEPLOY/);
  assert.throws(() => assertInputTopic([], 10), /DEPLOY/);
});

test('Custom JAR never manufactures a selectable option for a saved Topic outside the permitted list', () => {
  const markup = renderToStaticMarkup(createElement(CustomJarFields, { topics, disabled: false, onChange() {}, draft: { inputId: 11, entryClass: '', parallelism: 1, file: null, args: '' } }));
  assert.match(markup, /deploy-orders/);
  assert.doesNotMatch(markup, /value="11"|Topic #11/);
  assert.match(markup, /value="" disabled="" selected=""/);
});

test('registration forms do not mount while the permission list is loading', () => {
  const markup = renderToStaticMarkup(createElement(InputTopicsGate, { children: () => assert.fail('form must not render while loading') }));
  assert.match(markup, /입력 Topic 조회 중/);
  assert.doesNotMatch(markup, /<form/);
});

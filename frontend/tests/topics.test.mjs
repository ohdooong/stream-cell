import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { build } from 'esbuild';

const result = await build({
  stdin: { contents: "export { platformApi } from './src/api/platform'; export { setAccessToken } from './src/api/client'; export { isAdminOnlyView } from './src/topics/access'; export { TopicMetadata, TopicBrowser } from './src/topics/TopicBrowser'; export { createElement } from 'react'; export { renderToStaticMarkup } from 'react-dom/server';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false, external: ['react', 'react-dom/server'],
  loader: { '.css': 'empty' }, define: { 'import.meta.env': '{}' },
});
const code = result.outputFiles[0].text.replaceAll('"react"', JSON.stringify(import.meta.resolve('react'))).replaceAll('"react/jsx-runtime"', JSON.stringify(import.meta.resolve('react/jsx-runtime'))).replaceAll('"react-dom/server"', JSON.stringify(import.meta.resolve('react-dom/server')));
const { platformApi, setAccessToken, isAdminOnlyView, TopicMetadata, TopicBrowser, createElement, renderToStaticMarkup } = await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`);
const originalFetch = globalThis.fetch;
const topic = { topicId: 10, topicName: 'orders', displayName: '주문', description: '주문 이벤트', messageFormat: 'JSON', timeField: 'event_time', schemaJson: '{"type":"object","properties":{"event_time":{"type":"string"}}}' };
test.afterEach(() => { globalThis.fetch = originalFetch; setAccessToken(null); });

test('Topic 조회 is public within the console; management and permission views are ADMIN-only', () => {
  assert.equal(isAdminOnlyView('topics'), false);
  assert.equal(isAdminOnlyView('topic-admin'), true);
  assert.equal(isAdminOnlyView('permissions'), true);
  assert.equal(isAdminOnlyView('create'), false);
});

test('console guards both the ADMIN navigation and the editable Topic component', async () => {
  const source = await readFile(new URL('../src/ConnectedApp.tsx', import.meta.url), 'utf8');
  assert.match(source, /nav\.filter\(\(\[id\]\) => !isAdminOnlyView\(id\) \|\| isAdmin\)/);
  assert.match(source, /view === 'topic-admin' && isAdmin && <Topics/);
  assert.match(source, /view === 'topics' && <TopicBrowser/);
  assert.match(source, /if \(isAdminOnlyView\(view\) && !isAdmin\) setView\('topics'\)/);
});

test('read-only list uses the my-Topic GET API and detail keeps the existing API, with JWT and abort signals', async () => {
  setAccessToken('topic-fixture-jwt');
  const controller = new AbortController();
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push(url);
    assert.ok(!init.method || init.method === 'GET');
    assert.equal(init.body, undefined);
    assert.equal(init.headers.get('Authorization'), 'Bearer topic-fixture-jwt');
    assert.equal(init.signal, controller.signal);
    return Response.json({ body: url.endsWith('/10') ? topic : [topic] });
  };
  assert.deepEqual(await platformApi.getMyTopics(controller.signal), [topic]);
  assert.deepEqual(await platformApi.getTopic(10, controller.signal), topic);
  assert.deepEqual(calls, ['/api/v1/web/my/topic/topics', '/api/v1/platform/topic/topics/10']);
});

test('console initial loading and refresh choose my Topics for users and the full list for ADMIN', async () => {
  const calls = [];
  globalThis.fetch = async (url) => { calls.push(url); return Response.json({ body: [topic] }); };
  assert.deepEqual(await platformApi.getTopicsForRole(false), [topic]);
  assert.deepEqual(await platformApi.getTopicsForRole(true), [topic]);
  assert.deepEqual(await platformApi.getTopics(), [topic]);
  assert.deepEqual(calls, ['/api/v1/web/my/topic/topics', '/api/v1/platform/topic/topics', '/api/v1/platform/topic/topics']);
  const source = await readFile(new URL('../src/ConnectedApp.tsx', import.meta.url), 'utf8');
  const consoleSource = source.slice(source.indexOf('function Console('), source.indexOf('function Overview('));
  assert.match(consoleSource, /refreshTopics = async.*getTopicsForRole\(isAdmin\)/);
  assert.match(consoleSource, /Promise\.allSettled\(.*getTopicsForRole\(isAdmin, controller\.signal\)/);
  assert.doesNotMatch(consoleSource, /platformApi\.getTopics\(/);
  const browserSource = await readFile(new URL('../src/topics/TopicBrowser.tsx', import.meta.url), 'utf8');
  assert.match(browserSource, /platformApi\.getMyTopics\(controller\.signal\)/);
  assert.doesNotMatch(browserSource, /platformApi\.getTopics\(/);
});

test('my-Topic list supports body/data/direct responses and an empty permission list', async () => {
  for (const payload of [{ body: [topic] }, { data: [topic] }, [topic], { body: [] }]) {
    globalThis.fetch = async () => Response.json(payload);
    assert.deepEqual(await platformApi.getMyTopics(), Array.isArray(payload) ? payload : payload.body ?? payload.data);
  }
});

test('my-Topic list errors are exposed without falling back to the ADMIN or DEPLOY endpoints', async () => {
  for (const status of [403, 500]) {
    const calls = [];
    globalThis.fetch = async (url) => { calls.push(url); return Response.json({ message: '내 Topic 조회 실패' }, { status }); };
    await assert.rejects(platformApi.getMyTopics(), (error) => error.status === status && error.message === '내 Topic 조회 실패');
    assert.deepEqual(calls, ['/api/v1/web/my/topic/topics']);
  }
});

test('permission denial is surfaced; it does not trigger sync or a DEPLOY-list fallback', async () => {
  const calls = [];
  globalThis.fetch = async (url) => { calls.push(url); return Response.json({ message: '조회 권한 없음' }, { status: 403 }); };
  await assert.rejects(platformApi.getTopic(10), (error) => error.status === 403 && error.message === '조회 권한 없음');
  assert.deepEqual(calls, ['/api/v1/platform/topic/topics/10']);
});

test('saved metadata is visible without form, edit, save or sync controls', () => {
  const markup = renderToStaticMarkup(createElement(TopicMetadata, { topic }));
  for (const value of ['orders', '주문', '주문 이벤트', 'JSON', 'event_time', 'properties', '읽기 전용']) assert.ok(markup.includes(value));
  assert.doesNotMatch(markup, /<(form|input|textarea|select|button)\b/);
});

test('missing metadata is not fabricated and raw invalid schema text is safely escaped', () => {
  const empty = renderToStaticMarkup(createElement(TopicMetadata, { topic: { topicId: 1, topicName: 'empty' } }));
  assert.match(empty, /미등록/);
  assert.match(empty, /등록된 Schema가 없습니다/);
  const markup = renderToStaticMarkup(createElement(TopicMetadata, { topic: { ...topic, schemaJson: '<script>alert(1)</script>' } }));
  assert.ok(markup.includes('&lt;script&gt;'));
  assert.ok(!markup.includes('<script>'));
});

test('browser initially displays loading, not an empty list or any mutation controls', () => {
  const markup = renderToStaticMarkup(createElement(TopicBrowser));
  assert.match(markup, /Topic 목록 조회 중/);
  assert.doesNotMatch(markup, /Topic 동기화|<form|저장<|조회 가능한 Topic이 없습니다/);
});

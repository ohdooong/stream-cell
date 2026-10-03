import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

async function compile(entry) {
  const result = await build({ entryPoints: [entry], bundle: true, platform: 'node', format: 'esm', write: false, define: { 'import.meta.env': '{}' } });
  return import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
}
const { parseResults, resultsPath, getPipelineResults } = await compile('src/api/results.ts');
const { buildChartData, groupId } = await compile('src/results/chartData.ts');

const series = { key: 'orderCount', label: '주문 건수', groupKeys: ['productId', 'region'] };
const columns = [{ key: 'windowStart', label: '윈도우', type: 'TIMESTAMP' }, { key: 'orderCount', label: '주문 건수', type: 'NUMBER' }];

test('multi-field grouping separates the same product in different regions and sorts time ascending', () => {
  const model = buildChartData([
    { windowStart: '2026-10-03T10:01:00+09:00', productId: 'A', region: '서울', orderCount: 120 },
    { windowStart: '2026-10-03T10:00:00+09:00', productId: 'A', region: '서울', orderCount: 100 },
    { windowStart: '2026-10-03T10:00:00+09:00', productId: 'A', region: '부산', orderCount: 70 },
  ], 'windowStart', series);
  assert.equal(model.groups.length, 2);
  assert.equal(model.xs[0].value, '2026-10-03T10:00:00+09:00');
  assert.equal(model.groups[0].points.get(model.xs[0].id), 100);
  assert.equal(model.groups[1].points.get(model.xs[1].id), undefined);
});

test('composite identities preserve null and do not collide with separator-containing values', () => {
  assert.notEqual(groupId({ a: 'A / B', b: 'C' }, ['a', 'b']), groupId({ a: 'A', b: 'B / C' }, ['a', 'b']));
  assert.notEqual(groupId({ a: null }, ['a']), groupId({ a: 'null' }, ['a']));
  assert.notEqual(groupId({ a: 1 }, ['a']), groupId({ a: '1' }, ['a']));
});

test('ungrouped aggregates and numeric strings work while null metrics remain gaps', () => {
  const model = buildChartData([{ window: 2, count: null }, { window: 1, count: '15' }], 'window', { key: 'count', label: '건수', groupKeys: [] });
  assert.equal(model.groups.length, 1);
  assert.equal(model.groups[0].label, '전체 집계');
  assert.deepEqual(model.xs.map((item) => item.value), [1, 2]);
  assert.equal(model.groups[0].points.get(model.xs[0].id), 15);
  assert.equal(model.groups[0].points.get(model.xs[1].id), null);
});

test('table-only and empty results are supported without inventing chart metadata', () => {
  assert.equal(parseResults({ columns, rows: [] }).chart, null);
  assert.deepEqual(parseResults({ columns, rows: [], chart: { xKey: 'windowStart', series: [series] } }).rows, []);
  assert.throws(() => parseResults({ rows: [] }), /columns/);
  assert.throws(() => parseResults({ columns, rows: [], chart: { xKey: 'windowStart', series: [{ ...series, groupKeys: 'region' }] } }), /groupKeys/);
});

test('results request uses the supplied endpoint, query parameters, envelope and abort signal', async () => {
  const originalFetch = globalThis.fetch;
  const signal = new AbortController().signal;
  let requested;
  globalThis.fetch = async (path, init) => {
    requested = { path, init };
    return new Response(JSON.stringify({ body: { pipelineId: 12, columns, rows: [], chart: { xKey: 'windowStart', series: [series] } } }), { status: 200 });
  };
  try {
    const result = await getPipelineResults(12, 100, 20, signal);
    assert.equal(requested.path, '/api/v1/platform/pipeline/pipelines/12/results?limit=100&rangeMinutes=20');
    assert.equal(requested.init.signal, signal);
    assert.deepEqual(result.chart.series[0].groupKeys, ['productId', 'region']);
    assert.throws(() => resultsPath(0, 100, 20));
    await assert.rejects(getPipelineResults(24), /ID가 다릅니다/);
  } finally { globalThis.fetch = originalFetch; }
});

import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({ entryPoints: ['src/pipelines/aiSqlForm.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const {
  initialAiSqlDraft, toAiSqlConfigInput, toAiSqlInput, toAiSqlPreviewRequest,
  validateAiSqlInput, isAiSqlPreview,
} = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
const planResult = await build({ entryPoints: ['src/api/pipelinePlan.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const { isPersistedPipelinePlan } = await import(`data:text/javascript;base64,${Buffer.from(planResult.outputFiles[0].text).toString('base64')}`);
const detailResult = await build({ entryPoints: ['src/api/pipelineDetail.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const { pipelineDetailPath } = await import(`data:text/javascript;base64,${Buffer.from(detailResult.outputFiles[0].text).toString('base64')}`);
const topic = { topicId: 10, topicName: 'orders', schemaJson: JSON.stringify({ properties: { eventTime: { type: 'string' }, amount: { type: 'number' } } }) };
const draft = { ...initialAiSqlDraft, name: '  주문 집계  ', description: '  상품별 집계  ', topicId: 10, request: '  5분마다 합계  ' };

test('request keeps the current owner and exactly one selected Topic', () => {
  const input = toAiSqlInput(draft, 7);
  assert.equal(input.ownerUserId, 7);
  assert.equal(input.pipelineName, '주문 집계');
  assert.equal(input.description, '상품별 집계');
  assert.equal(input.naturalLanguageRequest, '5분마다 합계');
  assert.equal(input.inputTopicId, 10);
  assert.deepEqual(Object.keys(input).sort(), [
    'description', 'inputTopicId', 'naturalLanguageRequest', 'ownerUserId',
    'pipelineName', 'pipelineType',
  ]);
  assert.deepEqual(validateAiSqlInput(input, { 10: topic }), []);
});

test('preview and AI config requests match the backend DTOs', () => {
  const input = toAiSqlInput(draft, 7);
  assert.deepEqual(toAiSqlPreviewRequest(input), {
    inputTopicId: 10,
    userId: 7,
    naturalLanguageRequest: '5분마다 합계',
  });
  const pipelinePlan = {
    sourceTopicId: 10,
    window: { type: 'TUMBLE', size: 5, unit: 'MINUTE' },
    groupBy: ['product_id'],
    aggregations: [{ function: 'COUNT', field: '*', alias: 'order_count' }],
    filters: [],
  };
  assert.deepEqual(toAiSqlConfigInput(input, pipelinePlan), {
    inputTopicId: 10,
    userId: 7,
    naturalLanguageRequest: '5분마다 합계',
    pipelinePlan,
  });
});

test('generation requires the selected Topic detail and Schema', () => {
  const input = toAiSqlInput(draft, 1);
  assert.ok(validateAiSqlInput(input, {}).some((error) => error.includes('#10')));
  assert.ok(validateAiSqlInput(input, { 10: { ...topic, schemaJson: '' } }).some((error) => error.includes('Schema')));
});

test('the API contract rejects an invalid Topic value even when bypassing the form', () => {
  const input = toAiSqlInput(draft, 1);
  assert.ok(validateAiSqlInput({ ...input, inputTopicId: [10, 11] }, { 10: topic }).some((error) => error.includes('하나')));
});

test('missing basic inputs prevent creation', () => {
  const errors = validateAiSqlInput(toAiSqlInput(initialAiSqlDraft, 0), {});
  assert.equal(errors.length, 4);
});

test('field length limits are enforced', () => {
  const input = toAiSqlInput({ ...draft, description: 'a'.repeat(1001), request: 'b'.repeat(4001) }, 1);
  const errors = validateAiSqlInput(input, { 10: topic });
  assert.ok(errors.some((error) => error.includes('1,000')));
  assert.ok(errors.some((error) => error.includes('4,000')));
});

test('malformed preview responses cannot be treated as reviewed SQL', () => {
  const preview = {
    pipelinePlan: {
      sourceTopicId: 10,
      window: { type: 'TUMBLE', size: 5, unit: 'MINUTE' },
      groupBy: ['product_id'],
      aggregations: [{ function: 'COUNT', field: '*', alias: 'order_count' }],
      filters: [{ field: 'amount', operator: 'GTE', value: 10000 }],
    },
    generatedFlinkSql: 'SELECT 1',
  };
  assert.equal(isAiSqlPreview(preview), true);
  assert.equal(isAiSqlPreview(null), false);
  assert.equal(isAiSqlPreview({ ...preview, generatedFlinkSql: '' }), false);
  assert.equal(isAiSqlPreview({ ...preview, pipelinePlan: { ...preview.pipelinePlan, sourceTopicId: 0 } }), false);
  assert.equal(isAiSqlPreview({ ...preview, pipelinePlan: { ...preview.pipelinePlan, groupBy: null } }), false);
  assert.equal(isAiSqlPreview({ ...preview, pipelinePlan: { ...preview.pipelinePlan, filters: [{ field: 'amount', operator: 'GTE' }] } }), false);
});

test('AI SQL registration only succeeds when the saved Pipeline Plan matches the preview', () => {
  const plan = {
    sourceTopicId: 10,
    window: { type: 'TUMBLE', size: 5, unit: 'MINUTE' },
    groupBy: ['product_id'],
    aggregations: [{ function: 'COUNT', field: '*', alias: 'order_count' }],
    filters: [],
  };
  assert.equal(isPersistedPipelinePlan(JSON.stringify({ ...plan, window: { unit: 'MINUTE', size: 5, type: 'TUMBLE' } }), plan), true);
  assert.equal(isPersistedPipelinePlan(null, plan), false);
  assert.equal(isPersistedPipelinePlan('{bad json', plan), false);
  assert.equal(isPersistedPipelinePlan(JSON.stringify({ ...plan, sourceTopicId: 11 }), plan), false);
});

test('pipeline detail routes are selected by pipeline type', () => {
  assert.equal(pipelineDetailPath(42, 'CUSTOM_JAR'), '/api/v1/platform/pipeline/pipelines/custom-jar/42');
  assert.equal(pipelineDetailPath(42, 'AI_SQL'), '/api/v1/platform/pipeline/pipelines/ai-sql/42');
});

import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({ entryPoints: ['src/pipelines/aiSqlForm.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const { initialAiSqlDraft, toAiSqlInput, validateAiSqlInput, isAiSqlPreview } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
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
  const preview = { previewId: 'p-1', expiresAt: '2030-01-01T00:00:00Z', pipelinePlan: { summary: 'Summary', steps: [{ title: 'Source', description: 'orders' }] }, generatedSql: 'SELECT 1', warnings: [], validation: { valid: true, errors: [] } };
  assert.equal(isAiSqlPreview(preview), true);
  assert.equal(isAiSqlPreview(null), false);
  assert.equal(isAiSqlPreview({ ...preview, generatedSql: '' }), false);
  assert.equal(isAiSqlPreview({ ...preview, expiresAt: 'not a date' }), false);
  assert.equal(isAiSqlPreview({ ...preview, warnings: [{}] }), false);
});

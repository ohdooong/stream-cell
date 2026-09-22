import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({ entryPoints: ['src/pipelines/aiSqlForm.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const { initialAiSqlDraft, toAiSqlInput, validateAiSqlInput, schemaFields, isAiSqlPreview } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);
const topic = { topicId: 10, topicName: 'orders', schemaJson: JSON.stringify({ properties: { eventTime: { type: 'string' }, amount: { type: 'number' } } }), timeField: 'eventTime' };
const draft = { ...initialAiSqlDraft, name: '  주문 집계  ', topicIds: [10], request: '  5분마다 합계  ', timeFields: { 10: 'eventTime', 99: 'deletedField' } };

test('request keeps the current owner and only selected Topic settings', () => {
  const input = toAiSqlInput(draft, 7);
  assert.equal(input.ownerUserId, 7);
  assert.equal(input.pipelineName, '주문 집계');
  assert.equal(input.naturalLanguageRequest, '5분마다 합계');
  assert.deepEqual(input.timeConfig.eventTimeFields, [{ topicId: 10, field: 'eventTime' }]);
  assert.deepEqual(validateAiSqlInput(input, { 10: topic }), []);
});

test('processing time and automatic tables omit stale hidden values', () => {
  const input = toAiSqlInput({ ...draft, timeMode: 'PROCESSING_TIME', tableName: 'stale_name', watermarkSeconds: NaN }, 1);
  assert.deepEqual(input.timeConfig.eventTimeFields, []);
  assert.equal(input.timeConfig.watermarkDelaySeconds, 0);
  assert.equal(input.sinkConfig.tableName, null);
  assert.deepEqual(validateAiSqlInput(input, { 10: topic }), []);
});

test('generation requires loaded schemas and a field in every event-time Topic', () => {
  const input = toAiSqlInput({ ...draft, topicIds: [10, 11], timeFields: { 10: 'notAField' } }, 1);
  const errors = validateAiSqlInput(input, { 10: topic });
  assert.ok(errors.some((error) => error.includes('notAField')));
  assert.ok(errors.some((error) => error.includes('#11')));
  assert.ok(validateAiSqlInput(toAiSqlInput(draft, 1), { 10: { ...topic, schemaJson: '' } }).some((error) => error.includes('Schema')));
});

test('invalid numeric settings, timezone, and table names are rejected', () => {
  const errors = validateAiSqlInput(toAiSqlInput({ ...draft, parallelism: 1.5, watermarkSeconds: -1, timezone: 'invalid/zone', tableNaming: 'CUSTOM', tableName: 'public.orders; DROP TABLE orders' }, 1), { 10: topic });
  assert.equal(errors.length, 4);
});

test('missing basic inputs prevent creation', () => {
  const errors = validateAiSqlInput(toAiSqlInput(initialAiSqlDraft, 0), {});
  assert.equal(errors.length, 4);
});

test('schema hints accept JSON Schema and Avro; unknown formats do not crash', () => {
  assert.deepEqual(schemaFields(topic.schemaJson), ['eventTime', 'amount']);
  assert.deepEqual(schemaFields('{"fields":[{"name":"createdAt"},{}]}'), ['createdAt']);
  assert.deepEqual(schemaFields('not-json'), []);
});

test('malformed preview responses cannot be treated as reviewed SQL', () => {
  const preview = { previewId: 'p-1', expiresAt: '2030-01-01T00:00:00Z', pipelinePlan: { summary: 'Summary', steps: [{ title: 'Source', description: 'orders' }] }, generatedSql: 'SELECT 1', warnings: [], validation: { valid: true, errors: [] } };
  assert.equal(isAiSqlPreview(preview), true);
  assert.equal(isAiSqlPreview(null), false);
  assert.equal(isAiSqlPreview({ ...preview, generatedSql: '' }), false);
  assert.equal(isAiSqlPreview({ ...preview, expiresAt: 'not a date' }), false);
  assert.equal(isAiSqlPreview({ ...preview, warnings: [{}] }), false);
});

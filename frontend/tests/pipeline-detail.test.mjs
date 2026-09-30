import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const result = await build({ entryPoints: ['src/pipelines/detailData.ts'], bundle: true, platform: 'node', format: 'esm', write: false });
const { aiSqlDetail, customJarDetail, programArgsText } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`);

test('Custom JAR detail supports registered configuration and artifact data', () => {
  const detail = customJarDetail({
    pipelineId: 1,
    customJobConfig: {
      entryClass: 'com.example.Job', parallelism: 2,
      inputTopicIds: '[10]', outputTopicIds: [11], programArgs: '{"env":"prod"}',
    },
    pipelineArtifact: { originalFileName: 'job.jar', storedFileName: '1-job.jar', flinkJarId: 'jar-1' },
  });
  assert.equal(detail.entryClass, 'com.example.Job');
  assert.equal(detail.parallelism, 2);
  assert.deepEqual(detail.inputTopicIds, [10]);
  assert.deepEqual(detail.outputTopicIds, [11]);
  assert.equal(detail.originalFileName, 'job.jar');
  assert.equal(detail.flinkJarId, 'jar-1');
  assert.equal(programArgsText(detail.programArgs), 'env=prod');
});

test('AI SQL detail recovers the single Topic and saved Plan', () => {
  const plan = { sourceTopicId: 10, window: { type: 'TUMBLE', size: 5, unit: 'MINUTE' }, groupBy: [], aggregations: [], filters: [] };
  const detail = aiSqlDetail({ pipelineId: 2, naturalLanguageRequest: '5분 집계', pipelinePlanJson: JSON.stringify(plan), generatedSql: 'SELECT 1' });
  assert.equal(detail.inputTopicId, 10);
  assert.equal(detail.naturalLanguageRequest, '5분 집계');
  assert.deepEqual(detail.pipelinePlan, plan);
  assert.equal(detail.generatedSql, 'SELECT 1');
});

test('AI SQL detail uses non-empty fallback fields when the primary response field is blank', () => {
  const detail = aiSqlDetail({
    pipelinePlanJson: '', generatedSql: '', pipelinePlan: { sourceTopicId: 12 },
    generatedFlinkSql: 'SELECT * FROM orders', aiSqlConfig: { inputTopicId: 12 },
  });
  assert.equal(detail.inputTopicId, 12);
  assert.deepEqual(detail.pipelinePlan, { sourceTopicId: 12 });
  assert.equal(detail.generatedSql, 'SELECT * FROM orders');
});

test('missing type-specific data stays empty instead of inventing values', () => {
  const jar = customJarDetail({ pipelineId: 3 });
  const ai = aiSqlDetail({ pipelineId: 4 });
  assert.deepEqual(jar.inputTopicIds, []);
  assert.equal(jar.originalFileName, undefined);
  assert.equal(ai.inputTopicId, null);
  assert.equal(ai.pipelinePlan, undefined);
});

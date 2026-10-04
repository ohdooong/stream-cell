import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const bundle = await build({ entryPoints: ['src/pipelines/customJarRegistration.ts'], bundle: true, platform: 'node', format: 'esm', write: false, define: { 'import.meta.env': '{}' } });
const { registerCustomJar, canRegisterCustomJar, customJarDraft } = await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`);
const pipeline = { pipelineId: 42, pipelineType: 'CUSTOM_JAR', pipelineStatus: 'CREATED', ownerUserId: 7, pipelineName: 'orders' };
const draft = { file: new File(['fixture'], 'orders.jar'), entryClass: 'com.example.Orders', parallelism: 2, inputId: 10, outputId: 0, args: 'env=test\nquery=a=b' };

test('failed upload retains the created ID and retry never creates a second pipeline', async () => {
  let createdId;
  let creates = 0;
  let uploads = 0;
  const api = {
    createPipeline: async () => { creates++; return pipeline; },
    getPipeline: async (id, type) => { assert.equal(id, 42); assert.equal(type, 'CUSTOM_JAR'); return pipeline; },
    uploadCustomJar: async (id, file, config) => {
      uploads++;
      assert.equal(createdId, id);
      assert.equal(file, draft.file);
      assert.equal(config.userId, 7);
      assert.deepEqual(config.inputTopicIds, [10]);
      assert.deepEqual(config.programArgs, { env: 'test', query: 'a=b' });
      if (uploads === 1) throw new Error('Upload failed');
      return { artifactId: 1, pipelineId: id };
    },
  };
  await assert.rejects(registerCustomJar({ draft, userId: 7, pipelineInput: pipeline, onCreated: (id) => { createdId = id; } }, api), /Upload failed/);
  assert.equal(createdId, 42);
  const result = await registerCustomJar({ pipelineId: createdId, draft, userId: 7 }, api);
  assert.equal(result.pipelineId, 42);
  assert.equal(creates, 1);
  assert.equal(uploads, 2);
});

test('lost upload response is reconciled with detail before retrying a POST', async () => {
  const result = await registerCustomJar({ pipelineId: 42, draft, userId: 7 }, {
    createPipeline: async () => assert.fail('must not create'),
    getPipeline: async () => ({ ...pipeline, pipelineStatus: 'ARTIFACT_UPLOADED', pipelineArtifact: { artifactId: 99, originalFileName: 'orders.jar' } }),
    uploadCustomJar: async () => assert.fail('must not upload twice'),
  });
  assert.equal(result.alreadyRegistered, true);
});

test('unsafe state changes and unreadable detail prevent retry uploads', async () => {
  for (const latest of [{ ...pipeline, pipelineStatus: 'RUNNING' }, { ...pipeline, pipelineId: 9 }, { ...pipeline, pipelineArtifact: { artifactId: 99 } }]) {
    await assert.rejects(registerCustomJar({ pipelineId: 42, draft, userId: 7 }, {
      createPipeline: async () => assert.fail('must not create'), getPipeline: async () => latest,
      uploadCustomJar: async () => assert.fail('must not upload'),
    }));
  }
});

test('only incomplete custom JAR registrations can be recovered and saved settings prefill the form', () => {
  assert.equal(canRegisterCustomJar(pipeline), true);
  assert.equal(canRegisterCustomJar({ ...pipeline, pipelineStatus: 'FAILED' }), true);
  assert.equal(canRegisterCustomJar({ ...pipeline, pipelineType: 'AI_SQL' }), false);
  assert.equal(canRegisterCustomJar({ ...pipeline, pipelineArtifact: { artifactId: 99 } }), false);
  assert.equal(canRegisterCustomJar({ ...pipeline, pipelineStatus: 'RUNNING' }), false);
  const saved = customJarDraft({ ...pipeline, customJobConfig: { entryClass: 'Job', parallelism: 3, inputTopicIds: [10], programArgs: { env: 'test' } } });
  assert.equal(saved.entryClass, 'Job');
  assert.equal(saved.parallelism, 3);
  assert.equal(saved.inputId, 10);
  assert.equal(saved.args, 'env=test');
  assert.equal(saved.file, null);
});

test('invalid JAR settings are rejected before creating the pipeline', async () => {
  for (const invalid of [{ ...draft, file: null }, { ...draft, parallelism: 9 }, { ...draft, entryClass: 'not a class' }]) {
    await assert.rejects(registerCustomJar({ draft: invalid, userId: 7, pipelineInput: pipeline }, {
      createPipeline: async () => assert.fail('must not create'),
    }));
  }
});

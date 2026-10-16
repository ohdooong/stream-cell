import assert from 'node:assert/strict';
import test from 'node:test';
import { build } from 'esbuild';

const bundle = await build({
  stdin: { contents: "export { platformApi } from './src/api/platform'; export { setAccessToken } from './src/api/client';", resolveDir: process.cwd(), loader: 'ts' },
  bundle: true, platform: 'node', format: 'esm', write: false, define: { 'import.meta.env': '{}' },
});
const { platformApi, setAccessToken } = await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`);
const originalFetch = globalThis.fetch;
const config = { userId: 7, entryClass: 'com.example.Job', inputTopicIds: [10], parallelism: 2, programArgs: { env: 'test' } };
const artifact = { artifactId: 99, pipelineId: 42, artifactType: 'CUSTOM_JAR', originalFileName: 'orders.jar', storedFileName: 'stored.jar', storedFilePath: '/fixtures/stored.jar' };
const detail = { pipelineId: 42, ownerUserId: 7, pipelineType: 'CUSTOM_JAR', pipelineStatus: 'ARTIFACT_UPLOADED', customJobConfig: config };
test.beforeEach(() => setAccessToken('test-jwt'));
test.afterEach(() => { globalThis.fetch = originalFetch; setAccessToken(null); });

test('multipart sends the selected input Topic as a JSON array, without output Topics, and preserves the upload receipt', async () => {
  globalThis.fetch = async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-jwt');
    if (url.endsWith('/42/custom-jar')) {
      assert.equal(init.method, 'POST');
      assert.ok(init.body instanceof FormData);
      assert.equal(init.body.get('file').name, 'orders.jar');
      const part = init.body.get('createCustomJobConfig');
      assert.equal(part.type, 'application/json');
      const sent = JSON.parse(await part.text());
      assert.deepEqual(sent, config);
      assert.equal('outputTopicIds' in sent, false);
      return Response.json({ body: artifact });
    }
    return Response.json({ body: detail });
  };
  await platformApi.uploadCustomJar(42, new File(['fixture'], 'orders.jar'), config);
  const registered = await platformApi.getPipeline(42, 'CUSTOM_JAR');
  assert.equal(registered.pipelineArtifact.originalFileName, 'orders.jar');
  assert.deepEqual(registered.customJobConfig.inputTopicIds, [10]);
});

test('server Artifact is preferred and upload receipts are cleared when the session changes', async () => {
  let responseDetail = detail;
  globalThis.fetch = async (url) => Response.json({ body: url.endsWith('/42/custom-jar') ? artifact : responseDetail });
  await platformApi.uploadCustomJar(42, new File(['fixture'], 'orders.jar'), config);
  responseDetail = { ...detail, pipelineArtifact: { artifactId: 100, originalFileName: 'new.jar' } };
  assert.equal((await platformApi.getPipeline(42, 'CUSTOM_JAR')).pipelineArtifact.originalFileName, 'new.jar');
  responseDetail = detail;
  setAccessToken('another-session');
  assert.equal((await platformApi.getPipeline(42, 'CUSTOM_JAR')).pipelineArtifact, undefined);
});

test('receipts are not applied to another owner or to an incomplete Pipeline', async () => {
  let responseDetail = { ...detail, ownerUserId: 8 };
  globalThis.fetch = async (url) => Response.json({ body: url.endsWith('/42/custom-jar') ? artifact : responseDetail });
  await platformApi.uploadCustomJar(42, new File(['fixture'], 'orders.jar'), config);
  assert.equal((await platformApi.getPipeline(42, 'CUSTOM_JAR')).pipelineArtifact, undefined);
  responseDetail = { ...detail, pipelineStatus: 'CREATED' };
  assert.equal((await platformApi.getPipeline(42, 'CUSTOM_JAR')).pipelineArtifact, undefined);
});

test('empty or invalid input Topics never reach the upload API', async () => {
  globalThis.fetch = async () => assert.fail('must not upload');
  for (const inputTopicIds of [[], [10, 11], [0], [NaN]]) {
    await assert.rejects(platformApi.uploadCustomJar(42, new File(['fixture'], 'orders.jar'), { ...config, inputTopicIds }), /입력 Topic/);
  }
});

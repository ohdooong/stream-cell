// Local-only fixture: the first upload fails and the next succeeds for each Pipeline.
// Run on port 18086; use a separate Vite instance with VITE_PROXY_TARGET pointing here.
import { createServer } from 'node:http';

const pipelines = new Map([[24, { pipelineId: 24, ownerUserId: 1, pipelineName: 'JAR 등록 대기 · 테스트', pipelineType: 'CUSTOM_JAR', pipelineStatus: 'CREATED' }]]);
const attempts = new Map();
let creates = 0;
createServer(async (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  const path = new URL(req.url, 'http://127.0.0.1:18086').pathname;
  const send = (body, status = 200) => { res.writeHead(status); res.end(JSON.stringify(body)); };
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const body = Buffer.concat(chunks);
  if (path === '/api/v1/web/user/items') return send([{ userId: 1, name: 'UI 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]);
  if (path === '/api/v1/web/my/pipeline/pipelines') return send({ body: [...pipelines.values()] });
  if (path === '/api/v1/platform/topic/topics') return send({ body: [{ topicId: 10, topicName: 'orders' }] });
  if (path === '/api/v1/platform/flink/cluster-overview') return send({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 4, 'jobs-running': 0, 'flink-version': 'UI fixture' } });
  if (path === '/api/v1/platform/pipeline/pipelines' && req.method === 'POST') {
    creates++;
    const pipeline = { ...JSON.parse(body), pipelineId: 41 + creates, pipelineStatus: 'CREATED' };
    pipelines.set(pipeline.pipelineId, pipeline);
    return send({ body: pipeline });
  }
  const detail = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/custom-jar\/(\d+)$/);
  if (detail) return send({ body: pipelines.get(Number(detail[1])) });
  const upload = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(\d+)\/custom-jar$/);
  if (upload && req.method === 'POST') {
    const id = Number(upload[1]);
    const attempt = (attempts.get(id) || 0) + 1;
    attempts.set(id, attempt);
    if (attempt === 1) return send({ message: '테스트용 JAR 업로드 실패입니다. 다시 등록해 주세요.' }, 500);
    const parts = await new Request('http://127.0.0.1:18086', { method: 'POST', headers: { 'Content-Type': req.headers['content-type'] }, body }).formData();
    const config = JSON.parse(await parts.get('createCustomJobConfig').text());
    const artifact = { artifactId: id, pipelineId: id, originalFileName: parts.get('file').name, storedFileName: 'stored.jar' };
    Object.assign(pipelines.get(id), { pipelineStatus: 'ARTIFACT_UPLOADED', pipelineArtifact: artifact, customJobConfig: config });
    return send({ body: artifact });
  }
  if (path === '/fixture/stats') return send({ creates, attempts: Object.fromEntries(attempts) });
  send({ message: '지원하지 않는 UI fixture 경로입니다.' }, 404);
}).listen(18086, '127.0.0.1', () => console.log('Custom JAR UI fixture: http://127.0.0.1:18086 (test data only)'));

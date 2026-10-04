// In-memory UI fixture only; never forwards requests to Flink or a real backend.
import { createServer } from 'node:http';

const pipelines = new Map([
  [71, { pipelineId: 71, ownerUserId: 1, pipelineName: '주문 집계 · AI SQL 중지 테스트', pipelineType: 'AI_SQL', pipelineStatus: 'RUNNING', naturalLanguageRequest: '주문 건수 집계', generatedSql: 'SELECT COUNT(*) FROM orders' }],
  [72, { pipelineId: 72, ownerUserId: 1, pipelineName: 'Custom JAR 중지 재시도 테스트', pipelineType: 'CUSTOM_JAR', pipelineStatus: 'RUNNING', pipelineArtifact: { artifactId: 72, originalFileName: 'orders.jar' }, customJobConfig: { entryClass: 'com.example.OrdersJob', parallelism: 1 } }],
]);
const stops = new Map();
const acceptedAt = new Map();
const syncs = new Map();
const events = [];
createServer(async (req, res) => {
  const path = new URL(req.url, 'http://127.0.0.1:18087').pathname;
  res.setHeader('Content-Type', 'application/json');
  const send = (body, status = 200) => { res.writeHead(status); res.end(JSON.stringify(body)); };
  if (path === '/api/v1/web/user/items') return send([{ userId: 1, name: 'UI 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]);
  if (path === '/api/v1/web/my/pipeline/pipelines') return send({ body: [...pipelines.values()] });
  if (path === '/api/v1/platform/topic/topics') return send({ body: [] });
  if (path === '/api/v1/platform/flink/cluster-overview') return send({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 2, 'jobs-running': 2, 'flink-version': 'UI fixture' } });
  const detail = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(custom-jar|ai-sql)\/(\d+)$/);
  if (detail) {
    const id = Number(detail[2]);
    const pipeline = pipelines.get(id);
    if (!pipeline) return send({ message: 'Not found' }, 404);
    return send({ body: pipeline });
  }
  const sync = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(\d+)\/status$/);
  if (sync && req.method === 'PUT') {
    const id = Number(sync[1]);
    const pipeline = pipelines.get(id);
    if (!pipeline) return send({ message: 'Not found' }, 404);
    events.push({ method: 'PUT', pipelineId: id, at: Date.now() });
    syncs.set(id, (syncs.get(id) || 0) + 1);
    if (id === 72 && syncs.get(id) === 1) return send({ message: '테스트용 일시적 상태 동기화 실패' }, 500);
    if (acceptedAt.has(id)) {
      const elapsed = Date.now() - acceptedAt.get(id);
      pipeline.pipelineStatus = elapsed < 3000 ? 'RUNNING' : elapsed < 9000 ? 'STOPPING' : 'STOPPED';
    }
    return send({ status: 200, body: pipeline.pipelineStatus });
  }
  const stop = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/deployment\/(\d+)\/stop$/);
  if (stop && req.method === 'POST') {
    const id = Number(stop[1]);
    stops.set(id, (stops.get(id) || 0) + 1);
    events.push({ method: 'POST', pipelineId: id, at: Date.now() });
    if (id === 72 && stops.get(id) === 1) return send({ message: '테스트용 일시적 중지 실패입니다. 다시 시도해 주세요.' }, 500);
    acceptedAt.set(id, Date.now());
    return send({ status: 202, message: 'Job Cancel 성공', body: null }, 202);
  }
  if (path === '/fixture/stats') return send({ stops: Object.fromEntries(stops), syncs: Object.fromEntries(syncs), events });
  return send({ message: 'Unsupported fixture path' }, 404);
}).listen(18087, '127.0.0.1', () => console.log('Pipeline stop fixture: http://127.0.0.1:18087 (test data only)'));

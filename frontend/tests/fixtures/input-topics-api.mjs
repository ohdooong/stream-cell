// Isolated test API. No real Topic permissions, Pipelines, or Flink jobs are changed.
import { createServer } from 'node:http';
const topics = [{ topicId: 10, topicName: 'deploy-orders', displayName: '배포 가능 주문', messageFormat: 'JSON', schemaJson: '{"properties":{"amount":{"type":"number"}}}' }, { topicId: 11, topicName: 'view-only-inventory', displayName: '조회만 가능' }];
const pipeline = { pipelineId: 24, ownerUserId: 1, pipelineName: '권한이 변경된 JAR 등록', pipelineType: 'CUSTOM_JAR', pipelineStatus: 'CREATED', customJobConfig: { inputTopicIds: [11], entryClass: 'com.example.Job', parallelism: 1 } };
const mode = process.env.INPUT_TOPICS_FIXTURE_MODE || 'retry';
const events = [];
let inputRequests = 0;
createServer((req, res) => {
  const path = new URL(req.url, 'http://127.0.0.1:18090').pathname;
  events.push({ method: req.method, path });
  const send = (body, status = 200) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(body)); };
  if (path === '/api/v1/web/user/items') return send([{ userId: 1, name: 'Topic 권한 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]);
  if (path === '/api/v1/web/my/pipeline/pipelines') return send({ body: [pipeline] });
  if (path === '/api/v1/platform/topic/topics') return send({ body: topics });
  if (path === '/api/v1/platform/topic/input-topics' && req.method === 'GET') {
    inputRequests++;
    if (mode === 'retry' && inputRequests <= 2) return send({ message: '테스트용 권한 Topic 조회 실패' }, 503);
    return send({ body: mode === 'empty' ? [] : [topics[0]] });
  }
  if (path === '/api/v1/platform/topic/topics/10') return send({ body: topics[0] });
  if (path === '/api/v1/platform/pipeline/pipelines/custom-jar/24') return send({ body: pipeline });
  if (path === '/api/v1/platform/flink/cluster-overview') return send({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 4, 'jobs-running': 0, 'flink-version': 'Test fixture' } });
  if (path === '/fixture/stats') return send({ inputRequests, events });
  return send({ message: 'Unsupported test path' }, 404);
}).listen(18090, '127.0.0.1', () => console.log(`Input Topics fixture (${mode}) http://127.0.0.1:18090`));

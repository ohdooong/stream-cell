// In-memory UI fixture: no Kafka or real backend requests.
import { createServer } from 'node:http';

let calls = 0;
let topics = [];
const topic = { topicId: 1, topicName: 'orders', displayName: '주문 이벤트', messageFormat: 'JSON', schemaJson: '{}', timeField: 'event_time' };
createServer((req, res) => {
  const path = new URL(req.url, 'http://127.0.0.1:18089').pathname;
  const send = (data, status = 200) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(data)); };
  if (path === '/api/v1/web/user/items') return send([{ userId: 1, name: 'Topic 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]);
  if (path === '/api/v1/web/my/pipeline/pipelines') return send({ body: [] });
  if (path === '/api/v1/platform/flink/cluster-overview') return send({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 4, 'flink-version': 'UI fixture' } });
  if (path === '/api/v1/platform/topic/topics') return send({ body: topics });
  if (path === '/api/v1/platform/topic/topics/1') return send({ body: topic });
  if (path === '/api/v1/platform/topic/sync' && req.method === 'POST') {
    calls++;
    if (calls === 3) return send({ message: '테스트용 Kafka 동기화 실패' }, 500);
    topics = calls === 2 ? [] : [topic];
    return send({ status: 200, body: topics });
  }
  return send({ message: 'Unsupported fixture path' }, 404);
}).listen(18089, '127.0.0.1', () => console.log('Topic sync fixture: http://127.0.0.1:18089'));

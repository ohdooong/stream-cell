// Isolated UI test data only. Never forwards requests to a backend or Flink.
import { createServer } from 'node:http';
const pipelines = [
  { pipelineId: 81, ownerUserId: 1, pipelineName: 'IoT 에너지 집계', pipelineType: 'AI_SQL', pipelineStatus: 'FAILED', generatedSql: 'SELECT COUNT(*) FROM energy' },
  { pipelineId: 82, ownerUserId: 1, pipelineName: 'Custom JAR 실패 조회 재시도', pipelineType: 'CUSTOM_JAR', pipelineStatus: 'FAILED' },
  { pipelineId: 83, ownerUserId: 1, pipelineName: 'Exception 수집 대기', pipelineType: 'AI_SQL', pipelineStatus: 'FAILED' },
  { pipelineId: 84, ownerUserId: 1, pipelineName: '정상 실행', pipelineType: 'AI_SQL', pipelineStatus: 'RUNNING' },
];
const counts = {};
const failure = { errorExceptionName: 'org.apache.flink.runtime.JobException', errorMessage: 'org.apache.flink.runtime.JobException: Recovery is suppressed by NoRestartBackoffTimeStrategy\n\tat ExecutionFailureHandler.handleFailure(ExecutionFailureHandler.java:180)\nCaused by: java.io.IOException: Failed to deserialize consumer record\nCaused by: org.apache.flink.formats.json.JsonParseException: Fail to deserialize at field: event_time.\n<test-only-literal> & "not HTML"', errorTimestamp: '2026-10-05 13:49:59.480000' };
createServer((req, res) => {
  const path = new URL(req.url, 'http://127.0.0.1:18089').pathname;
  const send = (body, status = 200) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(body)); };
  if (path === '/api/v1/web/user/items') return send([{ userId: 1, name: '실패 조회 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]);
  if (path === '/api/v1/web/my/pipeline/pipelines') return send({ body: pipelines });
  if (path === '/api/v1/platform/topic/topics') return send({ body: [] });
  if (path === '/api/v1/platform/flink/cluster-overview') return send({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 2, 'jobs-running': 1, 'jobs-failed': 3, 'flink-version': 'Fixture' } });
  const detail = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(custom-jar|ai-sql)\/(\d+)$/);
  if (detail) return send({ body: pipelines.find((item) => item.pipelineId === Number(detail[2])) });
  const match = path.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(\d+)\/failures$/);
  if (match && req.method === 'GET') {
    const id = Number(match[1]); counts[id] = (counts[id] || 0) + 1;
    if (id === 82 && counts[id] <= 2) return send({ message: '테스트용 일시적 실패 조회 오류' }, 500);
    return send({ body: id === 83 ? null : { ...failure, errorExceptionName: id === 82 ? 'java.lang.IllegalStateException' : failure.errorExceptionName } });
  }
  if (path === '/fixture/stats') return send(counts);
  return send({ message: 'Unsupported test path' }, 404);
}).listen(18089, '127.0.0.1', () => console.log('Pipeline failures fixture http://127.0.0.1:18089'));

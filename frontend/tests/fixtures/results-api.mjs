// Local-only UI fixture. Never use this as a production backend.
// Run: node tests/fixtures/results-api.mjs
// Start Vite on a separate port with VITE_AUTH_ENABLED=false and
// VITE_PROXY_TARGET=http://127.0.0.1:18085 (clear VITE_API_BASE_URL).
import { createServer } from 'node:http';

const pipelines = [
  { pipelineId: 12, ownerUserId: 1, pipelineName: '다중 그룹 주문 집계 · 테스트', pipelineType: 'AI_SQL', pipelineStatus: 'RUNNING' },
  { pipelineId: 24, ownerUserId: 1, pipelineName: '빈 결과 Pipeline · 테스트', pipelineType: 'CUSTOM_JAR', pipelineStatus: 'STOPPED' },
];
function results(pipelineId, range, limit) {
  const rows = [];
  const now = Math.floor(Date.now() / 60000) * 60000;
  if (pipelineId === 12 && range !== 5) {
    for (let minute = 0; minute < Math.min(range, 20); minute++) {
      for (const [product, productId] of ['상품 A', '상품 B', '상품 C'].entries()) {
        for (const [area, region] of ['서울', '부산'].entries()) {
          rows.push({ windowStart: new Date(now - minute * 60000).toISOString(), productId, region, orderCount: Math.round(100 + product * 30 + area * 15 + Math.sin(minute * .5 + product + area) * 25), avgPaymentAmount: Math.round(30000 + product * 6000 + Math.sin(minute * .5 + area) * 2000) });
        }
      }
    }
  }
  return {
    pipelineId, pipelineStatus: pipelineId === 12 ? 'RUNNING' : 'STOPPED', updatedAt: new Date().toISOString(),
    columns: [{ key: 'windowStart', label: '윈도우 시작', type: 'TIMESTAMP' }, { key: 'productId', label: '상품', type: 'STRING' }, { key: 'region', label: '지역', type: 'STRING' }, { key: 'orderCount', label: '주문 건수', type: 'NUMBER' }, { key: 'avgPaymentAmount', label: '평균 결제금액', type: 'NUMBER' }],
    rows: rows.slice(0, limit),
    chart: { xKey: 'windowStart', series: [{ key: 'orderCount', label: '주문 건수', groupKeys: ['productId', 'region'], unit: '건' }, { key: 'avgPaymentAmount', label: '평균 결제금액', groupKeys: ['productId', 'region'], unit: '원' }] },
  };
}
createServer((req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1:18085');
  res.setHeader('Content-Type', 'application/json');
  const resultMatch = url.pathname.match(/^\/api\/v1\/platform\/pipeline\/pipelines\/(\d+)\/results$/);
  if (resultMatch) {
    const limit = Number(url.searchParams.get('limit') || 100);
    if (limit === 500) { res.writeHead(503); res.end(JSON.stringify({ message: '테스트용 조회 실패입니다.' })); return; }
    res.end(JSON.stringify({ body: results(Number(resultMatch[1]), Number(url.searchParams.get('rangeMinutes') || 20), limit) }));
  } else if (url.pathname === '/api/v1/web/user/items') res.end(JSON.stringify([{ userId: 1, name: 'UI 테스트 사용자', email: 'fixture@example.test', status: 'ACTIVE' }]));
  else if (url.pathname === '/api/v1/web/my/pipeline/pipelines') res.end(JSON.stringify({ body: pipelines }));
  else if (url.pathname === '/api/v1/platform/topic/topics') res.end(JSON.stringify({ body: [] }));
  else if (url.pathname === '/api/v1/platform/flink/cluster-overview') res.end(JSON.stringify({ body: { taskmanagers: 1, 'slots-total': 4, 'slots-available': 3, 'jobs-running': 1, 'flink-version': 'UI fixture' } }));
  else { res.writeHead(404); res.end(JSON.stringify({ message: '이 경로는 UI fixture에서 지원하지 않습니다.' })); }
}).listen(18085, '127.0.0.1', () => console.log('Results UI fixture: http://127.0.0.1:18085 (test data only)'));

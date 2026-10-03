export type ResultColumn = { key: string; label: string; type: 'TIMESTAMP' | 'STRING' | 'NUMBER' };
export type ResultRow = Record<string, string | number>;
export type DemoPipeline = {
  id: number; name: string; type: 'AI_SQL' | 'CUSTOM_JAR'; description: string;
  groupKey: string; groups: string[]; columns: ResultColumn[];
  metrics: Array<{ key: string; label: string; unit: string }>;
};

export const demoPipelines: DemoPipeline[] = [
  {
    id: 12, name: '상품별 주문 집계', type: 'AI_SQL', description: '주문 이벤트를 상품별로 묶어 주문 건수와 평균 결제금액을 계산합니다.',
    groupKey: 'productId', groups: ['상품 A', '상품 B', '상품 C'],
    columns: [
      { key: 'windowStart', label: 'Window Start', type: 'TIMESTAMP' },
      { key: 'productId', label: '상품', type: 'STRING' },
      { key: 'orderCount', label: '주문 건수', type: 'NUMBER' },
      { key: 'avgPaymentAmount', label: '평균 결제금액 (원)', type: 'NUMBER' },
    ],
    metrics: [{ key: 'orderCount', label: '주문 건수', unit: '건' }, { key: 'avgPaymentAmount', label: '평균 결제금액', unit: '원' }],
  },
  {
    id: 24, name: '채널별 세션 분석', type: 'CUSTOM_JAR', description: '사용자 행동 이벤트를 분석해 채널별 활성 세션과 평균 체류시간을 계산합니다.',
    groupKey: 'channel', groups: ['Web', 'App', 'Partner'],
    columns: [
      { key: 'windowStart', label: 'Window Start', type: 'TIMESTAMP' },
      { key: 'channel', label: '채널', type: 'STRING' },
      { key: 'activeSessions', label: '활성 세션', type: 'NUMBER' },
      { key: 'avgDurationSeconds', label: '평균 체류시간 (초)', type: 'NUMBER' },
    ],
    metrics: [{ key: 'activeSessions', label: '활성 세션', unit: '개' }, { key: 'avgDurationSeconds', label: '평균 체류시간', unit: '초' }],
  },
];

export function createDemoSnapshot(pipeline: DemoPipeline, anchorTime: number, tick: number, updatedAt: string, empty = false) {
  const rows: ResultRow[] = [];
  if (!empty) {
  for (let windowIndex = 0; windowIndex < 20; windowIndex += 1) {
      const sequence = tick - windowIndex;
      const windowStart = new Date(anchorTime + sequence * 60_000).toISOString();
      pipeline.groups.forEach((group, groupIndex) => {
        const wave = Math.sin(sequence * .62 + groupIndex * 1.7);
        const count = Math.round((pipeline.id === 12 ? 120 : 230) + groupIndex * 38 + wave * 32 + Math.cos(sequence * .21) * 12);
        rows.push(pipeline.id === 12
          ? { windowStart, productId: group, orderCount: count, avgPaymentAmount: Math.round(32_000 + groupIndex * 4_300 + wave * 2_100) }
          : { windowStart, channel: group, activeSessions: count, avgDurationSeconds: Math.round(150 + groupIndex * 32 + wave * 24) });
      });
    }
  }
  return {
    pipelineId: pipeline.id, pipelineStatus: 'RUNNING', updatedAt,
    columns: pipeline.columns, rows,
    chart: { xKey: 'windowStart', series: pipeline.metrics.map((metric) => ({ key: metric.key, label: metric.label, groupKeys: [pipeline.groupKey] })) },
  };
}

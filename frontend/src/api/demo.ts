import type { User } from './auth';

export const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true';

export const demoUser: User = {
  userId: 1,
  username: 'demo@streamcell.io',
  displayName: '데모 사용자',
  roles: ['ROLE_ADMIN'],
};

export const demoTopics = [
  { topicId: 1, topicName: 'orders.created.v1', displayName: '주문 생성', description: '신규 주문 이벤트 스트림', messageFormat: 'JSON' },
  { topicId: 2, topicName: 'payments.completed.v1', displayName: '결제 완료', description: '완료된 결제 이벤트', messageFormat: 'JSON' },
  { topicId: 3, topicName: 'inventory.updated.v1', displayName: '재고 변경', description: '상품별 재고 변경 이벤트', messageFormat: 'AVRO' },
];

export const demoPipelines = [
  { pipelineId: 1, pipelineName: '주문 실시간 집계', description: '주문 이벤트를 분 단위로 집계합니다.', pipelineType: 'FLINK_SQL', pipelineStatus: 'RUNNING' },
  { pipelineId: 2, pipelineName: '결제 이상 감지', description: '결제 흐름의 이상 패턴을 탐지합니다.', pipelineType: 'FLINK_SQL', pipelineStatus: 'RUNNING' },
  { pipelineId: 3, pipelineName: '재고 동기화', description: '재고 변경을 분석 저장소에 반영합니다.', pipelineType: 'CUSTOM_JOB', pipelineStatus: 'STOPPED' },
];

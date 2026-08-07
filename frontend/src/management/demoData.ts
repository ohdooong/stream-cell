export type TopicItem = {
  id: number;
  name: string;
  displayName: string;
  description: string;
  format: 'JSON' | 'AVRO';
  partitions: number;
  retention: string;
  eventTimeField: string;
  schema: string;
  throughput: string;
};

export type PipelineItem = {
  id: number;
  name: string;
  description: string;
  type: 'AI_SQL' | 'CUSTOM_JAR';
  status: 'RUNNING' | 'STOPPED' | 'FAILED' | 'DRAFT';
  jobId?: string;
  parallelism: number;
  inputTopics: string[];
  updatedAt: string;
};

export type PermissionItem = {
  id: number;
  topicId: number;
  user: string;
  email: string;
  role: 'READ' | 'WRITE' | 'ADMIN';
  grantedAt: string;
};

export const clusterSummary = {
  status: 'HEALTHY',
  version: '1.19.1',
  jobManager: 'flink-jobmanager-0',
  taskManagers: 3,
  slotsTotal: 12,
  slotsAvailable: 5,
  jobsRunning: 3,
  jobsFailed: 1,
  uptime: '18d 07h 32m',
};

export const taskManagers = [
  { id: 'tm-a13f', host: '10.20.1.21:6122', slots: '3 / 4', cpu: 42, memory: 68, heartbeat: '방금 전' },
  { id: 'tm-b82c', host: '10.20.1.22:6122', slots: '2 / 4', cpu: 36, memory: 54, heartbeat: '2초 전' },
  { id: 'tm-c91e', host: '10.20.1.23:6122', slots: '2 / 4', cpu: 51, memory: 73, heartbeat: '1초 전' },
];

export const initialTopics: TopicItem[] = [
  { id: 1, name: 'orders.created.v1', displayName: '주문 생성', description: '신규 주문 이벤트 스트림', format: 'JSON', partitions: 6, retention: '7 days', eventTimeField: 'createdAt', throughput: '2.8K/s', schema: '{\n  "type": "object",\n  "properties": {\n    "orderId": { "type": "string" },\n    "amount": { "type": "number" },\n    "createdAt": { "type": "string", "format": "date-time" }\n  },\n  "required": ["orderId", "createdAt"]\n}' },
  { id: 2, name: 'payments.completed.v1', displayName: '결제 완료', description: '완료된 결제 이벤트', format: 'JSON', partitions: 6, retention: '7 days', eventTimeField: 'paidAt', throughput: '2.1K/s', schema: '{\n  "type": "object",\n  "properties": {\n    "paymentId": { "type": "string" },\n    "orderId": { "type": "string" },\n    "paidAt": { "type": "string", "format": "date-time" }\n  }\n}' },
  { id: 3, name: 'inventory.updated.v1', displayName: '재고 변경', description: '상품별 재고 변경 이벤트', format: 'AVRO', partitions: 3, retention: '3 days', eventTimeField: 'occurredAt', throughput: '860/s', schema: '{\n  "type": "record",\n  "name": "InventoryUpdated",\n  "fields": [\n    { "name": "sku", "type": "string" },\n    { "name": "quantity", "type": "int" },\n    { "name": "occurredAt", "type": "string" }\n  ]\n}' },
  { id: 4, name: 'users.activity.v1', displayName: '사용자 행동', description: '서비스 내 사용자 행동 이벤트', format: 'JSON', partitions: 12, retention: '14 days', eventTimeField: 'timestamp', throughput: '7.4K/s', schema: '{\n  "type": "object",\n  "properties": {\n    "userId": { "type": "string" },\n    "event": { "type": "string" },\n    "timestamp": { "type": "integer" }\n  }\n}' },
];

export const initialPipelines: PipelineItem[] = [
  { id: 1, name: '주문 실시간 집계', description: '분 단위 주문 금액과 건수를 집계합니다.', type: 'AI_SQL', status: 'RUNNING', jobId: '2e3f9d8a71c4', parallelism: 3, inputTopics: ['orders.created.v1'], updatedAt: '방금 전' },
  { id: 2, name: '결제 이상 감지', description: '결제 흐름의 이상 패턴을 탐지합니다.', type: 'AI_SQL', status: 'RUNNING', jobId: '9ab4c7e12fd8', parallelism: 2, inputTopics: ['payments.completed.v1'], updatedAt: '3분 전' },
  { id: 3, name: '재고 동기화', description: '재고 변경을 분석 저장소에 반영합니다.', type: 'CUSTOM_JAR', status: 'STOPPED', jobId: '6ca21e07bd13', parallelism: 2, inputTopics: ['inventory.updated.v1'], updatedAt: '1시간 전' },
  { id: 4, name: '행동 세션 분석', description: '사용자 행동을 세션 단위로 분석합니다.', type: 'CUSTOM_JAR', status: 'FAILED', jobId: 'fa190d3c48ab', parallelism: 4, inputTopics: ['users.activity.v1'], updatedAt: '12분 전' },
];

export const initialPermissions: PermissionItem[] = [
  { id: 1, topicId: 1, user: '오승환', email: 'seunghwan@streamcell.io', role: 'ADMIN', grantedAt: '2026-08-01' },
  { id: 2, topicId: 1, user: '김데이터', email: 'data.kim@streamcell.io', role: 'READ', grantedAt: '2026-08-03' },
  { id: 3, topicId: 1, user: '이플랫폼', email: 'platform.lee@streamcell.io', role: 'WRITE', grantedAt: '2026-08-05' },
  { id: 4, topicId: 2, user: '오승환', email: 'seunghwan@streamcell.io', role: 'READ', grantedAt: '2026-08-01' },
  { id: 5, topicId: 3, user: '오승환', email: 'seunghwan@streamcell.io', role: 'WRITE', grantedAt: '2026-08-06' },
];

export const deployments = [
  { id: 'DEP-1042', pipelineId: 1, version: 'v12', status: 'RUNNING', startedAt: '2026-08-08 09:21', duration: '5h 18m', operator: '오승환' },
  { id: 'DEP-1038', pipelineId: 1, version: 'v11', status: 'FINISHED', startedAt: '2026-08-07 15:08', duration: '18h 02m', operator: '오승환' },
  { id: 'DEP-1021', pipelineId: 1, version: 'v10', status: 'FAILED', startedAt: '2026-08-06 11:42', duration: '3m 41s', operator: '김데이터' },
  { id: 'DEP-1040', pipelineId: 2, version: 'v7', status: 'RUNNING', startedAt: '2026-08-08 08:12', duration: '6h 27m', operator: '김데이터' },
];

export const resultRows = [
  { time: '14:35:00', window: '14:34–14:35', orders: 1284, amount: '₩48,293,000', avg: '₩37,611', change: '+12.4%' },
  { time: '14:34:00', window: '14:33–14:34', orders: 1142, amount: '₩42,918,000', avg: '₩37,581', change: '+4.1%' },
  { time: '14:33:00', window: '14:32–14:33', orders: 1097, amount: '₩41,207,000', avg: '₩37,563', change: '-2.3%' },
  { time: '14:32:00', window: '14:31–14:32', orders: 1123, amount: '₩43,104,000', avg: '₩38,383', change: '+7.8%' },
  { time: '14:31:00', window: '14:30–14:31', orders: 1042, amount: '₩39,526,000', avg: '₩37,933', change: '+1.2%' },
];

export const chartValues = [38, 44, 41, 52, 48, 57, 61, 58, 69, 74, 67, 79, 72, 84, 81, 91, 88, 96, 92, 100];

export const failures = [
  { id: 'FAIL-229', pipelineId: 4, pipeline: '행동 세션 분석', occurredAt: '2026-08-08 14:27:18', stage: 'SessionWindowOperator', severity: 'CRITICAL', exception: 'org.apache.flink.runtime.JobException: Recovery is suppressed by NoRestartBackoffTimeStrategy\nCaused by: java.lang.OutOfMemoryError: Java heap space\n  at com.streamcell.jobs.SessionAccumulator.add(SessionAccumulator.java:84)\n  at org.apache.flink.streaming.runtime.operators.windowing.WindowOperator.processElement(WindowOperator.java:392)', summary: '세션 윈도우에 장시간 누적된 상태 데이터가 TaskManager의 Heap 한도를 초과했습니다.', causes: ['사용자 행동 토픽의 처리량이 평소 대비 3.8배 증가했습니다.', '세션 Gap이 30분으로 길고 State TTL이 설정되지 않았습니다.', 'TaskManager managed memory가 현재 부하에 비해 부족합니다.'], actions: ['State TTL을 45분으로 설정하고 만료된 세션을 정리하세요.', 'Pipeline Parallelism을 4에서 8로 확장하세요.', 'RocksDB State Backend로 전환해 Heap 사용량을 줄이세요.'] },
  { id: 'FAIL-224', pipelineId: 1, pipeline: '주문 실시간 집계', occurredAt: '2026-08-06 11:45:41', stage: 'KafkaSource', severity: 'HIGH', exception: 'org.apache.kafka.common.errors.TimeoutException: Timeout expired while fetching topic metadata', summary: 'Kafka broker와의 일시적인 네트워크 지연으로 Topic metadata 조회가 시간 초과되었습니다.', causes: ['Kafka broker 응답 시간이 30초를 초과했습니다.'], actions: ['Kafka 연결 Timeout을 60초로 늘리세요.', 'Broker 네트워크 상태를 확인하세요.'] },
];

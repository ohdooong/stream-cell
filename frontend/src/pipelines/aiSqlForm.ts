import type { AiSqlInput, AiSqlPreview } from '../api/aiSql';
import type { Topic } from '../api/platform';

export type AiSqlDraft = {
  name: string;
  description: string;
  topicId: number | null;
  request: string;
  timeMode: AiSqlInput['timeConfig']['mode'];
  timeFields: Record<number, string>;
  watermarkSeconds: number;
  timezone: string;
  tableNaming: 'AUTO' | 'CUSTOM';
  tableName: string;
  startupMode: 'LATEST' | 'EARLIEST';
  parallelism: number;
};

export const initialAiSqlDraft: AiSqlDraft = {
  name: '', description: '', topicId: null, request: '', timeMode: 'EVENT_TIME',
  timeFields: {}, watermarkSeconds: 5, timezone: 'Asia/Seoul',
  tableNaming: 'AUTO', tableName: '', startupMode: 'LATEST', parallelism: 1,
};

// These are field-name hints only. Type/format compatibility is checked by the backend.
export function schemaFields(schema?: string): string[] {
  if (!schema) return [];
  try {
    const parsed = JSON.parse(schema);
    if (parsed?.properties && typeof parsed.properties === 'object') return Object.keys(parsed.properties);
    if (Array.isArray(parsed?.fields)) return parsed.fields.map((field: { name?: unknown }) => field?.name).filter((name: unknown): name is string => typeof name === 'string');
  } catch { /* The raw schema remains visible even when its format is not recognized. */ }
  return [];
}

export function toAiSqlInput(draft: AiSqlDraft, userId: number): AiSqlInput {
  return {
    ownerUserId: userId,
    pipelineName: draft.name.trim(),
    description: draft.description.trim(),
    pipelineType: 'AI_SQL',
    inputTopicIds: draft.topicId === null ? [] : [draft.topicId],
    naturalLanguageRequest: draft.request.trim(),
    timeConfig: {
      mode: draft.timeMode,
      eventTimeFields: draft.timeMode === 'EVENT_TIME' && draft.topicId !== null
        ? [{ topicId: draft.topicId, field: (draft.timeFields[draft.topicId] || '').trim() }] : [],
      watermarkDelaySeconds: draft.timeMode === 'EVENT_TIME' ? draft.watermarkSeconds : 0,
      timezone: draft.timezone.trim(),
    },
    sinkConfig: {
      sinkType: 'POSTGRESQL', tableNaming: draft.tableNaming,
      tableName: draft.tableNaming === 'CUSTOM' ? draft.tableName.trim() : null,
    },
    executionConfig: { startupMode: draft.startupMode, parallelism: draft.parallelism },
  };
}

export function validateAiSqlInput(input: AiSqlInput, topics: Record<number, Topic | undefined>): string[] {
  const errors: string[] = [];
  if (!Number.isSafeInteger(input.ownerUserId) || input.ownerUserId < 1) errors.push('현재 사용자 정보를 확인해 주세요.');
  if (!input.pipelineName || input.pipelineName.length > 100) errors.push('Pipeline 이름을 1~100자로 입력해 주세요.');
  if (input.description.length > 1000) errors.push('설명은 1,000자 이하로 입력해 주세요.');
  if (input.inputTopicIds.length !== 1) errors.push('입력 Topic을 하나만 선택해 주세요.');
  if (!input.naturalLanguageRequest) errors.push('자연어 처리 요청을 입력해 주세요.');
  if (input.naturalLanguageRequest.length > 4000) errors.push('자연어 요청은 4,000자 이하로 입력해 주세요.');
  input.inputTopicIds.forEach((topicId) => {
    const topic = topics[topicId];
    if (!topic) { errors.push(`Topic #${topicId}의 상세 정보를 불러와 주세요.`); return; }
    if (!topic.schemaJson?.trim()) errors.push(`${topic.topicName}의 Schema를 Topic 관리에서 먼저 등록해 주세요.`);
    if (input.timeConfig.mode === 'EVENT_TIME') {
      const field = input.timeConfig.eventTimeFields.find((value) => value.topicId === topicId)?.field;
      const fields = schemaFields(topic.schemaJson);
      if (!field) errors.push(`${topic.topicName}의 Event Time 필드를 입력해 주세요.`);
      else if (fields.length && !fields.includes(field)) errors.push(`${topic.topicName}의 Schema에 '${field}' 필드가 없습니다.`);
    }
  });
  if (!Number.isInteger(input.executionConfig.parallelism) || input.executionConfig.parallelism < 1) errors.push('병렬도는 1 이상의 정수여야 합니다.');
  if (!Number.isInteger(input.timeConfig.watermarkDelaySeconds) || input.timeConfig.watermarkDelaySeconds < 0) errors.push('Watermark 지연은 0 이상의 정수여야 합니다.');
  try { new Intl.DateTimeFormat('en', { timeZone: input.timeConfig.timezone }); }
  catch { errors.push('유효한 시간대를 입력해 주세요. 예: Asia/Seoul, UTC'); }
  if (!input.timeConfig.timezone) errors.push('시간대를 입력해 주세요.');
  if (input.sinkConfig.tableNaming === 'CUSTOM' && !/^[a-z][a-z0-9_]{0,62}$/.test(input.sinkConfig.tableName || '')) {
    errors.push('테이블명은 영문 소문자로 시작하는 63자 이하의 소문자·숫자·밑줄이어야 합니다.');
  }
  return errors;
}

export function isAiSqlPreview(value: unknown): value is AiSqlPreview {
  const preview = value as AiSqlPreview | null;
  return Boolean(preview && typeof preview.previewId === 'string' && preview.previewId
    && typeof preview.expiresAt === 'string' && Number.isFinite(Date.parse(preview.expiresAt))
    && typeof preview.generatedSql === 'string' && preview.generatedSql.trim()
    && typeof preview.pipelinePlan?.summary === 'string' && Array.isArray(preview.pipelinePlan.steps)
    && preview.pipelinePlan.steps.every((step) => step && typeof step.title === 'string' && typeof step.description === 'string')
    && Array.isArray(preview.warnings) && preview.warnings.every((warning) => typeof warning === 'string')
    && typeof preview.validation?.valid === 'boolean' && Array.isArray(preview.validation.errors)
    && preview.validation.errors.every((error) => typeof error === 'string'));
}

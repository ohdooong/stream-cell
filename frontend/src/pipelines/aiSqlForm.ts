import type { AiSqlInput, AiSqlPreview } from '../api/aiSql';
import type { Topic } from '../api/platform';

export type AiSqlDraft = {
  name: string;
  description: string;
  topicId: number | null;
  request: string;
};

export const initialAiSqlDraft: AiSqlDraft = {
  name: '', description: '', topicId: null, request: '',
};

export function toAiSqlInput(draft: AiSqlDraft, userId: number): AiSqlInput {
  return {
    ownerUserId: userId,
    pipelineName: draft.name.trim(),
    description: draft.description.trim(),
    pipelineType: 'AI_SQL',
    inputTopicId: draft.topicId ?? 0,
    naturalLanguageRequest: draft.request.trim(),
  };
}

export function validateAiSqlInput(input: AiSqlInput, topics: Record<number, Topic | undefined>): string[] {
  const errors: string[] = [];
  if (!Number.isSafeInteger(input.ownerUserId) || input.ownerUserId < 1) errors.push('현재 사용자 정보를 확인해 주세요.');
  if (!input.pipelineName || input.pipelineName.length > 100) errors.push('Pipeline 이름을 1~100자로 입력해 주세요.');
  if (input.description.length > 1000) errors.push('설명은 1,000자 이하로 입력해 주세요.');
  if (!Number.isSafeInteger(input.inputTopicId) || input.inputTopicId < 1) errors.push('입력 Topic을 하나 선택해 주세요.');
  if (!input.naturalLanguageRequest) errors.push('자연어 처리 요청을 입력해 주세요.');
  if (input.naturalLanguageRequest.length > 4000) errors.push('자연어 요청은 4,000자 이하로 입력해 주세요.');
  if (input.inputTopicId > 0) {
    const topic = topics[input.inputTopicId];
    if (!topic) errors.push(`Topic #${input.inputTopicId}의 상세 정보를 불러와 주세요.`);
    else if (!topic.schemaJson?.trim()) errors.push(`${topic.topicName}의 Schema를 Topic 관리에서 먼저 등록해 주세요.`);
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

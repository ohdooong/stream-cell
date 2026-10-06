import { api, unwrap } from './client';
import type { Topic } from './platform';

export function parseInputTopics(value: unknown): Topic[] {
  if (!Array.isArray(value) || value.some((item) => !item || typeof item !== 'object'
    || !Number.isSafeInteger(item.topicId) || item.topicId <= 0 || typeof item.topicName !== 'string' || !item.topicName.trim())) {
    throw new Error('입력 Topic 목록 응답 형식이 올바르지 않습니다.');
  }
  return value as Topic[];
}

export function assertInputTopic(topics: Topic[], topicId: number | null) {
  if (!Number.isSafeInteger(topicId) || !topics.some((topic) => topic.topicId === topicId)) {
    throw new Error('DEPLOY 권한이 있는 입력 Topic을 선택해 주세요.');
  }
}

export async function getInputTopics(signal?: AbortSignal) {
  return parseInputTopics(unwrap(await api<unknown>('/api/v1/platform/topic/input-topics', { signal })));
}

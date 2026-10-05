import { api, unwrap } from './client';

export type PipelineFailure = {
  errorExceptionName: string | null;
  errorMessage: string | null;
  errorTimestamp: number | string | null;
};

function record(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

// Supports a Failure DTO and the existing PipelineStatus DTO's nested failure.
export function parseFailures(value: unknown, pipelineId: number): PipelineFailure[] {
  if (record(value) && value.pipelineId !== undefined && value.pipelineId !== pipelineId) throw new Error('요청한 Pipeline과 실패 응답의 ID가 다릅니다.');
  const payload = record(value) && 'failure' in value ? value.failure : value;
  if (payload === null) return [];
  return (Array.isArray(payload) ? payload : [payload]).map((item) => {
    if (!record(item) || !['errorExceptionName', 'errorMessage', 'errorTimestamp'].some((key) => key in item)) throw new Error('실패 응답의 Exception 정보를 확인할 수 없습니다.');
    if ((item.errorExceptionName != null && typeof item.errorExceptionName !== 'string') ||
      (item.errorMessage != null && typeof item.errorMessage !== 'string') ||
      (item.errorTimestamp != null && typeof item.errorTimestamp !== 'string' && (typeof item.errorTimestamp !== 'number' || !Number.isFinite(item.errorTimestamp)))) throw new Error('실패 응답의 Exception 필드 형식이 올바르지 않습니다.');
    return {
      errorExceptionName: typeof item.errorExceptionName === 'string' ? item.errorExceptionName : null,
      errorMessage: typeof item.errorMessage === 'string' ? item.errorMessage : null,
      errorTimestamp: typeof item.errorTimestamp === 'number' || typeof item.errorTimestamp === 'string' ? item.errorTimestamp : null,
    };
  }).filter((item) => Boolean(item.errorExceptionName?.trim() || item.errorMessage?.trim() || typeof item.errorTimestamp === 'number' || item.errorTimestamp?.trim()));
}

export function failuresPath(pipelineId: number) {
  if (!Number.isSafeInteger(pipelineId) || pipelineId <= 0) throw new Error('실패 조회 Pipeline ID가 올바르지 않습니다.');
  return `/api/v1/platform/pipeline/pipelines/${pipelineId}/failures`;
}

export async function getPipelineFailures(pipelineId: number, signal?: AbortSignal) {
  return parseFailures(unwrap(await api<unknown>(failuresPath(pipelineId), { signal })), pipelineId);
}

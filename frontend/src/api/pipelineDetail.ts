import type { PipelineType } from './platform';

export function pipelineDetailPath(pipelineId: number, type: PipelineType): string {
  const segment = type === 'AI_SQL' ? 'ai-sql' : 'custom-jar';
  return `/api/v1/platform/pipeline/pipelines/${segment}/${pipelineId}`;
}

import { api, unwrap } from './client';
import type { Pipeline } from './platform';

export type AiSqlInput = {
  ownerUserId: number;
  pipelineName: string;
  description: string;
  pipelineType: 'AI_SQL';
  inputTopicId: number;
  naturalLanguageRequest: string;
};

export type PipelinePlan = {
  sourceTopicId: number;
  window: { type: string; size: number; unit: string };
  groupBy: string[];
  aggregations: Array<{ function: string; field: string; alias: string }>;
  filters: Array<{ field: string; operator: string; value: unknown }>;
};

export type AiSqlPreviewRequest = {
  inputTopicId: number;
  userId: number;
  naturalLanguageRequest: string;
};

export type AiSqlConfigInput = AiSqlPreviewRequest & {
  pipelinePlan: PipelinePlan;
};

export type AiSqlPreview = {
  pipelinePlan: PipelinePlan;
  generatedFlinkSql: string;
};

type Envelope<T> = { body: T };
const PIPELINES = '/api/v1/platform/pipeline/pipelines';

export const aiSqlApi = {
  async preview(input: AiSqlPreviewRequest) {
    return unwrap(await api<Envelope<AiSqlPreview>>(`${PIPELINES}/ai-sql/preview`, {
      method: 'POST', body: JSON.stringify(input),
    }));
  },
  async createConfig(pipelineId: number, input: AiSqlConfigInput) {
    return unwrap(await api<Envelope<Pipeline>>(`${PIPELINES}/${pipelineId}/ai-sql`, {
      method: 'POST', body: JSON.stringify(input),
    }));
  },
};

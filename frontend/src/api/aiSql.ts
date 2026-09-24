import { api, unwrap } from './client';
import type { Pipeline } from './platform';

// Proposed backend contract: docs/ai-sql-backend.md.
export type AiSqlInput = {
  ownerUserId: number;
  pipelineName: string;
  description: string;
  pipelineType: 'AI_SQL';
  inputTopicId: number;
  naturalLanguageRequest: string;
};

export type AiSqlPreview = {
  previewId: string;
  expiresAt: string;
  pipelinePlan: { summary: string; steps: Array<{ title: string; description: string }> };
  generatedSql: string;
  warnings: string[];
  validation: { valid: boolean; errors: string[] };
};

type Envelope<T> = { body: T };
const PATH = '/api/v1/platform/pipeline/pipelines/ai-sql';

export const aiSqlApi = {
  async preview(input: AiSqlInput) {
    return unwrap(await api<Envelope<AiSqlPreview>>(`${PATH}/preview`, {
      method: 'POST', body: JSON.stringify(input),
    }));
  },
  async create(input: AiSqlInput, previewId: string) {
    return unwrap(await api<Envelope<Pipeline>>(PATH, {
      method: 'POST', body: JSON.stringify({ ...input, previewId }),
    }));
  },
};

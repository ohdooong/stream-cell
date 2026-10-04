import type { PipelineDetail } from '../api/platform';

type Data = Record<string, unknown>;

function asData(value: unknown): Data {
  return value !== null && typeof value === 'object' && !Array.isArray(value) ? value as Data : {};
}

function first(...values: unknown[]): unknown {
  return values.find((value) => value !== null && value !== undefined
    && (typeof value !== 'string' || Boolean(value.trim())));
}

function parseJson(value: unknown): unknown {
  if (typeof value !== 'string') return value;
  try { return JSON.parse(value); } catch { return value; }
}

function ids(value: unknown): number[] {
  const parsed = parseJson(value);
  if (!Array.isArray(parsed)) return [];
  return parsed.map(Number).filter((id) => Number.isSafeInteger(id) && id > 0);
}

export function customJarDetail(item: PipelineDetail) {
  const config = asData(first(item.customJobConfig, item.customJarConfig, item.jobConfig));
  const artifact = asData(first(item.pipelineArtifact, item.artifact));
  return {
    artifactId: first(artifact.artifactId, item.artifactId),
    entryClass: first(config.entryClass, item.entryClass),
    parallelism: first(config.parallelism, item.parallelism),
    inputTopicIds: ids(first(config.inputTopicIds, item.inputTopicIds)),
    outputTopicIds: ids(first(config.outputTopicIds, item.outputTopicIds)),
    programArgs: parseJson(first(config.programArgs, item.programArgs)),
    originalFileName: first(artifact.originalFileName, item.originalFileName),
    storedFileName: first(artifact.storedFileName, item.storedFileName),
    flinkJarId: first(artifact.flinkJarId, item.flinkJarId),
  };
}

export function aiSqlDetail(item: PipelineDetail) {
  const config = asData(first(item.aiSqlConfig, item.aiConfig));
  const pipelinePlan = parseJson(first(item.pipelinePlanJson, item.pipelinePlan));
  const plan = asData(pipelinePlan);
  const topicId = Number(first(config.inputTopicId, item.inputTopicId, plan.sourceTopicId));
  return {
    inputTopicId: Number.isSafeInteger(topicId) && topicId > 0 ? topicId : null,
    naturalLanguageRequest: first(item.naturalLanguageRequest, config.naturalLanguageRequest),
    pipelinePlan,
    generatedSql: first(item.generatedSql, item.generatedFlinkSql, config.generatedSql),
  };
}

export function programArgsText(value: unknown): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) return value.map(String).join('\n');
  if (typeof value === 'object') return Object.entries(value).map(([key, entry]) => `${key}=${String(entry)}`).join('\n');
  return String(value);
}

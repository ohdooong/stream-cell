import { api, unwrap } from './client';

export type ResultValue = string | number | boolean | null;
export type ResultRow = Record<string, ResultValue>;
export type ResultColumn = { key: string; label: string; type: string };
export type ResultSeries = { key: string; label: string; groupKeys: string[]; unit?: string };
export type PipelineResults = {
  pipelineId?: number; pipelineStatus?: string; updatedAt?: string;
  columns: ResultColumn[]; rows: ResultRow[];
  chart: { xKey: string; series: ResultSeries[] } | null;
};

function record(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

export function parseResults(value: unknown): PipelineResults {
  if (!record(value) || !Array.isArray(value.columns) || !Array.isArray(value.rows)) {
    throw new Error('결과 응답에 columns와 rows 배열이 필요합니다.');
  }
  const columns = value.columns.map((column): ResultColumn => {
    if (!record(column) || typeof column.key !== 'string' || !column.key) throw new Error('결과 컬럼의 key가 올바르지 않습니다.');
    return { key: column.key, label: typeof column.label === 'string' ? column.label : column.key, type: typeof column.type === 'string' ? column.type.toUpperCase() : 'STRING' };
  });
  const rows = value.rows.map((row): ResultRow => {
    if (!record(row) || Object.values(row).some((cell) => cell !== null && !['string', 'number', 'boolean'].includes(typeof cell))) {
      throw new Error('결과 rows는 컬럼 키와 기본 값으로 구성된 객체 배열이어야 합니다.');
    }
    return row as ResultRow;
  });
  let chart: PipelineResults['chart'] = null;
  if (value.chart !== null && value.chart !== undefined) {
    if (!record(value.chart) || typeof value.chart.xKey !== 'string' || !Array.isArray(value.chart.series)) throw new Error('결과 chart의 xKey 또는 series가 올바르지 않습니다.');
    chart = { xKey: value.chart.xKey, series: value.chart.series.map((series): ResultSeries => {
      if (!record(series) || typeof series.key !== 'string' || !series.key) throw new Error('차트 지표 key가 올바르지 않습니다.');
      const groupKeys = series.groupKeys ?? (typeof series.groupKey === 'string' ? [series.groupKey] : []);
      if (!Array.isArray(groupKeys) || groupKeys.some((key) => typeof key !== 'string')) throw new Error('차트 groupKeys는 문자열 배열이어야 합니다.');
      return { key: series.key, label: typeof series.label === 'string' ? series.label : series.key, groupKeys, ...(typeof series.unit === 'string' ? { unit: series.unit } : {}) };
    }) };
  }
  return {
    columns, rows, chart,
    ...(typeof value.pipelineId === 'number' ? { pipelineId: value.pipelineId } : {}),
    ...(typeof value.pipelineStatus === 'string' ? { pipelineStatus: value.pipelineStatus } : {}),
    ...(typeof value.updatedAt === 'string' ? { updatedAt: value.updatedAt } : {}),
  };
}

export function resultsPath(pipelineId: number, limit: number, rangeMinutes: number) {
  if (!Number.isSafeInteger(pipelineId) || pipelineId <= 0 || !Number.isSafeInteger(limit) || limit <= 0 || !Number.isSafeInteger(rangeMinutes) || rangeMinutes <= 0) throw new Error('결과 조회 조건이 올바르지 않습니다.');
  return `/api/v1/platform/pipeline/pipelines/${pipelineId}/results?${new URLSearchParams({ limit: String(limit), rangeMinutes: String(rangeMinutes) })}`;
}

export async function getPipelineResults(pipelineId: number, limit = 100, rangeMinutes = 20, signal?: AbortSignal) {
  const response = await api<unknown>(resultsPath(pipelineId, limit, rangeMinutes), { signal });
  const results = parseResults(unwrap(response));
  if (results.pipelineId !== undefined && results.pipelineId !== pipelineId) throw new Error('요청한 Pipeline과 결과 응답의 ID가 다릅니다.');
  return results;
}

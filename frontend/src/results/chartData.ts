import type { ResultRow, ResultSeries, ResultValue } from '../api/results';

export function groupId(row: ResultRow, keys: string[]) {
  // Tuple encoding prevents collisions when values contain a label separator.
  return JSON.stringify(keys.map((key) => row[key] ?? null));
}

export function displayValue(value: ResultValue | undefined) {
  if (value === null || value === undefined) return '—';
  return typeof value === 'number' ? value.toLocaleString('ko-KR', { maximumFractionDigits: 4 }) : String(value);
}

export function numericValue(value: ResultValue | undefined): number | null {
  if (value === null || value === undefined || typeof value === 'boolean' || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

export function buildChartData(rows: ResultRow[], xKey: string, series: ResultSeries) {
  const groups = new Map<string, { id: string; label: string; points: Map<string, number | null> }>();
  const domain = new Map<string, ResultValue>();
  for (const row of rows) {
    const xValue = row[xKey];
    if (xValue === null || xValue === undefined) continue;
    const xId = JSON.stringify(xValue);
    domain.set(xId, xValue);
    const id = groupId(row, series.groupKeys);
    if (!groups.has(id)) groups.set(id, { id, label: series.groupKeys.length ? series.groupKeys.map((key) => `${key}: ${displayValue(row[key])}`).join(' / ') : '전체 집계', points: new Map() });
    groups.get(id)!.points.set(xId, numericValue(row[series.key]));
  }
  const xs = [...domain].map(([id, value]) => ({ id, value }));
  if (xs.every(({ value }) => typeof value === 'number')) xs.sort((a, b) => Number(a.value) - Number(b.value));
  else if (xs.every(({ value }) => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}[T ]/.test(value) && Number.isFinite(Date.parse(value)))) xs.sort((a, b) => Date.parse(String(a.value)) - Date.parse(String(b.value)));
  return { xs, groups: [...groups.values()] };
}

export function chartLabel(value: ResultValue) {
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}[T ]/.test(value) && Number.isFinite(Date.parse(value))) {
    return new Date(value).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false });
  }
  return displayValue(value);
}

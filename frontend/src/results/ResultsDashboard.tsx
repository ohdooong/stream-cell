import { useEffect, useMemo, useRef, useState } from 'react';
import { getPipelineResults, resultsPath, type PipelineResults, type ResultColumn, type ResultValue } from '../api/results';
import type { Pipeline } from '../api/platform';
import { buildChartData, chartLabel, displayValue, groupId } from './chartData';
import { ResultsChart } from './ResultsChart';
import '../demo/resultsDemo.css';
import './results.css';

function tableValue(value: ResultValue | undefined, column: ResultColumn) {
  if (value === null || value === undefined) return '—';
  if (['TIMESTAMP', 'DATETIME', 'DATE'].includes(column.type) && Number.isFinite(Date.parse(String(value)))) {
    return new Date(String(value)).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', hour12: false });
  }
  return displayValue(value);
}

export function ResultsDashboard({ pipelines, initialPipelineId, openPipeline }: { pipelines: Pipeline[]; initialPipelineId: number | null; openPipeline: (id: number, type: Pipeline['pipelineType']) => void }) {
  const [selectedId, setSelectedId] = useState(initialPipelineId ?? pipelines[0]?.pipelineId ?? 0);
  const pipeline = pipelines.find((item) => item.pipelineId === selectedId) ?? pipelines[0];
  const pipelineId = pipeline?.pipelineId ?? 0;
  const [limit, setLimit] = useState(100);
  const [rangeMinutes, setRangeMinutes] = useState(20);
  const [paused, setPaused] = useState(false);
  const pausedRef = useRef(paused);
  const [reload, setReload] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState<{ query: string; data: PipelineResults; receivedAt: string } | null>(null);
  const [metricKey, setMetricKey] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('ALL');
  const [page, setPage] = useState(0);
  const query = pipelineId ? resultsPath(pipelineId, limit, rangeMinutes) : '';
  const data = saved?.query === query ? saved.data : null;

  useEffect(() => { pausedRef.current = paused; }, [paused]);

  useEffect(() => {
    if (!pipelineId) return;
    const controller = new AbortController();
    let timer: number | undefined;
    let active = true;
    setError('');
    async function load() {
      if (!active) return;
      setBusy(true);
      try {
        const next = await getPipelineResults(pipelineId, limit, rangeMinutes, controller.signal);
        if (active) { setSaved({ query, data: next, receivedAt: new Date().toISOString() }); setError(''); }
      } catch (cause) {
        if (active) setError(cause instanceof Error ? cause.message : '결과 조회에 실패했습니다.');
      } finally {
        if (active) {
          setBusy(false);
          timer = window.setTimeout(poll, 5000);
        }
      }
    }
    function poll() {
      if (!active) return;
      if (document.hidden || pausedRef.current) timer = window.setTimeout(poll, 5000);
      else void load();
    }
    void load();
    return () => { active = false; controller.abort(); window.clearTimeout(timer); };
  }, [pipelineId, limit, rangeMinutes, query, reload]);

  useEffect(() => { setPage(0); setMetricKey(''); setSelectedGroup('ALL'); }, [pipelineId, limit, rangeMinutes]);
  const series = data?.chart?.series.find((item) => item.key === metricKey) ?? data?.chart?.series[0];
  const model = useMemo(() => data?.chart && series ? buildChartData(data.rows, data.chart.xKey, series) : null, [data, series]);
  const group = selectedGroup === 'ALL' || model?.groups.some((item) => item.id === selectedGroup) ? selectedGroup : 'ALL';
  const rows = useMemo(() => {
    if (!data) return [];
    const filtered = series && group !== 'ALL' ? data.rows.filter((row) => groupId(row, series.groupKeys) === group) : data.rows;
    if (!model || !data.chart) return filtered;
    const order = new Map(model.xs.map((point, index) => [point.id, index]));
    const xKey = data.chart.xKey;
    return [...filtered].sort((a, b) => (order.get(JSON.stringify(b[xKey])) ?? -1) - (order.get(JSON.stringify(a[xKey])) ?? -1));
  }, [data, series, group, model]);
  const pages = Math.max(1, Math.ceil(rows.length / 12));
  const currentPage = Math.min(page, pages - 1);
  const receivedAt = data && saved ? chartLabel(saved.receivedAt) : '—';

  if (!pipeline) return <section className="panel connected-unavailable"><span>RESULTS</span><h2>조회할 Pipeline이 없습니다</h2><p>Pipeline을 등록하면 처리 결과를 확인할 수 있습니다.</p></section>;
  return <div className="rd-shell results-dashboard">
    <div className="welcome-row"><div><h2>데이터의 흐름을, 결과로.</h2><p>실제 Pipeline 처리 결과를 Table과 Chart로 확인하세요.</p></div><span className="live-pill"><i /> LIVE API</span></div>
    <section className="rd-pipeline-card">
      <div className="rd-pipeline-icon">⌘</div>
      <div className="rd-pipeline-choice"><label htmlFor="results-pipeline">조회할 Pipeline</label><select id="results-pipeline" value={pipelineId} onChange={(event) => setSelectedId(Number(event.target.value))}>{pipelines.map((item) => <option key={item.pipelineId} value={item.pipelineId}>{item.pipelineName} · #{item.pipelineId}</option>)}</select><p>{pipeline.description || 'Pipeline의 집계 결과 조회'}</p></div>
      <div className="results-pipeline-status"><span className="format-chip">{pipeline.pipelineType}</span><span className={`status ${ (data?.pipelineStatus ?? pipeline.pipelineStatus) === 'RUNNING' ? 'running' : (data?.pipelineStatus ?? pipeline.pipelineStatus) === 'FAILED' ? 'failed' : ''}`}><i />{data?.pipelineStatus ?? pipeline.pipelineStatus}</span><button className="text-button" onClick={() => openPipeline(pipelineId, pipeline.pipelineType)}>상세 보기 →</button></div>
    </section>
    <section className="results-query" aria-label="결과 조회 조건">
      <label htmlFor="results-range">조회 기간<select id="results-range" value={rangeMinutes} onChange={(event) => setRangeMinutes(Number(event.target.value))}>{[5, 20, 60].map((value) => <option key={value} value={value}>최근 {value}분</option>)}</select></label>
      <label htmlFor="results-limit">최대 결과 행<select id="results-limit" value={limit} onChange={(event) => setLimit(Number(event.target.value))}>{[100, 300, 500, 1000].map((value) => <option key={value} value={value}>{value.toLocaleString()}행</option>)}</select></label>
      <div className="rd-table-actions"><span className={paused ? 'rd-refresh paused' : 'rd-refresh'}><i />{paused ? '자동 조회 일시정지' : '5초마다 자동 조회'}</span><button onClick={() => setPaused((value) => !value)} aria-pressed={paused}>{paused ? '▶ 재개' : 'Ⅱ 일시정지'}</button><button disabled={busy} onClick={() => setReload((value) => value + 1)}>{busy ? '조회 중…' : '↻ 새로고침'}</button></div>
    </section>
    {error && <div className="results-error" role="alert"><div><strong>결과 조회에 실패했습니다.</strong><p>{error}</p>{data && <small>아래는 마지막으로 성공한 조회 결과입니다. 최신 데이터가 아닐 수 있습니다.</small>}</div><button className="secondary-button" disabled={busy} onClick={() => setReload((value) => value + 1)}>다시 조회</button></div>}
    {!data && !error && <div className="rd-empty" role="status"><h3>처리 결과 조회 중</h3><p>결과 API 응답을 기다리고 있습니다.</p></div>}
    {data && <>
      <div className="rd-metrics results-metrics">
        <article><small>조회된 처리 결과</small><strong>{data.rows.length}<em>행</em></strong><p><span>최근 {rangeMinutes}분 · 최대 {limit}행</span></p></article>
        <article><small>선택한 지표의 분석 그룹</small><strong>{model?.groups.length ?? '—'}<em>개</em></strong><p><span>{series?.groupKeys.join(' · ') || (series ? '전체 집계' : '차트 설정 없음')}</span></p></article>
        <article><small>마지막 조회 성공</small><strong className="results-time">{receivedAt}</strong><p><span>Asia/Seoul{data.updatedAt ? ` · 데이터 갱신 ${chartLabel(data.updatedAt)}` : ''}</span></p></article>
      </div>
      {data.rows.length >= limit && <p className="results-hint">최대 {limit}행에 도달했습니다. 조회 기간 내 일부 결과가 생략되었을 수 있습니다. 최대 결과 행을 늘리거나 기간을 줄여 주세요.</p>}
      <section className="rd-panel"><div className="rd-panel-heading"><div><h2>집계 결과 추이</h2><p>{data.chart ? `X축: ${data.chart.xKey} · 응답의 그룹 조합별 집계` : 'API의 chart 설정으로 차트를 구성합니다.'}</p></div>{series && <div className="rd-chart-controls"><label htmlFor="results-metric">지표<select id="results-metric" value={series.key} onChange={(event) => { setMetricKey(event.target.value); setSelectedGroup('ALL'); setPage(0); }}>{data.chart!.series.map((item) => <option key={item.key} value={item.key}>{item.label}</option>)}</select></label><label htmlFor="results-group">그룹<select id="results-group" value={group} onChange={(event) => { setSelectedGroup(event.target.value); setPage(0); }}><option value="ALL">모든 그룹</option>{model?.groups.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}</select></label></div>}</div>
        {series && data.chart ? <ResultsChart key={`${pipelineId}-${series.key}-${group}`} rows={data.rows} xKey={data.chart.xKey} series={series} group={group} /> : <div className="rd-empty"><h3>차트 설정이 없습니다</h3><p>처리 결과는 아래 Table에서 확인할 수 있습니다.</p></div>}
      </section>
      <section className="rd-panel rd-table-panel"><div className="rd-panel-heading"><div><h2>처리 결과 <span>{rows.length}</span></h2><p>컬럼은 API의 columns를 기준으로 표시합니다. 그룹 필터는 Table에도 적용됩니다.</p></div></div>
        {rows.length ? <div className="rd-table-scroll"><table><thead><tr>{data.columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}</tr></thead><tbody>{rows.slice(currentPage * 12, (currentPage + 1) * 12).map((row, index) => <tr key={currentPage * 12 + index}>{data.columns.map((column) => <td key={column.key}>{tableValue(row[column.key], column)}</td>)}</tr>)}</tbody></table></div> : <div className="rd-table-empty">조회 조건에 해당하는 처리 결과가 없습니다. 데이터가 도착하면 자동 조회 시 표시됩니다.</div>}
        <div className="rd-table-footer"><span>{rows.length ? `${currentPage * 12 + 1}–${Math.min((currentPage + 1) * 12, rows.length)} / ${rows.length}행` : '0개 결과'}</span><div className="results-pagination"><button disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>이전</button><span>{currentPage + 1} / {pages}</span><button disabled={currentPage === pages - 1} onClick={() => setPage(currentPage + 1)}>다음</button></div></div>
      </section>
      <details className="rd-response"><summary>조회 응답 보기 <span>columns · rows · chart</span></summary><p>GET {query}</p><pre>{JSON.stringify(data, null, 2)}</pre></details>
    </>}
  </div>;
}

import { useEffect, useMemo, useState } from 'react';
import { createDemoSnapshot, demoPipelines, type ResultRow } from './resultsDemoData';
import './resultsDemo.css';

const colors = ['#149e91', '#5a79dc', '#e69a43'];
const timeLabel = (value: string, seconds = false) => new Date(value).toLocaleTimeString('ko-KR', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', ...(seconds ? { second: '2-digit' } : {}), hour12: false });
const numberLabel = (value: number) => value.toLocaleString('ko-KR');

function ResultsChart({ rows, metricKey, groupKey, groups, unit }: { rows: ResultRow[]; metricKey: string; groupKey: string; groups: string[]; unit: string }) {
  const [hover, setHover] = useState<number | null>(null);
  const windows = [...new Set(rows.map((row) => String(row.windowStart)))].reverse();
  const series = groups.map((group) => windows.map((window) => rows.find((row) => row.windowStart === window && row[groupKey] === group)));
  const maximum = Math.max(1, ...series.flat().map((row) => row ? Number(row[metricKey]) : 0));
  const ceiling = Math.ceil(maximum / (maximum > 1000 ? 5000 : 100)) * (maximum > 1000 ? 5000 : 100);
  const x = (index: number) => 60 + index / Math.max(1, windows.length - 1) * 850;
  const y = (value: number) => 248 - value / ceiling * 208;
  if (!rows.length) return <div className="rd-empty"><span>⌁</span><h3>아직 처리 결과가 없습니다</h3><p>데이터가 도착하면 차트와 테이블이 함께 업데이트됩니다.</p></div>;
  return <div className="rd-chart-wrap">
    <svg viewBox="0 0 940 300" role="img" aria-label={`최근 20개 윈도우의 그룹별 ${metricKey} 추이`} onMouseLeave={() => setHover(null)}>
      {[0, 1, 2, 3, 4].map((index) => <g key={index}><line x1="60" x2="910" y1={y(ceiling * index / 4)} y2={y(ceiling * index / 4)} stroke="#e9eef4" strokeDasharray="4 5" /><text x="47" y={y(ceiling * index / 4) + 4} textAnchor="end">{numberLabel(ceiling * index / 4)}</text></g>)}
      {series.map((points, groupIndex) => <polyline key={groups[groupIndex]} points={points.map((row, index) => `${x(index)},${y(Number(row?.[metricKey] || 0))}`).join(' ')} fill="none" stroke={colors[groupIndex]} strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />)}
      {[0, 4, 9, 14, 19].map((index) => windows[index] && <text key={index} x={x(index)} y="280" textAnchor="middle">{timeLabel(windows[index])}</text>)}
      {hover !== null && <line x1={x(hover)} x2={x(hover)} y1="34" y2="248" stroke="#a3b4c8" strokeDasharray="4 4" />}
      {windows.map((window, index) => <rect key={window} x={x(index) - 21} y="25" width="42" height="230" fill="transparent" onMouseEnter={() => setHover(index)} />)}
      {hover !== null && series.map((points, groupIndex) => <circle key={groups[groupIndex]} cx={x(hover)} cy={y(Number(points[hover]?.[metricKey] || 0))} r="5" fill={colors[groupIndex]} stroke="white" strokeWidth="2" />)}
    </svg>
    {hover !== null && <div className="rd-chart-tooltip"><strong>{timeLabel(windows[hover])} 윈도우</strong>{series.map((points, index) => <span key={groups[index]}><i style={{ background: colors[index] }} />{groups[index]}<b>{numberLabel(Number(points[hover]?.[metricKey] || 0))} {unit}</b></span>)}</div>}
  </div>;
}

export function ResultsDashboardDemo() {
  const [pipelineId, setPipelineId] = useState(demoPipelines[0].id);
  const pipeline = demoPipelines.find((item) => item.id === pipelineId)!;
  const [anchorTime] = useState(() => Math.floor(Date.now() / 60_000) * 60_000);
  const [tick, setTick] = useState(0);
  const [updatedAt, setUpdatedAt] = useState(() => new Date().toISOString());
  const [paused, setPaused] = useState(false);
  const [empty, setEmpty] = useState(false);
  const [metricKey, setMetricKey] = useState(pipeline.metrics[0].key);
  const [group, setGroup] = useState('ALL');
  const metric = pipeline.metrics.find((item) => item.key === metricKey) || pipeline.metrics[0];
  const snapshot = useMemo(() => createDemoSnapshot(pipeline, anchorTime, tick, updatedAt, empty), [pipeline, anchorTime, tick, updatedAt, empty]);

  function refresh() { setTick((value) => value + 1); setUpdatedAt(new Date().toISOString()); }
  useEffect(() => {
    if (paused || empty) return;
    const timer = window.setInterval(() => { if (!document.hidden) refresh(); }, 5000);
    return () => window.clearInterval(timer);
  }, [paused, empty]);
  function selectPipeline(id: number) {
    const next = demoPipelines.find((item) => item.id === id)!;
    setPipelineId(id); setMetricKey(next.metrics[0].key); setGroup('ALL'); setTick(0); setEmpty(false); setUpdatedAt(new Date().toISOString());
  }
  const filteredRows = snapshot.rows.filter((row) => group === 'ALL' || row[pipeline.groupKey] === group);
  const selectedGroups = group === 'ALL' ? pipeline.groups : [group];
  const latest = filteredRows.filter((row) => row.windowStart === filteredRows[0]?.windowStart);
  const latestCount = latest.reduce((sum, row) => sum + Number(row[pipeline.metrics[0].key]), 0);
  const latestAverage = latest.length ? latest.reduce((sum, row) => sum + Number(row[pipeline.metrics[1].key]), 0) / latest.length : 0;
  const previous = filteredRows.filter((row) => row.windowStart === filteredRows[selectedGroups.length]?.windowStart);
  const previousCount = previous.reduce((sum, row) => sum + Number(row[pipeline.metrics[0].key]), 0);
  const change = previousCount ? (latestCount - previousCount) / previousCount * 100 : 0;

  return <div className="rd-shell">
    <aside className="rd-sidebar"><a className="brand inverse" href="/"><span className="brand-symbol"><i /><i /><i /></span><strong>StreamCell</strong></a><span className="rd-environment">INTERACTIVE DEMO</span><p className="rd-nav-label">MANAGEMENT</p><a href="/">◉ <span>관리 콘솔</span></a><div className="rd-nav-active">▥ <span>결과 Dashboard</span><i /></div><div className="rd-side-note"><span>DATA FLOW</span><strong>Kafka → Pipeline → Result</strong><p>흐르는 데이터가<br />의미 있는 결과가 되는 곳.</p><div className="rd-flow-dots"><i /><b /><i /><b /><i /></div></div><div className="rd-demo-account"><span>D</span><div><strong>Demo workspace</strong><small>샘플 데이터 시뮬레이션</small></div></div></aside>
    <main className="rd-main"><header className="rd-topbar"><span>Management <b>/</b> 결과 Dashboard</span><a href="/">로그인 화면으로 ↗</a></header>
      <div className="rd-content"><div className="rd-heading"><div><p className="rd-eyebrow">RESULTS / OBSERVABILITY</p><h1>데이터의 흐름을, 결과로.</h1><p>Pipeline의 처리 결과와 집계 추이를 한 화면에서 확인하세요.</p></div><span className="rd-demo-badge">DEMO · 샘플 데이터</span></div>
        <div className="rd-demo-notice"><span>✦</span><p>백엔드 없이 동작하는 예시입니다. 5초마다 데모 시간의 1분 윈도우가 추가됩니다.</p><button onClick={() => setEmpty((value) => !value)}>{empty ? '샘플 데이터 복원' : '빈 결과 보기'}</button></div>
        <section className="rd-pipeline-card"><div className="rd-pipeline-icon">⌘</div><div className="rd-pipeline-choice"><label htmlFor="rd-pipeline">조회할 Pipeline</label><select id="rd-pipeline" value={pipelineId} onChange={(event) => selectPipeline(Number(event.target.value))}>{demoPipelines.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select><p>{pipeline.description}</p></div><div className="rd-pipeline-meta"><span>{pipeline.type}</span><b><i /> RUNNING</b></div><div className="rd-updated"><small>마지막 조회</small><strong>{timeLabel(updatedAt, true)}</strong><span>Asia/Seoul</span></div></section>
        <div className="rd-metrics"><article><span className="rd-metric-icon">↗</span><small>최근 윈도우 · {pipeline.metrics[0].label}</small><strong>{numberLabel(latestCount)}<em>{pipeline.metrics[0].unit}</em></strong><p className={change >= 0 ? 'positive' : 'negative'}>{change >= 0 ? '↑' : '↓'} {Math.abs(change).toFixed(1)}% <span>이전 윈도우 대비</span></p></article><article><span className="rd-metric-icon blue">≋</span><small>최근 윈도우 · {pipeline.metrics[1].label}</small><strong>{numberLabel(Math.round(latestAverage))}<em>{pipeline.metrics[1].unit}</em></strong><p><span>그룹별 평균의 단순 평균</span></p></article><article><span className="rd-metric-icon amber">▦</span><small>조회된 결과</small><strong>{filteredRows.length}<em>행</em></strong><p><span>{empty ? '데이터 대기 중' : '최근 20개 윈도우 · 1분 단위'}</span></p></article><article><span className="rd-metric-icon purple">⌁</span><small>분석 그룹</small><strong>{empty ? 0 : selectedGroups.length}<em>개</em></strong><p><span>{selectedGroups.join(' · ')}</span></p></article></div>
        <section className="rd-panel"><div className="rd-panel-heading"><div><h2>집계 결과 추이</h2><p>최근 20개 윈도우의 그룹별 처리 결과</p></div><div className="rd-chart-controls"><label>지표<select value={metric.key} onChange={(event) => setMetricKey(event.target.value)}>{pipeline.metrics.map((item) => <option key={item.key} value={item.key}>{item.label}</option>)}</select></label><label>그룹<select value={group} onChange={(event) => setGroup(event.target.value)}><option value="ALL">모든 그룹</option>{pipeline.groups.map((item) => <option key={item}>{item}</option>)}</select></label></div></div><div className="rd-chart-legend">{selectedGroups.map((item) => <span key={item}><i style={{ background: colors[selectedGroups.indexOf(item)] }} />{item}</span>)}<small>단위: {metric.unit}</small></div><ResultsChart key={`${pipelineId}-${metric.key}-${group}-${empty}`} rows={filteredRows} metricKey={metric.key} groupKey={pipeline.groupKey} groups={selectedGroups} unit={metric.unit} /></section>
        <section className="rd-panel rd-table-panel"><div className="rd-panel-heading"><div><h2>실시간 집계 결과 <span>{filteredRows.length}</span></h2><p>동일한 응답 데이터로 Table과 Chart를 표시합니다.</p></div><div className="rd-table-actions"><span className={paused || empty ? 'rd-refresh paused' : 'rd-refresh'}><i />{empty ? '데이터 대기' : paused ? '자동 갱신 일시정지' : '5초마다 갱신'}</span><button onClick={() => setPaused((value) => !value)} aria-pressed={paused}>{paused ? '▶ 재개' : 'Ⅱ 일시정지'}</button><button onClick={refresh} disabled={empty}>↻ 새로고침</button></div></div>{empty ? <div className="rd-table-empty">새로운 처리 결과를 기다리고 있습니다.</div> : <div className="rd-table-scroll"><table><thead><tr>{snapshot.columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead><tbody>{filteredRows.slice(0, 12).map((row) => <tr key={`${row.windowStart}-${row[pipeline.groupKey]}`}>{snapshot.columns.map((column) => <td key={column.key}>{column.type === 'TIMESTAMP' ? <span className="rd-time-cell">{timeLabel(String(row[column.key]))}<small>1분 윈도우</small></span> : column.type === 'NUMBER' ? numberLabel(Number(row[column.key])) : <span className="rd-group-chip">{row[column.key]}</span>}</td>)}</tr>)}</tbody></table></div>}<div className="rd-table-footer">{empty ? '0개 결과' : `${filteredRows.length}개 중 최근 ${Math.min(12, filteredRows.length)}개 표시`}<span>시뮬레이션 데이터 · 실제 처리 결과가 아닙니다</span></div></section>
        <details className="rd-response"><summary>샘플 API 응답 보기 <span>columns · rows · chart</span></summary><p>GET /api/v1/platform/pipeline/pipelines/{pipelineId}/results?limit=100&amp;rangeMinutes=20</p><pre>{JSON.stringify({ body: snapshot }, null, 2)}</pre></details>
        <footer className="rd-footer">StreamCell Management Frontend <span>실시간 결과 Dashboard 예시</span></footer>
      </div>
    </main>
  </div>;
}

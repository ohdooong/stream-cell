import { useState } from 'react';
import type { ResultRow, ResultSeries } from '../api/results';
import { buildChartData, chartLabel, displayValue } from './chartData';

const colors = ['#149e91', '#5a79dc', '#e69a43', '#9974c8', '#dc718b', '#2e98bc', '#7f9d4e', '#ad7450', '#666bbe', '#c49323', '#4b9f7c', '#9f688a'];

export function ResultsChart({ rows, xKey, series, group }: { rows: ResultRow[]; xKey: string; series: ResultSeries; group: string }) {
  const [hover, setHover] = useState<number | null>(null);
  const model = buildChartData(rows, xKey, series);
  const selected = group === 'ALL' ? model.groups : model.groups.filter((item) => item.id === group);
  const visible = selected.slice(0, 12);
  const values = visible.flatMap((item) => [...item.points.values()].filter((value): value is number => value !== null));
  if (!model.xs.length || !values.length) return <div className="rd-empty"><span>⌁</span><h3>표시할 차트 데이터가 없습니다</h3><p>X축과 선택한 지표의 값이 있는 결과가 도착하면 표시됩니다.</p></div>;
  const low = Math.min(0, ...values);
  const high = Math.max(0, ...values);
  const padding = (high - low || 1) * .08;
  const minimum = low < 0 ? low - padding : 0;
  const maximum = high + padding;
  const x = (index: number) => model.xs.length === 1 ? 480 : 85 + index / (model.xs.length - 1) * 815;
  const y = (value: number) => 245 - (value - minimum) / (maximum - minimum) * 205;
  const activeHover = hover !== null && hover < model.xs.length ? hover : null;
  const ticks = [...new Set(Array.from({ length: Math.min(5, model.xs.length) }, (_, index) => Math.round(index * (model.xs.length - 1) / Math.max(1, Math.min(5, model.xs.length) - 1))))];
  return <>
    <div className="rd-chart-legend results-legend">{visible.map((item) => <span key={item.id}><i style={{ background: colors[model.groups.indexOf(item) % colors.length] }} />{item.label}</span>)}{series.unit && <small>단위: {series.unit}</small>}</div>
    {selected.length > 12 && <p className="results-hint">전체 {selected.length}개 그룹 중 12개만 차트에 표시합니다. 그룹을 선택하면 해당 조합을 확인할 수 있습니다.</p>}
    <div className="rd-chart-wrap">
      <svg viewBox="0 0 940 305" role="img" aria-label={`${series.label} 추이 · X축 ${xKey}`} onMouseLeave={() => setHover(null)}>
        {[0, 1, 2, 3, 4].map((index) => { const value = minimum + (maximum - minimum) * index / 4; return <g key={index}><line x1="85" x2="900" y1={y(value)} y2={y(value)} stroke="#e9eef4" strokeDasharray="4 5" /><text x="73" y={y(value) + 4} textAnchor="end">{displayValue(Math.round(value * 100) / 100)}</text></g>; })}
        {visible.map((item) => {
          let penDown = false;
          const path = model.xs.map((point, index) => { const value = item.points.get(point.id); if (value === null || value === undefined) { penDown = false; return ''; } const command = penDown ? 'L' : 'M'; penDown = true; return `${command}${x(index)},${y(value)}`; }).join(' ');
          const color = colors[model.groups.indexOf(item) % colors.length];
          return <g key={item.id}><path d={path} fill="none" stroke={color} strokeWidth="3" strokeLinejoin="round" />{model.xs.map((point, index) => { const value = item.points.get(point.id); return value === null || value === undefined ? null : <circle key={point.id} cx={x(index)} cy={y(value)} r={activeHover === index || model.xs.length === 1 ? 4 : 2} fill={color} />; })}</g>;
        })}
        {ticks.map((index) => <text key={index} x={x(index)} y="281" textAnchor="middle">{chartLabel(model.xs[index].value)}</text>)}
        {activeHover !== null && <line x1={x(activeHover)} x2={x(activeHover)} y1="30" y2="245" stroke="#a3b4c8" strokeDasharray="4 4" />}
        {model.xs.map((point, index) => <rect key={point.id} x={x(index) - Math.min(30, 407 / model.xs.length)} y="25" width={Math.min(60, 815 / model.xs.length)} height="230" fill="transparent" onMouseEnter={() => setHover(index)} />)}
      </svg>
      {activeHover !== null && <div className="rd-chart-tooltip"><strong>{chartLabel(model.xs[activeHover].value)}</strong>{visible.map((item) => <span key={item.id}><i style={{ background: colors[model.groups.indexOf(item) % colors.length] }} />{item.label}<b>{displayValue(item.points.get(model.xs[activeHover].id))} {series.unit}</b></span>)}</div>}
    </div>
  </>;
}

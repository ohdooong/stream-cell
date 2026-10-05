import { useEffect, useState } from 'react';
import { getPipelineFailures, type PipelineFailure } from '../api/failures';
import type { Pipeline } from '../api/platform';
import './failures.css';

export function failureTime(timestamp: PipelineFailure['errorTimestamp']) {
  if (timestamp === null) return '발생 시각 정보 없음';
  // A backend local datetime has no timezone: preserve it without guessing an offset.
  if (typeof timestamp === 'string') return timestamp.trim() || '발생 시각 정보 없음';
  const date = new Date(timestamp);
  return Number.isFinite(date.getTime()) ? date.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', hour12: false }) + ' (Asia/Seoul)' : '발생 시각 정보 없음';
}

export function PipelineFailures({ pipelineId }: { pipelineId: number }) {
  const [reload, setReload] = useState(0);
  const [saved, setSaved] = useState<{ pipelineId: number; reload: number; items: PipelineFailure[] } | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(true);
  const items = saved?.pipelineId === pipelineId && saved.reload === reload ? saved.items : null;
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setBusy(true); setError('');
    void getPipelineFailures(pipelineId, controller.signal).then((next) => {
      if (active) setSaved({ pipelineId, reload, items: next });
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : '실패 정보를 조회하지 못했습니다.');
    }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; controller.abort(); };
  }, [pipelineId, reload]);

  return <section className="panel pipeline-failures" aria-label="Pipeline 원본 Exception" aria-busy={busy}>
    <div className="panel-heading"><div><h3>원본 Exception</h3><p>Pipeline #{pipelineId}의 실패 메시지와 Stack Trace입니다.</p></div><button className="secondary-button" disabled={busy} onClick={() => setReload((value) => value + 1)}>{busy ? '조회 중…' : '다시 조회'}</button></div>
    {error ? <div className="failure-empty" role="alert"><h4>실패 정보를 조회하지 못했습니다</h4><p>{error}</p></div>
      : !items ? <p className="failure-empty" role="status">Exception 정보를 불러오고 있습니다.</p>
      : !items.length ? <div className="failure-empty"><h4>저장된 Exception 정보가 없습니다</h4><p>Pipeline은 FAILED 상태지만 아직 조회할 실패 메시지가 없습니다. 잠시 후 다시 조회해 주세요.</p></div>
      : items.map((item, index) => <article className="failure-entry" key={index}>
        <div className="failure-entry-heading"><h4>{item.errorExceptionName || 'Exception 이름 정보 없음'}</h4><span>{failureTime(item.errorTimestamp)}</span></div>
        {item.errorMessage ? <pre tabIndex={0} aria-label={`Exception 메시지 ${index + 1}`}>{item.errorMessage}</pre> : <p className="failure-empty">Exception 메시지 정보가 없습니다.</p>}
      </article>)}
  </section>;
}

export function FailuresDashboard({ pipelines, openPipeline }: { pipelines: Pipeline[]; openPipeline: (id: number, type: Pipeline['pipelineType']) => void }) {
  const failed = pipelines.filter((item) => item.pipelineStatus === 'FAILED');
  const [selectedId, setSelectedId] = useState(0);
  const pipeline = failed.find((item) => item.pipelineId === selectedId) ?? failed[0];
  return <div className="failures-dashboard">
    <div className="welcome-row"><div><h2>Pipeline 실패 확인</h2><p>실패한 Pipeline의 원본 Exception을 확인하세요.</p></div></div>
    {pipeline ? <>
      <section className="panel failure-selector"><label htmlFor="failure-pipeline">FAILED Pipeline<select id="failure-pipeline" value={pipeline.pipelineId} onChange={(event) => setSelectedId(Number(event.target.value))}>{failed.map((item) => <option key={item.pipelineId} value={item.pipelineId}>{item.pipelineName} · {item.pipelineType} · #{item.pipelineId}</option>)}</select></label><button className="secondary-button" onClick={() => openPipeline(pipeline.pipelineId, pipeline.pipelineType)}>Pipeline 상세 →</button></section>
      <PipelineFailures key={pipeline.pipelineId} pipelineId={pipeline.pipelineId} />
    </> : <section className="panel failure-empty"><h3>실패한 Pipeline이 없습니다</h3><p>현재 사용자의 FAILED 상태 Pipeline이 여기에 표시됩니다. 최신 상태가 필요하면 상단 새로고침을 사용하세요.</p></section>}
    <p className="failure-note">현재는 원본 Exception 조회를 제공합니다. AI 원인 분석 및 권장 조치는 별도 분석 API 연동 후 제공됩니다.</p>
  </div>;
}

import { useRef, useState, type FormEvent } from 'react';
import type { PipelineDetail, Topic } from '../api/platform';
import { CustomJarFields } from './CustomJarFields';
import { customJarDraft, registerCustomJar } from './customJarRegistration';

export function CustomJarRecovery({ item, topics, userId, busy, onBusyChange, onUploaded }: {
  item: PipelineDetail; topics: Topic[]; userId: number; busy: boolean;
  onBusyChange: (busy: boolean) => void; onUploaded: () => Promise<void>;
}) {
  const [draft, setDraft] = useState(() => customJarDraft(item));
  const [error, setError] = useState('');
  const [uploaded, setUploaded] = useState(false);
  const inFlight = useRef(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy || inFlight.current || uploaded) return;
    inFlight.current = true; onBusyChange(true); setError('');
    try {
      await registerCustomJar({ pipelineId: item.pipelineId, userId, draft });
      setUploaded(true);
      await onUploaded();
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'JAR 등록에 실패했습니다. 다시 시도해 주세요.'); }
    finally { inFlight.current = false; onBusyChange(false); }
  }
  return <form className="panel connected-create jar-recovery" onSubmit={submit}>
    <div className="panel-heading"><div><h3>JAR 등록 이어하기</h3><p>Pipeline #{item.pipelineId}의 JAR 등록이 완료되지 않았습니다. 파일과 실행 설정을 등록해 주세요.</p></div></div>
    <div className="connected-form"><CustomJarFields draft={draft} onChange={setDraft} topics={topics} disabled={busy || uploaded} /></div>
    {error && <p className="form-error jar-registration-message" role="alert">{error}</p>}
    {uploaded && <p className="jar-registration-message" role="status">JAR 등록이 완료되었습니다. 최신 상세 정보를 조회하면 배포할 수 있습니다.</p>}
    <div className="connected-actions">{uploaded
      ? <button type="button" className="secondary-button" disabled={busy} onClick={() => void onUploaded().catch((cause) => setError(cause instanceof Error ? cause.message : '상세 조회에 실패했습니다.'))}>등록 상태 다시 조회</button>
      : <button className="primary-button compact" disabled={busy}>{busy ? '등록 중…' : error ? 'JAR 등록 다시 시도' : 'JAR 등록'}</button>}</div>
  </form>;
}

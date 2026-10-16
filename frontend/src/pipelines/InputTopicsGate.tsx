import { useEffect, useState, type ReactNode } from 'react';
import { platformApi, type Topic } from '../api/platform';

// Mount separately for each user/registration. Never fall back to the global Topic list.
export function InputTopicsGate({ children }: { children: (topics: Topic[]) => ReactNode }) {
  const [topics, setTopics] = useState<Topic[] | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(true);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setBusy(true); setError(''); setTopics(null);
    void platformApi.getInputTopics(controller.signal).then((items) => {
      if (active) setTopics(items);
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : '입력 Topic을 조회하지 못했습니다.');
    }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; controller.abort(); };
  }, [reload]);

  if (topics?.length) return <>{children(topics)}</>;
  return <section className="panel connected-unavailable" aria-label="등록용 입력 Topic 조회" aria-busy={busy}>
    <span>TOPIC</span><h2>{busy ? '입력 Topic 조회 중' : error ? '입력 Topic을 불러오지 못했습니다' : 'DEPLOY 권한이 있는 Topic이 없습니다'}</h2>
    <p role={error ? 'alert' : 'status'}>{busy ? '현재 사용자가 배포에 사용할 수 있는 Topic을 확인하고 있습니다.' : error || 'Pipeline 등록에는 Topic의 DEPLOY 권한이 필요합니다. 관리자에게 권한을 요청한 뒤 다시 조회해 주세요.'}</p>
    {!busy && <button className="secondary-button" onClick={() => setReload((value) => value + 1)}>입력 Topic 다시 조회</button>}
  </section>;
}

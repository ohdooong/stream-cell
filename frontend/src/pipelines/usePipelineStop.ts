import { useEffect, useRef, useState } from 'react';
import { platformApi, type PipelineDetail, type PipelineStatus, type PipelineType } from '../api/platform';

export const canStopPipeline = (status: PipelineStatus) => status === 'RUNNING';
export const isStopSettled = (status: PipelineStatus) => ['STOPPED', 'FINISHED', 'FAILED', 'SUSPENDED'].includes(status);
const messageOf = (error: unknown) => error instanceof Error ? error.message : '요청을 처리하지 못했습니다.';

export function usePipelineStop({ id, type, status, onUpdated, refreshList }: {
  id: number; type: PipelineType; status?: PipelineStatus;
  onUpdated: (item: PipelineDetail) => void; refreshList: () => Promise<void>;
}) {
  const [phase, setPhase] = useState<'idle' | 'confirm' | 'submitting' | 'accepted'>('idle');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [checking, setChecking] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const inFlight = useRef(false);
  const mounted = useRef(true);
  const callbacks = useRef({ onUpdated, refreshList });
  callbacks.current = { onUpdated, refreshList };
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  const pending = phase === 'submitting' || phase === 'accepted' || status === 'STOPPING';

  async function stop() {
    if (inFlight.current || phase !== 'confirm' || !status || !canStopPipeline(status)) return;
    inFlight.current = true;
    setPhase('submitting'); setError(''); setNotice('');
    try {
      await platformApi.stopPipeline(id);
      if (!mounted.current) return;
      // 202 means accepted, not stopped. Only the detail API confirms completion.
      setNotice('중지 요청이 접수되었습니다. 실제 처리 상태를 확인하고 있습니다.');
      setPhase('accepted');
    } catch (cause) {
      if (!mounted.current) return;
      setError(`중지 요청에 실패했습니다: ${messageOf(cause)} 현재 상태를 확인한 후 다시 시도해 주세요.`);
      setPhase('idle');
    } finally { inFlight.current = false; }
  }

  const polling = phase === 'accepted' || status === 'STOPPING';
  useEffect(() => {
    if (!polling) return;
    let active = true;
    let timer: ReturnType<typeof setTimeout>;
    let attempts = 0;
    async function check() {
      setChecking(true); setError('');
      try {
        const updated = await platformApi.getPipeline(id, type);
        if (!active) return;
        if (!updated || updated.pipelineId !== id || updated.pipelineType !== type) throw new Error('Pipeline 상세 응답이 요청과 다릅니다.');
        callbacks.current.onUpdated(updated);
        if (isStopSettled(updated.pipelineStatus)) {
          setPhase('idle');
          setNotice(updated.pipelineStatus === 'STOPPED' ? 'Pipeline 배포가 중지되었습니다.' : `Pipeline 상태가 ${updated.pipelineStatus}(으)로 변경되었습니다.`);
          // Detail is authoritative even if the list refresh fails.
          void callbacks.current.refreshList().catch(() => {
            if (mounted.current) setError('상태 확인은 완료됐지만 목록 갱신에 실패했습니다. 상단 새로고침을 눌러 주세요.');
          });
          return;
        }
        attempts++;
        if (attempts < 20) timer = setTimeout(() => void check(), 3000);
        else setNotice('중지 완료가 아직 확인되지 않았습니다. 잠시 후 상태 다시 조회를 눌러 주세요.');
      } catch (cause) {
        if (active) setError(`중지 상태를 확인하지 못했습니다: ${messageOf(cause)} 상태 다시 조회를 눌러 주세요.`);
      } finally { if (active) setChecking(false); }
    }
    void check();
    return () => { active = false; clearTimeout(timer); };
  }, [id, type, polling, refreshKey]);

  return {
    phase, pending, error, notice, checking, stop,
    confirm: () => { setError(''); setNotice(''); setPhase('confirm'); },
    cancel: () => setPhase('idle'),
    refresh: () => setRefreshKey((value) => value + 1),
  };
}

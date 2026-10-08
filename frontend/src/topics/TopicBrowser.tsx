import { useEffect, useState } from 'react';
import { platformApi, type Topic } from '../api/platform';
import './topics.css';

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Topic을 조회하지 못했습니다.';
}

function schemaText(schema?: string) {
  if (!schema?.trim()) return '등록된 Schema가 없습니다.';
  try { return JSON.stringify(JSON.parse(schema), null, 2); } catch { return schema; }
}

export function TopicMetadata({ topic }: { topic: Topic }) {
  return <>
    <div className="panel-heading"><div><h3>저장된 Topic 메타데이터</h3><p>{topic.topicName} · #{topic.topicId}</p></div><span className="format-chip">읽기 전용</span></div>
    <dl className="topic-metadata">
      <div><dt>Topic 이름</dt><dd>{topic.topicName}</dd></div>
      <div><dt>표시 이름</dt><dd>{topic.displayName || '미등록'}</dd></div>
      <div><dt>Message Format</dt><dd>{topic.messageFormat || '미등록'}</dd></div>
      <div><dt>Event Time Field</dt><dd>{topic.timeField || '미등록'}</dd></div>
      <div className="wide"><dt>설명</dt><dd>{topic.description || '등록된 설명이 없습니다.'}</dd></div>
      <div className="wide"><dt>Schema JSON</dt><dd><pre>{schemaText(topic.schemaJson)}</pre></dd></div>
    </dl>
  </>;
}

export function TopicBrowser({ refreshKey = 0 }: { refreshKey?: number }) {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Topic | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [revision, setRevision] = useState(0);
  const [detailRevision, setDetailRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setLoading(true); setError(''); setTopics([]); setSelectedId(null); setDetail(null);
    void platformApi.getTopics(controller.signal).then((items) => {
      if (!active) return;
      if (!Array.isArray(items)) throw new Error('Topic 목록 응답 형식을 확인해 주세요.');
      setTopics(items);
    }).catch((cause) => { if (active) setError(errorMessage(cause)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [revision, refreshKey]);

  useEffect(() => {
    if (selectedId === null) { setDetail(null); setDetailLoading(false); setDetailError(''); return; }
    const controller = new AbortController();
    let active = true;
    setDetail(null); setDetailLoading(true); setDetailError('');
    void platformApi.getTopic(selectedId, controller.signal).then((item) => {
      if (!active) return;
      if (!item || item.topicId !== selectedId) throw new Error('Topic 상세 응답이 요청한 Topic과 다릅니다.');
      setDetail(item);
    }).catch((cause) => { if (active) setDetailError(errorMessage(cause)); })
      .finally(() => { if (active) setDetailLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [selectedId, detailRevision]);

  function selectTopic(id: number) {
    if (id === selectedId) return;
    setDetail(null); setDetailError(''); setDetailLoading(true); setSelectedId(id);
  }

  return <>
    <div className="welcome-row"><div><h2>Topic 조회</h2><p>관리자가 저장한 Topic 설명과 Schema를 확인하세요. 이 화면에서는 메타데이터를 변경하지 않습니다.</p></div><button className="secondary-button" disabled={loading} onClick={() => setRevision((value) => value + 1)}>목록 다시 조회</button></div>
    <div className="connected-split">
      <section className="panel" aria-label="조회 가능한 Topic 목록">
        {loading ? <p className="topic-state" role="status">Topic 목록 조회 중…</p> : error ? <div className="topic-state"><p className="form-error" role="alert">{error}</p><button className="secondary-button" onClick={() => setRevision((value) => value + 1)}>다시 조회</button></div> : topics.length ? <div className="connected-list">{topics.map((topic) => <button key={topic.topicId} className={selectedId === topic.topicId ? 'selected' : ''} aria-pressed={selectedId === topic.topicId} onClick={() => selectTopic(topic.topicId)}><strong>{topic.displayName || topic.topicName}</strong><small>{topic.topicName} · #{topic.topicId}</small><span>{topic.messageFormat || '미등록'}</span></button>)}</div> : <div className="topic-state"><h3>조회 가능한 Topic이 없습니다</h3><p>Topic 등록 상태와 조회 권한을 관리자에게 확인해 주세요.</p></div>}
      </section>
      <section className="panel" aria-label="Topic 메타데이터 상세">
        {selectedId === null ? <div className="topic-state"><h3>Topic을 선택하세요</h3><p>저장된 메타데이터를 읽기 전용으로 표시합니다.</p></div> : detailLoading ? <p className="topic-state" role="status">메타데이터 조회 중…</p> : detailError ? <div className="topic-state"><p className="form-error" role="alert">{detailError}</p><button className="secondary-button" onClick={() => setDetailRevision((value) => value + 1)}>상세 다시 조회</button></div> : detail ? <TopicMetadata topic={detail} /> : null}
      </section>
    </div>
  </>;
}

import { FormEvent, useState } from 'react';

export type PipelineDraft = {
  pipelineName: string;
  description: string;
  pipelineType: 'AI_SQL' | 'CUSTOM_JAR';
  inputTopicIds: number[];
  entryClass?: string;
};

type Topic = { topicId: number; topicName: string; displayName?: string; description?: string };

export function PipelineCreateView({
  topics,
  onCancel,
  onCreate,
}: {
  topics: Topic[];
  onCancel: () => void;
  onCreate: (draft: PipelineDraft) => Promise<void>;
}) {
  const [pipelineType, setPipelineType] = useState<PipelineDraft['pipelineType']>('AI_SQL');
  const [pipelineName, setPipelineName] = useState('');
  const [description, setDescription] = useState('');
  const [entryClass, setEntryClass] = useState('');
  const [inputTopicIds, setInputTopicIds] = useState<number[]>([]);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  function toggleTopic(topicId: number) {
    setInputTopicIds((current) => current.includes(topicId)
      ? current.filter((id) => id !== topicId)
      : [...current, topicId]);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!pipelineName.trim()) {
      setError('파이프라인 이름을 입력해 주세요.');
      return;
    }
    if (pipelineType === 'CUSTOM_JAR' && !entryClass.trim()) {
      setError('Custom Job의 엔트리 클래스를 입력해 주세요.');
      return;
    }
    setError('');
    setIsSubmitting(true);
    try {
      await onCreate({ pipelineName: pipelineName.trim(), description: description.trim(), pipelineType, inputTopicIds, entryClass: entryClass.trim() || undefined });
    } catch {
      setError('파이프라인을 등록하지 못했습니다. 입력값을 확인한 뒤 다시 시도해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return <section className="page-content create-page">
    <button className="back-button" onClick={onCancel}>← 파이프라인 목록</button>
    <div className="create-heading"><div><p className="eyebrow">CREATE PIPELINE</p><h2>새 파이프라인</h2><p>처리할 데이터와 실행 방식을 선택한 뒤 기본 정보를 등록하세요.</p></div><span className="step-indicator">1 <i /> 2 <i /> 3</span></div>
    <form className="pipeline-form" onSubmit={submit} noValidate>
      <section className="form-panel">
        <div className="form-panel-heading"><span className="form-step">01</span><div><h3>파이프라인 유형</h3><p>데이터 처리 방식에 맞는 시작점을 선택하세요.</p></div></div>
        <div className="type-options">
          <button type="button" className={`type-option ${pipelineType === 'AI_SQL' ? 'selected' : ''}`} onClick={() => setPipelineType('AI_SQL')}><span className="type-icon sql">SQL</span><strong>AI SQL Pipeline</strong><p>자연어와 Flink SQL로 데이터 흐름을 설계합니다.</p><i /></button>
          <button type="button" className={`type-option ${pipelineType === 'CUSTOM_JAR' ? 'selected' : ''}`} onClick={() => setPipelineType('CUSTOM_JAR')}><span className="type-icon jar">JAR</span><strong>Custom Job</strong><p>직접 개발한 Flink Job을 업로드하고 실행합니다.</p><i /></button>
        </div>
      </section>
      <section className="form-panel">
        <div className="form-panel-heading"><span className="form-step">02</span><div><h3>기본 정보</h3><p>워크스페이스에서 구분하기 쉬운 이름을 설정하세요.</p></div></div>
        <div className="form-grid"><label>파이프라인 이름 <b>필수</b><input value={pipelineName} onChange={(event) => setPipelineName(event.target.value)} placeholder="예: 주문 실시간 집계" maxLength={100} /></label><label>설명 <span>선택</span><input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="파이프라인의 목적을 간단히 적어주세요" maxLength={500} /></label></div>
        {pipelineType === 'CUSTOM_JAR' && <label className="wide-field">엔트리 클래스 <b>필수</b><input value={entryClass} onChange={(event) => setEntryClass(event.target.value)} placeholder="예: com.streamcell.jobs.OrderAggregationJob" /></label>}
      </section>
      <section className="form-panel">
        <div className="form-panel-heading"><span className="form-step">03</span><div><h3>입력 토픽</h3><p>파이프라인에서 읽을 Kafka 토픽을 선택하세요. 나중에 변경할 수 있습니다.</p></div></div>
        {topics.length ? <div className="topic-selector">{topics.map((topic) => <label className={`topic-choice ${inputTopicIds.includes(topic.topicId) ? 'checked' : ''}`} key={topic.topicId}><input type="checkbox" checked={inputTopicIds.includes(topic.topicId)} onChange={() => toggleTopic(topic.topicId)} /><span><strong>{topic.displayName ?? topic.topicName}</strong><small>{topic.topicName}</small></span><i>{inputTopicIds.includes(topic.topicId) ? '✓' : ''}</i></label>)}</div> : <div className="no-topics">현재 연결된 토픽이 없습니다. 토픽 동기화 후에도 입력 토픽을 설정할 수 있습니다.</div>}
      </section>
      {error && <p className="create-error" role="alert">{error}</p>}
      <div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>취소</button><button type="submit" className="primary-button compact" disabled={isSubmitting}>{isSubmitting ? '등록 중…' : '파이프라인 등록'}</button></div>
    </form>
  </section>;
}

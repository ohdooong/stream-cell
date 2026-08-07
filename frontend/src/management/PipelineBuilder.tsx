import { FormEvent, useState } from 'react';
import type { PipelineItem, TopicItem } from './demoData';

export function PipelineBuilder({ topics, onCancel, onCreate }: {
  topics: TopicItem[];
  onCancel: () => void;
  onCreate: (pipeline: PipelineItem) => void;
}) {
  const [type, setType] = useState<PipelineItem['type']>('AI_SQL');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTopics, setSelectedTopics] = useState<number[]>([topics[0]?.id].filter(Boolean) as number[]);
  const [parallelism, setParallelism] = useState(2);
  const [entryClass, setEntryClass] = useState('');
  const [fileName, setFileName] = useState('');
  const [programArgs, setProgramArgs] = useState('--checkpoint.interval=60000\n--sink.batch-size=1000');
  const [request, setRequest] = useState('주문 이벤트를 1분 단위로 집계해서 주문 건수, 총 주문 금액, 평균 주문 금액을 계산해줘.');
  const [generated, setGenerated] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');

  function switchType(nextType: PipelineItem['type']) {
    setType(nextType);
    setGenerated(false);
    setError('');
  }

  function toggleTopic(id: number) {
    setSelectedTopics((current) => current.includes(id) ? current.filter((topicId) => topicId !== id) : [...current, id]);
  }

  function generatePlan() {
    if (!request.trim() || !selectedTopics.length) {
      setError('입력 토픽과 자연어 요청을 입력해 주세요.');
      return;
    }
    setError('');
    setGenerating(true);
    window.setTimeout(() => {
      setGenerated(true);
      setGenerating(false);
    }, 650);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!name.trim() || !selectedTopics.length) {
      setError('파이프라인 이름과 입력 토픽을 설정해 주세요.');
      return;
    }
    if (type === 'CUSTOM_JAR' && (!entryClass.trim() || !fileName)) {
      setError('JAR 파일과 Entry Class를 입력해 주세요.');
      return;
    }
    if (type === 'AI_SQL' && !generated) {
      setError('AI Pipeline Plan과 SQL을 먼저 생성해 주세요.');
      return;
    }
    onCreate({
      id: Date.now(),
      name: name.trim(),
      description: description.trim() || (type === 'AI_SQL' ? 'AI가 생성한 Flink SQL 파이프라인' : 'Custom Flink Job'),
      type,
      status: 'DRAFT',
      parallelism,
      inputTopics: topics.filter((topic) => selectedTopics.includes(topic.id)).map((topic) => topic.name),
      updatedAt: '방금 전',
    });
  }

  return <div className="mc-page mc-builder">
    <button className="mc-back" onClick={onCancel}>← 파이프라인 목록</button>
    <div className="mc-title-row"><div><span className="mc-kicker">CREATE PIPELINE</span><h2>새 파이프라인</h2><p>Pipeline Type에 맞는 정보를 입력하고 배포 준비를 완료하세요.</p></div><div className="mc-stepper"><b>1</b><i /><b>2</b><i /><b>3</b></div></div>
    <form onSubmit={submit} className="mc-builder-form">
      <section className="mc-panel mc-form-section"><header><span>01</span><div><h3>Pipeline Type</h3><p>생성할 파이프라인의 실행 방식을 선택합니다.</p></div></header><div className="mc-type-grid"><button type="button" className={type === 'AI_SQL' ? 'active' : ''} onClick={() => switchType('AI_SQL')}><em>AI</em><strong>AI_SQL Pipeline</strong><small>자연어 요청을 기반으로 Pipeline Plan과 Flink SQL을 생성합니다.</small><i /></button><button type="button" className={type === 'CUSTOM_JAR' ? 'active' : ''} onClick={() => switchType('CUSTOM_JAR')}><em>JAR</em><strong>Custom JAR Pipeline</strong><small>직접 개발한 Flink Job JAR를 등록하고 배포합니다.</small><i /></button></div></section>
      <section className="mc-panel mc-form-section"><header><span>02</span><div><h3>기본 정보와 입력 Topic</h3><p>목록에서 식별할 이름과 사용할 Kafka Topic을 설정합니다.</p></div></header><div className="mc-form-grid"><label>파이프라인 이름 <b>필수</b><input value={name} onChange={(event) => setName(event.target.value)} placeholder={type === 'AI_SQL' ? '예: 주문 실시간 집계' : '예: 사용자 세션 분석'} /></label><label>설명 <small>선택</small><input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="파이프라인 목적을 입력하세요" /></label></div><div className="mc-field-label">입력 Topic <b>하나 이상 선택</b></div><div className="mc-topic-choices">{topics.map((topic) => <label className={selectedTopics.includes(topic.id) ? 'selected' : ''} key={topic.id}><input type="checkbox" checked={selectedTopics.includes(topic.id)} onChange={() => toggleTopic(topic.id)} /><span><strong>{topic.displayName}</strong><small>{topic.name}</small></span><i>{selectedTopics.includes(topic.id) ? '✓' : ''}</i></label>)}</div></section>

      {type === 'CUSTOM_JAR' ? <section className="mc-panel mc-form-section"><header><span>03</span><div><h3>Custom JAR 설정</h3><p>Artifact와 Flink Job 실행 정보를 등록합니다.</p></div></header><div className="mc-upload"><input id="jar-file" type="file" accept=".jar" onChange={(event) => setFileName(event.target.files?.[0]?.name ?? '')} /><label htmlFor="jar-file"><span>⇧</span><strong>{fileName || 'JAR 파일을 선택하거나 여기에 드래그하세요'}</strong><small>최대 50MB · .jar 형식</small></label></div><div className="mc-form-grid"><label>Entry Class <b>필수</b><input value={entryClass} onChange={(event) => setEntryClass(event.target.value)} placeholder="com.example.StreamingJob" /></label><label>Parallelism<select value={parallelism} onChange={(event) => setParallelism(Number(event.target.value))}><option value="1">1</option><option value="2">2</option><option value="4">4</option><option value="8">8</option></select></label></div><label className="mc-code-field">Program Arguments <small>한 줄에 하나씩 입력</small><textarea value={programArgs} onChange={(event) => setProgramArgs(event.target.value)} rows={4} /></label><div className="mc-next-note"><span>다음 단계</span><p>등록 완료 후 Pipeline 운영 화면에서 JAR 업로드 상태를 확인하고 즉시 배포할 수 있습니다.</p></div></section>
      : <section className="mc-panel mc-form-section"><header><span>03</span><div><h3>AI SQL 생성</h3><p>원하는 처리 로직을 자연어로 설명하면 Plan과 SQL을 생성합니다.</p></div></header><label className="mc-code-field">자연어 요청 <b>필수</b><textarea value={request} onChange={(event) => { setRequest(event.target.value); setGenerated(false); }} rows={5} placeholder="선택한 Topic으로 어떤 데이터를 계산할지 설명하세요." /></label><div className="mc-ai-action"><span><i>✦</i> 선택된 Topic Schema를 AI Context로 사용합니다.</span><button type="button" onClick={generatePlan} disabled={generating}>{generating ? '생성 중…' : '✦ Pipeline Plan 생성'}</button></div>{generated && <div className="mc-ai-preview"><div className="mc-plan"><div className="mc-preview-title"><span>PIPELINE PLAN</span><b>Generated</b></div><ol><li><b>Source</b><span>orders.created.v1에서 JSON 이벤트를 읽습니다.</span></li><li><b>Transform</b><span>1분 Tumbling Window와 Event Time을 적용합니다.</span></li><li><b>Aggregate</b><span>COUNT, SUM, AVG 집계를 계산합니다.</span></li><li><b>Sink</b><span>PostgreSQL order_minute_summary에 Upsert합니다.</span></li></ol></div><div className="mc-sql"><div className="mc-preview-title"><span>FLINK SQL PREVIEW</span><button type="button">Copy</button></div><pre><code>{`INSERT INTO order_minute_summary\nSELECT\n  window_start,\n  window_end,\n  COUNT(*) AS order_count,\n  SUM(amount) AS total_amount,\n  AVG(amount) AS average_amount\nFROM TABLE(\n  TUMBLE(TABLE orders,\n    DESCRIPTOR(createdAt),\n    INTERVAL '1' MINUTE)\n)\nGROUP BY window_start, window_end;`}</code></pre></div></div>}</section>}

      {error && <div className="mc-form-error">! {error}</div>}
      <footer className="mc-form-actions"><button type="button" className="mc-btn secondary" onClick={onCancel}>취소</button><button className="mc-btn primary" type="submit">{type === 'CUSTOM_JAR' ? 'JAR 등록 후 계속' : 'AI_SQL Pipeline 생성'}</button></footer>
    </form>
  </div>;
}

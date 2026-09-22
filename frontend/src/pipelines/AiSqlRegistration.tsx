import { useEffect, useRef, useState, type FormEvent } from 'react';
import { aiSqlApi, type AiSqlPreview } from '../api/aiSql';
import { ApiError } from '../api/client';
import { platformApi, type Topic } from '../api/platform';
import { initialAiSqlDraft, isAiSqlPreview, schemaFields, toAiSqlInput, validateAiSqlInput, type AiSqlDraft } from './aiSqlForm';
import './aiSql.css';

type Props = {
  topics: Topic[];
  userId: number;
  onCreated: (pipelineId: number) => Promise<void>;
  notify: (message: string) => void;
};

function errorMessage(error: unknown) {
  if (error instanceof ApiError && [404, 405, 501].includes(error.status)) {
    return 'AI SQL 생성·등록 기능을 준비 중입니다. 입력한 내용은 이 화면에 유지됩니다.';
  }
  if (error instanceof ApiError && [409, 410].includes(error.status)) {
    return '미리보기가 만료되었거나 Topic 설정이 변경되었습니다. Plan과 SQL을 다시 생성해 주세요.';
  }
  return error instanceof Error ? error.message : '요청을 완료하지 못했습니다. 다시 시도해 주세요.';
}

function formatSchema(schema: string) {
  try { return JSON.stringify(JSON.parse(schema), null, 2); } catch { return schema; }
}

export function AiSqlRegistration({ topics, userId, onCreated, notify }: Props) {
  const [draft, setDraft] = useState<AiSqlDraft>(initialAiSqlDraft);
  const [details, setDetails] = useState<Record<number, Topic>>({});
  const [topicLoading, setTopicLoading] = useState<Record<number, boolean>>({});
  const [topicErrors, setTopicErrors] = useState<Record<number, string>>({});
  const [preview, setPreview] = useState<{ key: string; value: AiSqlPreview } | null>(null);
  const [reviewed, setReviewed] = useState(false);
  const [expired, setExpired] = useState(false);
  const [busy, setBusy] = useState<'generate' | 'save' | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [createdId, setCreatedId] = useState<number | null>(null);
  const requestSequence = useRef(0);
  const topicSequence = useRef<Record<number, number>>({});

  const input = toAiSqlInput(draft, userId);
  const inputKey = JSON.stringify(input);
  const currentPreview = preview?.key === inputKey ? preview.value : null;
  const loadingTopics = draft.topicId !== null && topicLoading[draft.topicId];
  const canSave = currentPreview?.validation.valid && !currentPreview.validation.errors.length && reviewed && !expired;

  useEffect(() => {
    requestSequence.current += 1;
    setPreview(null); setReviewed(false); setBusy(null); setCreatedId(null);
    return () => { requestSequence.current += 1; };
  }, [userId]);

  useEffect(() => {
    setExpired(false);
    if (!currentPreview) return;
    const remaining = Date.parse(currentPreview.expiresAt) - Date.now();
    if (remaining <= 0) { setExpired(true); return; }
    const timer = window.setTimeout(() => setExpired(true), Math.min(remaining, 2_147_483_647));
    return () => window.clearTimeout(timer);
  }, [currentPreview?.expiresAt]);

  function update(patch: Partial<AiSqlDraft>) {
    setDraft((current) => ({ ...current, ...patch }));
    setPreview(null); setReviewed(false); setErrors([]);
  }

  async function loadTopic(id: number) {
    const sequence = (topicSequence.current[id] || 0) + 1;
    topicSequence.current[id] = sequence;
    setTopicLoading((current) => ({ ...current, [id]: true }));
    setTopicErrors((current) => ({ ...current, [id]: '' }));
    setPreview(null); setReviewed(false);
    try {
      const topic = await platformApi.getTopic(id);
      if (topicSequence.current[id] !== sequence) return;
      setDetails((current) => ({ ...current, [id]: topic }));
      setDraft((current) => current.topicId === id ? ({ ...current, timeFields: {
        ...current.timeFields, [id]: current.timeFields[id] ?? topic.timeField ?? '',
      } }) : current);
    } catch (error) {
      if (topicSequence.current[id] === sequence) setTopicErrors((current) => ({ ...current, [id]: errorMessage(error) }));
    } finally {
      if (topicSequence.current[id] === sequence) setTopicLoading((current) => ({ ...current, [id]: false }));
    }
  }

  function selectTopic(id: number) {
    if (draft.topicId === id) return;
    update({ topicId: id, timeFields: {} });
    void loadTopic(id);
  }

  async function generate(event: FormEvent) {
    event.preventDefault();
    if (busy || createdId !== null) return;
    const problems = validateAiSqlInput(input, details);
    if (loadingTopics) problems.push('Topic 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.');
    if (draft.topicId !== null && topicErrors[draft.topicId]) problems.push('상세 조회에 실패한 Topic을 다시 불러와 주세요.');
    setErrors(problems);
    if (problems.length) return;
    const sequence = ++requestSequence.current;
    setBusy('generate'); setPreview(null); setReviewed(false);
    try {
      const result = await aiSqlApi.preview(input);
      if (sequence !== requestSequence.current) return;
      if (!isAiSqlPreview(result)) throw new Error('생성 응답을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.');
      setPreview({ key: inputKey, value: result });
    } catch (error) {
      if (sequence === requestSequence.current) setErrors([errorMessage(error)]);
    } finally {
      if (sequence === requestSequence.current) setBusy(null);
    }
  }

  async function save() {
    if (!currentPreview || !canSave || busy) return;
    if (Date.parse(currentPreview.expiresAt) <= Date.now()) { setExpired(true); return; }
    const sequence = ++requestSequence.current;
    setBusy('save'); setErrors([]);
    try {
      const pipeline = await aiSqlApi.create(input, currentPreview.previewId);
      if (sequence !== requestSequence.current) return;
      if (!Number.isSafeInteger(pipeline?.pipelineId) || pipeline.pipelineId < 1) throw new Error('등록 응답을 확인할 수 없습니다. Pipeline 목록을 확인해 주세요.');
      setCreatedId(pipeline.pipelineId);
      notify('AI SQL Pipeline을 등록했습니다.');
      try { await onCreated(pipeline.pipelineId); }
      catch { setErrors([`Pipeline #${pipeline.pipelineId} 등록은 완료됐지만 목록을 갱신하지 못했습니다. 아래 버튼으로 다시 열어 주세요.`]); }
    } catch (error) {
      if (sequence === requestSequence.current) {
        setErrors([errorMessage(error)]);
        if (error instanceof ApiError && [409, 410].includes(error.status)) { setPreview(null); setReviewed(false); }
      }
    } finally {
      if (sequence === requestSequence.current) setBusy(null);
    }
  }

  return <form className="ai-registration" onSubmit={generate} noValidate>
    <ol className="ai-steps" aria-label="AI SQL 등록 단계">
      <li className={!currentPreview ? 'active' : ''}><b>01</b> 설정 입력</li>
      <li className={currentPreview && !createdId ? 'active' : ''}><b>02</b> Plan · SQL 검토</li>
      <li className={createdId ? 'active' : ''}><b>03</b> Pipeline 등록</li>
    </ol>

    <fieldset disabled={busy !== null || createdId !== null} className="ai-inputs">
      <section className="panel ai-section">
        <header><span>01</span><div><h3>기본 정보</h3><p>분석 작업의 이름과 사용할 데이터를 정해 주세요.</p></div></header>
        <div className="ai-grid">
          <label>Pipeline 이름 <em>필수</em><input value={draft.name} onChange={(e) => update({ name: e.target.value })} maxLength={100} required placeholder="상품별 주문 집계" /></label>
          <label>설명 <small>선택</small><input value={draft.description} onChange={(e) => update({ description: e.target.value })} maxLength={1000} placeholder="이 Pipeline의 목적을 입력하세요" /></label>
        </div>
        <fieldset className="ai-topic-fieldset"><legend>입력 Topic <em>필수 · 하나만 선택</em></legend>
          {topics.length ? <div className="ai-topic-options">{topics.map((topic) => <label key={topic.topicId} className={draft.topicId === topic.topicId ? 'selected' : ''}>
            <input type="radio" name="ai-input-topic" checked={draft.topicId === topic.topicId} onChange={() => selectTopic(topic.topicId)} />
            <span><strong>{topic.displayName || topic.topicName}</strong><small>{topic.topicName}</small></span>
            <i>{topic.messageFormat || 'Schema'}</i>
          </label>)}</div> : <p className="ai-empty">선택할 Topic이 없습니다. Topic 관리에서 동기화한 후 다시 등록해 주세요.</p>}
        </fieldset>
        {draft.topicId !== null && [draft.topicId].map((id) => <div className="ai-topic-schema" key={id}>
          <div><strong>{topics.find((topic) => topic.topicId === id)?.topicName || `Topic #${id}`}</strong><span>{topicLoading[id] ? '불러오는 중…' : 'Topic Schema'}</span></div>
          {topicErrors[id] ? <p role="alert">{topicErrors[id]} <button type="button" className="text-button" onClick={() => void loadTopic(id)}>다시 불러오기</button></p>
            : !topicLoading[id] && details[id] && <>
              {details[id].schemaJson ? <details><summary>Schema 확인 <span>읽기 전용</span></summary><pre>{formatSchema(details[id].schemaJson!)}</pre></details>
                : <p className="ai-inline-warning">Schema가 없습니다. Topic 관리에서 Schema를 등록해 주세요.</p>}
              <small>등록된 Event Time: {details[id].timeField || '미설정'}</small>
            </>}
        </div>)}
      </section>

      <section className="panel ai-section">
        <header><span>02</span><div><h3>처리 요청</h3><p>집계 주기, 그룹 기준, 계산할 값과 필터 조건을 알려 주세요.</p></div></header>
        <label className="ai-prompt">자연어 요청 <em>필수</em><textarea rows={5} maxLength={4000} value={draft.request} onChange={(e) => update({ request: e.target.value })} required placeholder="orders Topic에서 취소된 주문을 제외하고, 5분 단위로 상품별 주문 건수와 총금액을 집계해줘." /></label>
        <p className="ai-help">생성된 Plan에서 해석된 조건을 확인할 수 있습니다. 수정이 필요하면 요청을 바꾸고 다시 생성하세요.</p>
      </section>

      <section className="panel ai-section">
        <header><span>03</span><div><h3>시간 기준 · 결과 저장</h3><p>시간을 계산하는 기준과 집계 결과의 저장 위치입니다.</p></div></header>
        <div className="ai-grid">
          <label>시간 기준<select value={draft.timeMode} onChange={(e) => update({ timeMode: e.target.value as AiSqlDraft['timeMode'] })}><option value="EVENT_TIME">Event Time · 이벤트 발생 시간</option><option value="PROCESSING_TIME">Processing Time · 처리 시간</option></select><small>Event Time을 선택하면 Topic별 시간 필드를 사용합니다.</small></label>
          <label>결과 저장소<input value="관리형 PostgreSQL" readOnly /><small>플랫폼에 설정된 결과 저장소를 사용합니다.</small></label>
          {draft.timeMode === 'EVENT_TIME' && draft.topicId !== null && <label>{topics.find((topic) => topic.topicId === draft.topicId)?.topicName} · Event Time 필드
            <input value={draft.timeFields[draft.topicId] || ''} list="ai-time-fields" onChange={(e) => update({ timeFields: { ...draft.timeFields, [draft.topicId!]: e.target.value } })} placeholder="eventTime" required />
            <datalist id="ai-time-fields">{schemaFields(details[draft.topicId]?.schemaJson).map((field) => <option key={field} value={field} />)}</datalist>
            <small>Topic 설정을 기본값으로 사용합니다. 변경은 이 Pipeline에만 적용됩니다.</small>
          </label>}
          <label>결과 테이블<select value={draft.tableNaming} onChange={(e) => update({ tableNaming: e.target.value as AiSqlDraft['tableNaming'] })}><option value="AUTO">자동 생성</option><option value="CUSTOM">테이블명 직접 지정</option></select><small>자동 생성 시 Pipeline 전용 테이블을 만듭니다.</small></label>
          {draft.tableNaming === 'CUSTOM' && <label>테이블명<input value={draft.tableName} maxLength={63} onChange={(e) => update({ tableName: e.target.value })} placeholder="order_summary_5min" /><small>소문자·숫자·밑줄 사용, 첫 글자는 소문자</small></label>}
        </div>
      </section>

      <details className="panel ai-advanced"><summary>고급 실행 설정 <span>읽기 시작점 · 병렬도 · 시간대</span></summary><div className="ai-grid">
        <label>데이터 읽기 시작점<select value={draft.startupMode} onChange={(e) => update({ startupMode: e.target.value as AiSqlDraft['startupMode'] })}><option value="LATEST">새 데이터부터</option><option value="EARLIEST">보관 중인 가장 오래된 데이터부터</option></select><small>최초 실행에 적용됩니다. 재시작 복구는 저장된 상태를 따릅니다.</small></label>
        <label>병렬도 (Parallelism)<input type="number" min={1} step={1} value={Number.isNaN(draft.parallelism) ? '' : draft.parallelism} onChange={(e) => update({ parallelism: e.target.valueAsNumber })} /></label>
        <label>시간대<input value={draft.timezone} onChange={(e) => update({ timezone: e.target.value })} placeholder="Asia/Seoul" /></label>
        {draft.timeMode === 'EVENT_TIME' && <label>Watermark 지연 (초)<input type="number" min={0} step={1} value={Number.isNaN(draft.watermarkSeconds) ? '' : draft.watermarkSeconds} onChange={(e) => update({ watermarkSeconds: e.target.valueAsNumber })} /><small>이벤트 순서가 뒤바뀌는 지연을 고려하는 설정입니다.</small></label>}
      </div></details>
    </fieldset>

    {errors.length > 0 && <div className="ai-errors" role="alert"><strong>입력 및 연결 상태를 확인해 주세요.</strong><ul>{errors.map((error, index) => <li key={index}>{error}</li>)}</ul></div>}
    <div className="ai-generate-actions"><p>설정을 변경하면 이전 미리보기를 다시 생성해야 합니다.</p><button type="submit" className="primary-button compact" disabled={!!busy || loadingTopics || createdId !== null}>{busy === 'generate' ? 'Plan · SQL 생성 중…' : currentPreview ? 'Plan · SQL 다시 생성' : 'Plan · SQL 생성'}</button></div>

    <section className="panel ai-preview" aria-label="Pipeline Plan 및 SQL 미리보기" aria-busy={busy === 'generate'}>
      <div className="panel-heading"><div><h3>Plan · SQL 미리보기</h3><p>처리 조건과 SQL을 확인한 후 Pipeline을 등록하세요.</p></div>{currentPreview && <span className={`status ${currentPreview.validation.valid ? 'running' : 'failed'}`}>{currentPreview.validation.valid ? '검증 완료' : '검토 필요'}</span>}</div>
      {currentPreview ? <>
        <div className="ai-preview-grid"><article><h4>Pipeline Plan</h4><p>{currentPreview.pipelinePlan.summary}</p><ol>{currentPreview.pipelinePlan.steps.map((step, index) => <li key={index}><strong>{step.title}</strong><p>{step.description}</p></li>)}</ol></article><article><h4>Flink SQL</h4><pre>{currentPreview.generatedSql}</pre></article></div>
        {currentPreview.warnings.length > 0 && <div className="ai-preview-notes"><strong>확인할 사항</strong><ul>{currentPreview.warnings.map((warning, index) => <li key={index}>{warning}</li>)}</ul></div>}
        {currentPreview.validation.errors.length > 0 && <div className="ai-errors" role="alert"><ul>{currentPreview.validation.errors.map((error, index) => <li key={index}>{error}</li>)}</ul></div>}
        <div className="ai-review"><label><input type="checkbox" checked={reviewed} onChange={(e) => setReviewed(e.target.checked)} disabled={!currentPreview.validation.valid || currentPreview.validation.errors.length > 0 || expired || !!busy || createdId !== null} />생성된 처리 조건과 SQL을 확인했습니다.</label><small>{expired ? '미리보기가 만료되었습니다. 다시 생성해 주세요.' : `미리보기 유효 시간: ${new Date(currentPreview.expiresAt).toLocaleString('ko-KR')}`}</small></div>
      </> : <div className="ai-preview-empty"><span>SQL</span><h4>처리할 내용을 입력하고 생성해 주세요.</h4><p>Pipeline Plan과 검증된 Flink SQL이 여기에 표시됩니다.</p></div>}
    </section>
    <div className="ai-save-actions"><p>등록 후 Pipeline 상세에서 배포 상태를 확인할 수 있습니다.</p>{createdId !== null ? <button type="button" className="primary-button compact" onClick={() => void onCreated(createdId).catch(() => setErrors(['목록을 갱신하지 못했습니다. 잠시 후 다시 시도해 주세요.']))}>등록된 Pipeline 열기</button> : <button type="button" className="primary-button compact" disabled={!canSave || !!busy} onClick={() => void save()}>{busy === 'save' ? '등록 중…' : '검토한 Pipeline 등록'}</button>}</div>
  </form>;
}

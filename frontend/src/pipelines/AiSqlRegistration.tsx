import { useEffect, useRef, useState, type FormEvent } from 'react';
import { aiSqlApi, type AiSqlPreview, type PipelinePlan } from '../api/aiSql';
import { ApiError } from '../api/client';
import { platformApi, type Topic } from '../api/platform';
import { assertInputTopic } from '../api/inputTopics';
import {
  initialAiSqlDraft, isAiSqlPreview, toAiSqlConfigInput, toAiSqlInput,
  toAiSqlPreviewRequest, validateAiSqlInput, type AiSqlDraft,
} from './aiSqlForm';
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
  return error instanceof Error ? error.message : '요청을 완료하지 못했습니다. 다시 시도해 주세요.';
}

function formatSchema(schema: string) {
  try { return JSON.stringify(JSON.parse(schema), null, 2); } catch { return schema; }
}

const windowUnits: Record<string, string> = { SECOND: '초', MINUTE: '분', HOUR: '시간', DAY: '일' };
const windowTypes: Record<string, string> = { TUMBLE: 'Tumbling Window', HOP: 'Hopping Window', SESSION: 'Session Window' };
const operators: Record<string, string> = { EQ: '=', NE: '≠', NEQ: '≠', GT: '>', GTE: '≥', LT: '<', LTE: '≤', IN: '포함', NOT_IN: '미포함' };

function displayValue(value: unknown) {
  if (typeof value === 'string') return value;
  if (value === null) return 'null';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

export function PlanPreview({ plan, topics }: { plan: PipelinePlan; topics: Topic[] }) {
  const topic = topics.find((item) => item.topicId === plan.sourceTopicId);
  return <div className="ai-plan">
    <div className="ai-plan-row"><span>입력 Topic</span><strong>{topic?.displayName || topic?.topicName || `Topic #${plan.sourceTopicId}`}</strong></div>
    <div className="ai-plan-row"><span>Window</span><strong>{plan.window.size}{windowUnits[plan.window.unit] || ` ${plan.window.unit}`} · {windowTypes[plan.window.type] || plan.window.type}</strong></div>
    <div className="ai-plan-block"><span>그룹 기준</span>{plan.groupBy.length ? <div className="ai-plan-chips">{plan.groupBy.map((field) => <code key={field}>{field}</code>)}</div> : <strong>없음</strong>}</div>
    <div className="ai-plan-block"><span>필터</span>{plan.filters.length ? <ul>{plan.filters.map((filter, index) => <li key={`${filter.field}-${index}`}><code>{filter.field}</code> {operators[filter.operator] || filter.operator} <b>{displayValue(filter.value)}</b></li>)}</ul> : <strong>없음</strong>}</div>
    <div className="ai-plan-block"><span>집계</span><ul>{plan.aggregations.map((aggregation, index) => <li key={`${aggregation.alias}-${index}`}><code>{aggregation.function}({aggregation.field})</code><i>→</i><b>{aggregation.alias}</b></li>)}</ul></div>
  </div>;
}

export function AiSqlRegistration({ topics, userId, onCreated, notify }: Props) {
  const [draft, setDraft] = useState<AiSqlDraft>(initialAiSqlDraft);
  const [details, setDetails] = useState<Record<number, Topic>>({});
  const [topicLoading, setTopicLoading] = useState<Record<number, boolean>>({});
  const [topicErrors, setTopicErrors] = useState<Record<number, string>>({});
  const [preview, setPreview] = useState<{ key: string; value: AiSqlPreview } | null>(null);
  const [reviewed, setReviewed] = useState(false);
  const [busy, setBusy] = useState<'generate' | 'save' | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [basePipelineId, setBasePipelineId] = useState<number | null>(null);
  const [createdId, setCreatedId] = useState<number | null>(null);
  const requestSequence = useRef(0);
  const topicSequence = useRef<Record<number, number>>({});

  const input = toAiSqlInput(draft, userId);
  const inputKey = JSON.stringify(input);
  const currentPreview = preview?.key === inputKey ? preview.value : null;
  const loadingTopics = draft.topicId !== null && topicLoading[draft.topicId];
  const canSave = Boolean(currentPreview && reviewed);

  useEffect(() => {
    requestSequence.current += 1;
    setPreview(null); setReviewed(false); setBusy(null); setBasePipelineId(null); setCreatedId(null);
    return () => { requestSequence.current += 1; };
  }, [userId]);

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
    } catch (error) {
      if (topicSequence.current[id] === sequence) setTopicErrors((current) => ({ ...current, [id]: errorMessage(error) }));
    } finally {
      if (topicSequence.current[id] === sequence) setTopicLoading((current) => ({ ...current, [id]: false }));
    }
  }

  function selectTopic(id: number) {
    if (draft.topicId === id) return;
    update({ topicId: id });
    void loadTopic(id);
  }

  async function generate(event: FormEvent) {
    event.preventDefault();
    if (busy || createdId !== null) return;
    const problems = validateAiSqlInput(input, details);
    try { assertInputTopic(topics, draft.topicId); } catch (cause) { problems.push(errorMessage(cause)); }
    if (loadingTopics) problems.push('Topic 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.');
    if (draft.topicId !== null && topicErrors[draft.topicId]) problems.push('상세 조회에 실패한 Topic을 다시 불러와 주세요.');
    setErrors(problems);
    if (problems.length) return;
    const sequence = ++requestSequence.current;
    setBusy('generate'); setPreview(null); setReviewed(false);
    try {
      const result = await aiSqlApi.preview(toAiSqlPreviewRequest(input));
      if (sequence !== requestSequence.current) return;
      if (!isAiSqlPreview(result)) throw new Error('생성 응답을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.');
      if (result.pipelinePlan.sourceTopicId !== input.inputTopicId) throw new Error('생성된 Plan의 입력 Topic이 선택한 Topic과 다릅니다. 다시 생성해 주세요.');
      setPreview({ key: inputKey, value: result });
    } catch (error) {
      if (sequence === requestSequence.current) setErrors([errorMessage(error)]);
    } finally {
      if (sequence === requestSequence.current) setBusy(null);
    }
  }

  async function save() {
    if (!currentPreview || !canSave || busy) return;
    const sequence = ++requestSequence.current;
    let pipelineId = basePipelineId;
    setBusy('save'); setErrors([]);
    try {
      assertInputTopic(topics, draft.topicId);
      if (pipelineId === null) {
        const pipeline = await platformApi.createPipeline({
          ownerUserId: input.ownerUserId,
          pipelineName: input.pipelineName,
          description: input.description,
          pipelineType: 'AI_SQL',
        });
        if (!Number.isSafeInteger(pipeline?.pipelineId) || pipeline.pipelineId < 1) throw new Error('Pipeline 생성 응답을 확인할 수 없습니다.');
        pipelineId = pipeline.pipelineId;
        setBasePipelineId(pipelineId);
      }
      await aiSqlApi.createConfig(pipelineId, toAiSqlConfigInput(input, currentPreview.pipelinePlan));
      if (sequence !== requestSequence.current) return;
      setCreatedId(pipelineId);
      notify('AI SQL Pipeline을 등록했습니다.');
      try { await onCreated(pipelineId); }
      catch { setErrors([`Pipeline #${pipelineId} 등록은 완료됐지만 목록을 갱신하지 못했습니다. 아래 버튼으로 다시 열어 주세요.`]); }
    } catch (error) {
      if (sequence === requestSequence.current) {
        setErrors([pipelineId === null
          ? errorMessage(error)
          : `Pipeline #${pipelineId}의 기본 정보는 생성됐지만 AI 정보 등록에 실패했습니다. 다시 시도해 주세요: ${errorMessage(error)}`]);
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

    <fieldset disabled={busy !== null || basePipelineId !== null || createdId !== null} className="ai-inputs">
      <section className="panel ai-section">
        <header><span>01</span><div><h3>기본 정보</h3><p>분석 작업의 이름과 사용할 데이터를 정해 주세요.</p></div></header>
        <div className="ai-grid">
          <label>Pipeline 이름 <em>필수</em><input value={draft.name} onChange={(e) => update({ name: e.target.value })} maxLength={100} required placeholder="상품별 주문 집계" /></label>
          <label>설명 <small>선택</small><input value={draft.description} onChange={(e) => update({ description: e.target.value })} maxLength={1000} placeholder="이 Pipeline의 목적을 입력하세요" /></label>
        </div>
        <fieldset className="ai-topic-fieldset"><legend>입력 Topic <em>DEPLOY 권한 · 하나만 선택</em></legend>
          {topics.length ? <div className="ai-topic-options">{topics.map((topic) => <label key={topic.topicId} className={draft.topicId === topic.topicId ? 'selected' : ''}>
            <input type="radio" name="ai-input-topic" checked={draft.topicId === topic.topicId} onChange={() => selectTopic(topic.topicId)} />
            <span><strong>{topic.displayName || topic.topicName}</strong><small>{topic.topicName}</small></span>
            <i>{topic.messageFormat || 'Schema'}</i>
          </label>)}</div> : <p className="ai-empty">DEPLOY 권한이 있는 Topic이 없습니다. 관리자에게 권한을 요청해 주세요.</p>}
        </fieldset>
        {draft.topicId !== null && [draft.topicId].map((id) => <div className="ai-topic-schema" key={id}>
          <div><strong>{topics.find((topic) => topic.topicId === id)?.topicName || `Topic #${id}`}</strong><span>{topicLoading[id] ? '불러오는 중…' : 'Topic Schema'}</span></div>
          {topicErrors[id] ? <p role="alert">{topicErrors[id]} <button type="button" className="text-button" onClick={() => void loadTopic(id)}>다시 불러오기</button></p>
            : !topicLoading[id] && details[id] && <>
              {details[id].schemaJson ? <details><summary>Schema 확인 <span>읽기 전용</span></summary><pre>{formatSchema(details[id].schemaJson!)}</pre></details>
                : <p className="ai-inline-warning">Schema가 없습니다. Topic 관리에서 Schema를 등록해 주세요.</p>}
            </>}
        </div>)}
      </section>

      <section className="panel ai-section">
        <header><span>02</span><div><h3>처리 요청</h3><p>집계 주기, 그룹 기준, 계산할 값과 필터 조건을 알려 주세요.</p></div></header>
        <label className="ai-prompt">자연어 요청 <em>필수</em><textarea rows={5} maxLength={4000} value={draft.request} onChange={(e) => update({ request: e.target.value })} required placeholder="orders Topic에서 취소된 주문을 제외하고, 5분 단위로 상품별 주문 건수와 총금액을 집계해줘." /></label>
        <p className="ai-help">생성된 Plan에서 해석된 조건을 확인할 수 있습니다. 수정이 필요하면 요청을 바꾸고 다시 생성하세요.</p>
      </section>

    </fieldset>

    {errors.length > 0 && <div className="ai-errors" role="alert"><strong>입력 및 연결 상태를 확인해 주세요.</strong><ul>{errors.map((error, index) => <li key={index}>{error}</li>)}</ul></div>}
    <div className="ai-generate-actions"><p>설정을 변경하면 이전 미리보기를 다시 생성해야 합니다.</p><button type="submit" className="primary-button compact" disabled={!!busy || loadingTopics || basePipelineId !== null || createdId !== null}>{busy === 'generate' ? 'Plan · SQL 생성 중…' : currentPreview ? 'Plan · SQL 다시 생성' : 'Plan · SQL 생성'}</button></div>

    <section className="panel ai-preview" aria-label="Pipeline Plan 및 SQL 미리보기" aria-busy={busy === 'generate'}>
      <div className="panel-heading"><div><h3>Plan · SQL 미리보기</h3><p>처리 조건과 SQL을 확인한 후 Pipeline을 등록하세요.</p></div>{currentPreview && <span className="status running">생성 완료</span>}</div>
      {currentPreview ? <>
        <div className="ai-preview-grid"><article><h4>Pipeline Plan</h4><PlanPreview plan={currentPreview.pipelinePlan} topics={topics} /></article><article><h4>Flink SQL</h4><pre>{currentPreview.generatedFlinkSql}</pre></article></div>
        <div className="ai-review"><label><input type="checkbox" checked={reviewed} onChange={(e) => setReviewed(e.target.checked)} disabled={!!busy || createdId !== null} />생성된 처리 조건과 SQL을 확인했습니다.</label><small>확인한 Pipeline Plan이 AI 정보 등록 API로 전달됩니다.</small></div>
      </> : <div className="ai-preview-empty"><span>SQL</span><h4>처리할 내용을 입력하고 생성해 주세요.</h4><p>Pipeline Plan과 검증된 Flink SQL이 여기에 표시됩니다.</p></div>}
    </section>
    <div className="ai-save-actions"><p>기본 Pipeline을 생성한 뒤 검토한 AI 정보를 연결해 등록합니다.</p>{createdId !== null ? <button type="button" className="primary-button compact" onClick={() => void onCreated(createdId).catch(() => setErrors(['목록을 갱신하지 못했습니다. 잠시 후 다시 시도해 주세요.']))}>등록된 Pipeline 열기</button> : <button type="button" className="primary-button compact" disabled={!canSave || !!busy} onClick={() => void save()}>{busy === 'save' ? '등록 중…' : basePipelineId !== null ? 'AI 정보 다시 등록' : '검토한 Pipeline 등록'}</button>}</div>
  </form>;
}

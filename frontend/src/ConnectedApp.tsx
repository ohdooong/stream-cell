import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { ApiError } from './api/client';
import {
  platformApi, type ClusterOverview, type Pipeline, type PipelineDetail as PipelineDetailData, type PipelineStatus, type PipelineType,
  type Topic, type TopicPermission, type TopicPermissionType, type User,
} from './api/platform';
import { useAuth } from './auth/AuthContext';
import { AiSqlRegistration, PlanPreview } from './pipelines/AiSqlRegistration';
import { isPipelinePlan } from './pipelines/aiSqlForm';
import { aiSqlDetail, customJarDetail, programArgsText } from './pipelines/detailData';
import { CustomJarFields } from './pipelines/CustomJarFields';
import { CustomJarRecovery } from './pipelines/CustomJarRecovery';
import { canRegisterCustomJar, customJarDraft, isCustomJarRegistered, registerCustomJar } from './pipelines/customJarRegistration';
import { canStopPipeline, usePipelineStop } from './pipelines/usePipelineStop';
import { ResultsDashboard } from './results/ResultsDashboard';
import './connected.css';

type View = 'overview' | 'cluster' | 'topics' | 'permissions' | 'pipelines' | 'create' | 'detail' | 'results' | 'failures';
const titles: Record<View, string> = { overview: 'Overview', cluster: 'Flink Cluster', topics: 'Topic 관리', permissions: 'Topic 권한', pipelines: 'Pipeline 운영', create: '새 Pipeline', detail: 'Pipeline 상세', results: '결과 Dashboard', failures: '실패 분석' };
const nav: Array<[View, string, string]> = [['overview', '▦', 'Overview'], ['cluster', '◉', 'Flink Cluster'], ['topics', '≡', 'Topics'], ['permissions', '⌁', 'Topic 권한'], ['pipelines', '⌘', 'Pipelines'], ['results', '▥', '결과 Dashboard'], ['failures', '△', '실패 분석']];

function messageOf(error: unknown) { return error instanceof ApiError ? error.message : error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'; }
function Brand() { return <div className="brand inverse"><span className="brand-symbol"><i /><i /><i /></span><strong>StreamCell</strong></div>; }
function FlowIllustration() {
  return <div className="auth-flow-board" aria-label="Kafka Topic에서 Pipeline을 거쳐 처리 결과로 이어지는 데이터 흐름 예시">
    <div className="auth-flow-board-heading"><span><i /> PIPELINE FLOW</span><small>데이터 처리 흐름 예시</small></div>
    <div className="auth-flow-track">
      <div className="auth-flow-node">
        <span className="auth-flow-icon source-icon"><svg viewBox="0 0 32 32" fill="none" aria-hidden="true"><ellipse cx="16" cy="8" rx="10" ry="4" /><path d="M6 8v15c0 2.2 4.5 4 10 4s10-1.8 10-4V8M6 15c0 2.2 4.5 4 10 4s10-1.8 10-4" /></svg></span>
        <small>SOURCE</small><strong>Kafka Topic</strong><span>이벤트 수집</span>
      </div>
      <div className="auth-flow-link" aria-hidden="true"><i /><i /><i /></div>
      <div className="auth-flow-node featured">
        <span className="auth-flow-icon pipeline-icon"><svg viewBox="0 0 32 32" fill="none" aria-hidden="true"><rect x="4" y="11" width="7" height="10" rx="2" /><rect x="21" y="11" width="7" height="10" rx="2" /><path d="M11 16h10M15 11l5 5-5 5" /></svg></span>
        <small>PROCESS</small><strong>Pipeline</strong><span>실시간 처리</span>
      </div>
      <div className="auth-flow-link" aria-hidden="true"><i /><i /><i /></div>
      <div className="auth-flow-node">
        <span className="auth-flow-icon result-icon"><svg viewBox="0 0 32 32" fill="none" aria-hidden="true"><path d="M5 25h22M8 21v-5M14 21V9M20 21v-8M26 21V6" strokeLinecap="round" strokeWidth="3" /></svg></span>
        <small>OBSERVE</small><strong>Result</strong><span>결과 확인</span>
      </div>
    </div>
    <div className="auth-flow-board-footer"><span><i /> Topic</span><b>→</b><span><i /> Pipeline</span><b>→</b><span><i /> Dashboard</span></div>
  </div>;
}
function Status({ value }: { value: PipelineStatus | string }) { return <span className={`status ${value === 'RUNNING' ? 'running' : value === 'FAILED' ? 'failed' : ''}`}><i />{value}</span>; }
function Empty({ title, children }: { title: string; children: ReactNode }) { return <div className="empty-state"><span>◇</span><h3>{title}</h3><p>{children}</p></div>; }
function Field({ label, hint, wide, children }: { label: string; hint?: string; wide?: boolean; children: ReactNode }) { return <label className={`connected-field ${wide ? 'wide' : ''}`}><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>; }

export function App() {
  const { user, signOut } = useAuth();
  if (!user) return <Login />;
  return <Console defaultUserId={user.userId} onSignOut={signOut} />;
}

function Login() {
  const { signIn } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try { await signIn(loginId, password); }
    catch (cause) { setError(messageOf(cause)); setBusy(false); }
  }
  return <main className="auth-page">
    <section className="auth-art">
      <div className="auth-art-inner">
        <Brand />
        <div className="art-copy">
          <p className="eyebrow"><span /> STREAMCELL CONTROL CENTER</p>
          <h1>흐르는 데이터를<br /><em>운영 가능한 흐름</em>으로.</h1>
          <p className="auth-art-description">Topic에서 시작된 이벤트가 Pipeline을 지나 결과가 되는 순간까지, 한곳에서 설계하고 관리하세요.</p>
        </div>
        <FlowIllustration />
        <div className="auth-art-footer"><span>STREAMING DATA OPERATIONS</span><span>TOPIC <b>·</b> PIPELINE <b>·</b> DEPLOYMENT</span></div>
      </div>
    </section>
    <section className="auth-panel">
      <form className="login-card" onSubmit={submit}>
        <div className="auth-form-emblem" aria-hidden="true"><span className="brand-symbol"><i /><i /><i /></span></div>
        <div className="login-heading"><p className="auth-form-eyebrow">WELCOME TO STREAMCELL</p><h2>관리 콘솔 로그인</h2><p>실시간 데이터 파이프라인을 관리할 준비가 되셨나요?<br />계정으로 로그인해 계속하세요.</p></div>
        <label htmlFor="login-id">아이디</label>
        <div className="field"><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="8" r="3.25" /><path d="M5.5 19c.4-3.1 2.9-5 6.5-5s6.1 1.9 6.5 5" /></svg><input id="login-id" autoComplete="username" placeholder="아이디를 입력하세요" value={loginId} onChange={(e) => setLoginId(e.target.value)} required /></div>
        <div className="label-row"><label htmlFor="login-password">비밀번호</label></div>
        <div className="field"><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" /></svg><input id="login-password" type="password" autoComplete="current-password" placeholder="비밀번호를 입력하세요" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button auth-submit" disabled={busy}>{busy ? '로그인 중…' : <>로그인하고 시작하기 <span aria-hidden="true">→</span></>}</button>
        <p className="auth-form-note"><span aria-hidden="true">●</span> 승인된 StreamCell 계정으로만 접속할 수 있습니다.</p>
      </form>
    </section>
  </main>;
}

function Console({ defaultUserId, onSignOut }: { defaultUserId: number; onSignOut: () => Promise<void> }) {
  const { user, authEnabled } = useAuth();
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  const [view, setView] = useState<View>('overview'); const [users, setUsers] = useState<User[]>([]); const [topics, setTopics] = useState<Topic[]>([]); const [pipelines, setPipelines] = useState<Pipeline[]>([]); const [cluster, setCluster] = useState<ClusterOverview | null>(null); const [activeUserId, setActiveUserId] = useState(defaultUserId); const [selectedPipeline, setSelectedPipeline] = useState<{ id: number; type: PipelineType } | null>(null); const [loading, setLoading] = useState(true); const [notice, setNotice] = useState(''); const [error, setError] = useState('');
  useEffect(() => { if (view === 'permissions' && !isAdmin) setView('overview'); }, [view, isAdmin]);
  const success = (message: string) => { setError(''); setNotice(message); window.setTimeout(() => setNotice(''), 4000); };
  const fail = (cause: unknown) => { setNotice(''); setError(messageOf(cause)); };
  const refreshTopics = async () => setTopics(await platformApi.getTopics());
  const refreshPipelines = async () => setPipelines(await platformApi.getPipelines(activeUserId));
  const refreshCluster = async () => setCluster(await platformApi.getClusterOverview());
  useEffect(() => { Promise.allSettled([platformApi.getUsers(), platformApi.getTopics(), platformApi.getClusterOverview()]).then(([u, t, c]) => { if (u.status === 'fulfilled') setUsers(u.value); if (t.status === 'fulfilled') setTopics(t.value); if (c.status === 'fulfilled') setCluster(c.value); const count = [u, t, c].filter((item) => item.status === 'rejected').length; if (count) setError(`일부 API를 불러오지 못했습니다. 백엔드(기본 포트 8085) 실행 상태를 확인해 주세요. (${count}/3)`); setLoading(false); }); }, []);
  useEffect(() => { platformApi.getPipelines(activeUserId).then(setPipelines).catch(fail); }, [activeUserId]);
  const activeUser = users.find((item) => item.userId === activeUserId);
  const [resultsPipelineId, setResultsPipelineId] = useState<number | null>(null);
  const openResults = (id: number) => { setResultsPipelineId(id); setView('results'); };
  const accountName = activeUser?.name || user?.displayName || user?.username || '사용자';
  const openPipeline = (id: number, type: PipelineType) => { setSelectedPipeline({ id, type }); setView('detail'); };
  return <div className="app-shell">
    <aside className="sidebar">
      <Brand />
      <nav>{nav.filter(([id]) => id !== 'permissions' || isAdmin).map(([id, icon, label]) => <button key={id} className={`nav-item ${view === id || (id === 'pipelines' && (view === 'create' || view === 'detail')) ? 'active' : ''}`} onClick={() => setView(id)}><b>{icon}</b><span>{label}</span></button>)}</nav>
      <div className="sidebar-bottom">
        <div className="help-card"><p>Backend integration</p><a href="http://localhost:8085/swagger-ui/index.html" target="_blank" rel="noreferrer">Swagger UI ↗</a></div>
        <button className="account-button" onClick={() => void onSignOut()}><span className="avatar">{accountName.slice(0, 1)}</span><span><strong>{accountName}</strong><small>{authEnabled ? '로그아웃' : '개발 사용자'}</small></span></button>
      </div>
    </aside>
    <section className="workspace">
      <header className="topbar"><div><p className="breadcrumb">Management <span>/</span> {titles[view]}</p><h1>{titles[view]}</h1></div><div className="top-actions">
        {authEnabled ? <span className="api-user">{accountName} · #{activeUserId}</span> : <label className="api-user">API 사용자<select value={activeUserId} onChange={(e) => setActiveUserId(Number(e.target.value))}>{users.length ? users.map((item) => <option key={item.userId} value={item.userId}>{item.name} · #{item.userId}</option>) : <option value={activeUserId}>User #{activeUserId}</option>}</select></label>}
        <button className="secondary-button" onClick={() => void Promise.all([refreshCluster(), refreshTopics(), refreshPipelines()]).then(() => success('데이터를 새로고침했습니다.')).catch(fail)}>새로고침</button>
      </div></header>
      {notice && <div className="toast connected-success">✓ {notice}</div>}
      {error && <div className="toast connected-error">! {error}<button onClick={() => setError('')}>닫기</button></div>}
      <main className="page-content">{loading ? <Empty title="API 연결 중">백엔드 데이터를 불러오고 있습니다.</Empty> : <>
        {view === 'overview' && <Overview cluster={cluster} topics={topics} pipelines={pipelines} navigate={setView} open={openPipeline} />}
        {view === 'cluster' && <Cluster cluster={cluster} refresh={() => void refreshCluster().then(() => success('Cluster 상태를 갱신했습니다.')).catch(fail)} />}
        {view === 'topics' && <Topics topics={topics} refresh={refreshTopics} success={success} fail={fail} />}
        {view === 'permissions' && isAdmin && <Permissions topics={topics} users={users} activeUserId={activeUserId} success={success} fail={fail} />}
        {view === 'pipelines' && <Pipelines pipelines={pipelines} create={() => setView('create')} open={openPipeline} />}
        {view === 'create' && <CreatePipeline topics={topics} activeUserId={activeUserId} success={success} fail={fail} refreshList={refreshPipelines} done={async (id, type) => { await refreshPipelines(); openPipeline(id, type); }} />}
        {view === 'detail' && selectedPipeline && <PipelineDetail key={`${selectedPipeline.type}-${selectedPipeline.id}`} id={selectedPipeline.id} type={selectedPipeline.type} userId={activeUserId} topics={topics} success={success} fail={fail} refreshList={refreshPipelines} openResults={openResults} />}
        {view === 'results' && <ResultsDashboard key={activeUserId} pipelines={pipelines} initialPipelineId={resultsPipelineId} openPipeline={openPipeline} />}
        {view === 'failures' && <Unavailable title="실패 분석 API가 필요합니다">원본 Exception과 AI 분석 결과 조회 엔드포인트가 구현되면 이 화면에 연결할 수 있습니다.</Unavailable>}
      </>}</main>
    </section>
  </div>;
}

function Overview({ cluster, topics, pipelines, navigate, open }: { cluster: ClusterOverview | null; topics: Topic[]; pipelines: Pipeline[]; navigate: (view: View) => void; open: (id: number, type: PipelineType) => void }) {
  return <><div className="welcome-row"><div><h2>Streaming Platform 현황</h2><p>백엔드 API에서 조회한 최신 운영 상태입니다.</p></div><button className="primary-button compact" onClick={() => navigate('create')}>＋ 새 Pipeline</button></div><div className="stats-grid"><Stat title="Running Jobs" value={cluster?.['jobs-running'] ?? '—'} hint={`${cluster?.['jobs-failed'] ?? 0} failed`} tone="blue" /><Stat title="Kafka Topics" value={topics.length} hint="synced topics" tone="green" /><Stat title="Available Slots" value={cluster?.['slots-available'] ?? '—'} hint={`/ ${cluster?.['slots-total'] ?? '—'} total`} tone="amber" /></div><div className="dashboard-grid"><section className="panel"><div className="panel-heading"><div><h3>내 Pipeline</h3><p>선택한 사용자의 Pipeline 목록</p></div><button className="text-button" onClick={() => navigate('pipelines')}>전체 보기 →</button></div>{pipelines.length ? <div className="pipeline-list">{pipelines.slice(0, 5).map((p) => <div className="pipeline-row" key={p.pipelineId}><span className="pipeline-mark">⌘</span><div><strong>{p.pipelineName}</strong><small>{p.pipelineType} · #{p.pipelineId}</small></div><Status value={p.pipelineStatus} /><button className="row-action" onClick={() => open(p.pipelineId, p.pipelineType)}>열기 →</button></div>)}</div> : <Empty title="Pipeline이 없습니다">새 Pipeline을 등록해 운영을 시작하세요.</Empty>}</section><section className="panel"><div className="panel-heading"><div><h3>Flink Cluster</h3><p>{cluster?.['flink-version'] || '연결 정보 없음'}</p></div><span className="live-pill"><i /> LIVE API</span></div><div className="cluster-summary"><b>{cluster?.taskmanagers ?? '—'}</b><span>TaskManagers</span><b>{cluster?.['slots-total'] ?? '—'}</b><span>Total Slots</span><b>{cluster?.['jobs-finished'] ?? '—'}</b><span>Finished Jobs</span></div></section></div></>;
}
function Stat({ title, value, hint, tone }: { title: string; value: number | string; hint: string; tone: string }) { return <article className="stat-card"><span className={`stat-icon ${tone}`}>◇</span><p>{title}</p><strong>{value}</strong><small>{hint}</small></article>; }

function Cluster({ cluster, refresh }: { cluster: ClusterOverview | null; refresh: () => void }) {
  if (!cluster) return <Unavailable title="Flink Cluster에 연결할 수 없습니다"><button className="secondary-button" onClick={refresh}>다시 조회</button></Unavailable>;
  const metrics = [['TaskManagers', cluster.taskmanagers], ['Total Slots', cluster['slots-total']], ['Available Slots', cluster['slots-available']], ['Running Jobs', cluster['jobs-running']], ['Finished Jobs', cluster['jobs-finished']], ['Failed Jobs', cluster['jobs-failed']], ['Cancelled Jobs', cluster['jobs-cancelled']]];
  return <><div className="welcome-row"><div><h2>Apache Flink {cluster['flink-version']}</h2><p>Cluster Overview API 실시간 응답</p></div><button className="secondary-button" onClick={refresh}>상태 갱신</button></div><div className="connected-metrics">{metrics.map(([label, value]) => <article key={label}><span>{label}</span><strong>{value}</strong></article>)}</div></>;
}

function Topics({ topics, refresh, success, fail }: { topics: Topic[]; refresh: () => Promise<void>; success: (m: string) => void; fail: (e: unknown) => void }) {
  const [selected, setSelected] = useState<Topic | null>(null); const [busy, setBusy] = useState(false); const [form, setForm] = useState({ displayName: '', description: '', messageFormat: 'JSON', timeField: '', schemaJson: '{\n  "type": "object",\n  "properties": {}\n}' });
  async function choose(id: number) { setBusy(true); try { const item = await platformApi.getTopic(id); setSelected(item); setForm({ displayName: item.displayName || '', description: item.description || '', messageFormat: item.messageFormat || 'JSON', timeField: item.timeField || '', schemaJson: item.schemaJson || '{\n  "type": "object",\n  "properties": {}\n}' }); } catch (e) { fail(e); } finally { setBusy(false); } }
  async function save(event: FormEvent) { event.preventDefault(); if (!selected) return; setBusy(true); try { await platformApi.updateTopicSchema(selected.topicId, form); await refresh(); success('Topic Schema와 Event Time 설정을 저장했습니다.'); } catch (e) { fail(e); } finally { setBusy(false); } }
  async function sync() { if (busy) return; setBusy(true); try { await platformApi.syncTopics(); await refresh(); success('Topic 동기화를 완료했습니다.'); } catch (e) { fail(e); } finally { setBusy(false); } }
  return <><div className="welcome-row"><div><h2>Kafka Topics</h2><p>Topic 목록, Schema 및 Event Time을 관리합니다.</p></div><button className="primary-button compact" onClick={() => void sync()} disabled={busy}>↻ Topic 동기화</button></div><div className="connected-split"><section className="panel">{topics.length ? <div className="connected-list">{topics.map((t) => <button key={t.topicId} className={selected?.topicId === t.topicId ? 'selected' : ''} onClick={() => void choose(t.topicId)}><strong>{t.displayName || t.topicName}</strong><small>{t.topicName} · #{t.topicId}</small><span>{t.messageFormat || '미설정'}</span></button>)}</div> : <Empty title="Topic이 없습니다">Topic 동기화를 실행하세요.</Empty>}</section><section className="panel">{selected ? <form onSubmit={save}><div className="panel-heading"><div><h3>Topic 설정</h3><p>{selected.topicName}</p></div><button className="primary-button compact" disabled={busy}>저장</button></div><div className="connected-form"><Field label="표시 이름"><input value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></Field><Field label="Message Format"><select value={form.messageFormat} onChange={(e) => setForm({ ...form, messageFormat: e.target.value })}><option>JSON</option><option>AVRO</option><option>PROTOBUF</option></select></Field><Field label="설명" wide><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field><Field label="Event Time Field" wide hint="Schema JSON에 존재하는 필드명을 입력하세요."><input value={form.timeField} onChange={(e) => setForm({ ...form, timeField: e.target.value })} /></Field><Field label="Schema JSON" wide><textarea className="connected-code" rows={13} value={form.schemaJson} onChange={(e) => setForm({ ...form, schemaJson: e.target.value })} /></Field></div></form> : <Empty title="Topic을 선택하세요">상세 정보와 Schema를 API에서 불러옵니다.</Empty>}</section></div></>;
}

function Permissions({ topics, users, activeUserId, success, fail }: { topics: Topic[]; users: User[]; activeUserId: number; success: (m: string) => void; fail: (e: unknown) => void }) {
  const [topicId, setTopicId] = useState(0); const [userId, setUserId] = useState(activeUserId); const [type, setType] = useState<TopicPermissionType>('VIEW'); const [items, setItems] = useState<TopicPermission[]>([]); const [mine, setMine] = useState<TopicPermission[]>([]);
  useEffect(() => { if (!topicId && topics[0]) setTopicId(topics[0].topicId); }, [topicId, topics]);
  async function load(id = topicId) { try { const [a, b] = await Promise.all([id ? platformApi.getTopicPermissions(id) : Promise.resolve([]), platformApi.getUserTopicPermissions(activeUserId)]); setItems(a); setMine(b); } catch (e) { fail(e); } }
  useEffect(() => { void load(); }, [topicId, activeUserId]);
  async function grant(event: FormEvent) { event.preventDefault(); try { await platformApi.grantTopicPermissions(topicId, [userId], type); await load(topicId); success('Topic 권한을 부여했습니다.'); } catch (e) { fail(e); } }
  return <><div className="welcome-row"><div><h2>Topic 권한 관리</h2><p>Topic별 권한을 조회하고 새로운 사용자 권한을 부여합니다.</p></div></div><div className="connected-split"><section className="panel"><div className="panel-heading"><h3>Topic별 권한</h3><select value={topicId} onChange={(e) => setTopicId(Number(e.target.value))}>{topics.map((t) => <option key={t.topicId} value={t.topicId}>{t.topicName}</option>)}</select></div><PermissionTable items={items} /><form className="permission-form" onSubmit={grant}><select value={userId} onChange={(e) => setUserId(Number(e.target.value))}>{users.map((u) => <option key={u.userId} value={u.userId}>{u.name} (#{u.userId})</option>)}</select><select value={type} onChange={(e) => setType(e.target.value as TopicPermissionType)}>{(['VIEW', 'QUERY', 'DEPLOY', 'ADMIN'] as const).map((p) => <option key={p}>{p}</option>)}</select><button className="primary-button compact">권한 부여</button></form></section><section className="panel"><div className="panel-heading"><div><h3>내 사용 가능 Topic</h3><p>User #{activeUserId}</p></div></div><PermissionTable items={mine} /></section></div></>;
}
function PermissionTable({ items }: { items: TopicPermission[] }) { return items.length ? <div className="table-scroll"><table><thead><tr><th>Topic</th><th>User</th><th>권한</th></tr></thead><tbody>{items.map((p) => <tr key={p.permissionId}><td><strong>{p.topicName}</strong><small>#{p.topicId}</small></td><td>{p.userName}<small>#{p.userId}</small></td><td><span className="format-chip">{p.topicPermissionType}</span></td></tr>)}</tbody></table></div> : <Empty title="권한이 없습니다">조회된 Topic 권한이 없습니다.</Empty>; }

function Pipelines({ pipelines, create, open }: { pipelines: Pipeline[]; create: () => void; open: (id: number, type: PipelineType) => void }) { return <><div className="welcome-row"><div><h2>Pipeline 운영</h2><p>사용자 소유 Pipeline의 현재 상태를 조회합니다.</p></div><button className="primary-button compact" onClick={create}>＋ 새 Pipeline</button></div><section className="panel table-panel">{pipelines.length ? <div className="table-scroll"><table><thead><tr><th>Pipeline</th><th>Type</th><th>Status</th><th>Action</th></tr></thead><tbody>{pipelines.map((p) => <tr key={p.pipelineId}><td><strong>{p.pipelineName}</strong><small>#{p.pipelineId} · {p.description || '설명 없음'}</small></td><td><span className="format-chip">{p.pipelineType}</span></td><td><Status value={p.pipelineStatus} /></td><td><button className="row-action" onClick={() => open(p.pipelineId, p.pipelineType)}>상세 →</button></td></tr>)}</tbody></table></div> : <Empty title="Pipeline이 없습니다">새 Pipeline을 등록하세요.</Empty>}</section></>; }

function CreatePipeline({ topics, activeUserId, success, fail, refreshList, done }: { topics: Topic[]; activeUserId: number; success: (m: string) => void; fail: (e: unknown) => void; refreshList: () => Promise<void>; done: (id: number, type: PipelineType) => Promise<void> }) {
  const [type, setType] = useState<PipelineType>('CUSTOM_JAR');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [jarDraft, setJarDraft] = useState(customJarDraft);
  const [createdPipelineId, setCreatedPipelineId] = useState<number>();
  const [uploadedPipelineId, setUploadedPipelineId] = useState<number>();
  const [registrationError, setRegistrationError] = useState('');
  const inFlight = useRef(false);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy || inFlight.current || uploadedPipelineId) return;
    inFlight.current = true; setBusy(true); setRegistrationError('');
    let pipelineId = createdPipelineId;
    try {
      const result = await registerCustomJar({
        pipelineId, userId: activeUserId, draft: jarDraft,
        pipelineInput: { ownerUserId: activeUserId, pipelineName: name, description, pipelineType: 'CUSTOM_JAR' },
        onCreated: (id) => { pipelineId = id; setCreatedPipelineId(id); void refreshList().catch(fail); },
      });
      setUploadedPipelineId(result.pipelineId);
      success(result.alreadyRegistered ? '서버에서 JAR 등록 완료를 확인했습니다.' : 'Pipeline과 Custom JAR를 등록했습니다.');
      try {
        await done(result.pipelineId, 'CUSTOM_JAR');
      } catch (error) {
        setRegistrationError(`JAR 등록은 완료됐지만 상세 화면을 열지 못했습니다. 다시 상세 보기를 눌러 주세요. ${messageOf(error)}`);
      }
    } catch (error) {
      setRegistrationError(`${pipelineId ? `Pipeline #${pipelineId}는 생성되어 있습니다. 입력값을 확인한 뒤 JAR 등록을 다시 시도해 주세요. ` : ''}${messageOf(error)}`);
    } finally { inFlight.current = false; setBusy(false); }
  }

  return <>
    <div className="welcome-row"><div><h2>새 Pipeline 등록</h2><p>데이터와 처리 방식을 선택하고 Pipeline을 구성하세요.</p></div></div>
    <div className="panel pipeline-type-picker"><div className="type-tabs" aria-label="Pipeline 유형">
      <button type="button" aria-pressed={type === 'CUSTOM_JAR'} className={type === 'CUSTOM_JAR' ? 'active' : ''} onClick={() => setType('CUSTOM_JAR')} disabled={busy || Boolean(createdPipelineId)}><b>Custom JAR</b><span>직접 빌드한 Flink Job 등록</span></button>
      <button type="button" aria-pressed={type === 'AI_SQL'} className={type === 'AI_SQL' ? 'active' : ''} onClick={() => setType('AI_SQL')} disabled={busy || Boolean(createdPipelineId)}><b>AI SQL</b><span>자연어로 분석을 요청하고 SQL 검토</span></button>
    </div></div>
    {type === 'AI_SQL'
      ? <AiSqlRegistration topics={topics} userId={activeUserId} onCreated={(id) => done(id, 'AI_SQL')} notify={success} />
      : <form className="panel connected-create" onSubmit={submit}>
        {createdPipelineId && <p className="jar-registration-message" role="status">Pipeline #{createdPipelineId}의 기본 정보가 저장되었습니다. {uploadedPipelineId ? 'JAR 등록이 완료되었습니다.' : '현재 화면에서 JAR 등록을 이어갈 수 있습니다.'}</p>}
        <div className="connected-form">
          <Field label="Pipeline 이름"><input required readOnly={busy || Boolean(createdPipelineId)} value={name} onChange={(e) => setName(e.target.value)} /></Field>
          <Field label="설명"><input readOnly={busy || Boolean(createdPipelineId)} value={description} onChange={(e) => setDescription(e.target.value)} /></Field>
          <CustomJarFields draft={jarDraft} onChange={setJarDraft} topics={topics} disabled={busy || Boolean(uploadedPipelineId)} />
        </div>
        {registrationError && <p className="form-error jar-registration-message" role="alert">{registrationError}</p>}
        <div className="connected-actions">{uploadedPipelineId
          ? <button type="button" className="primary-button compact" disabled={busy} onClick={() => void done(uploadedPipelineId, 'CUSTOM_JAR').catch(fail)}>등록된 Pipeline 상세 보기 →</button>
          : <button className="primary-button compact" disabled={busy}>{busy ? '등록 중…' : createdPipelineId ? 'JAR 등록 다시 시도' : 'Pipeline 및 JAR 등록'}</button>}</div>
      </form>}
  </>;
}

function detailText(value: unknown, fallback = '등록 정보 없음'): string {
  return typeof value === 'string' && value.trim() ? value : typeof value === 'number' ? String(value) : fallback;
}

function topicNames(ids: number[], topics: Topic[]): string {
  return ids.length ? ids.map((id) => topics.find((topic) => topic.topicId === id)?.topicName || `Topic #${id}`).join(', ') : '선택 안 함';
}

function formattedJson(value: unknown): string {
  if (typeof value !== 'string') return JSON.stringify(value, null, 2);
  try { return JSON.stringify(JSON.parse(value), null, 2); } catch { return value; }
}

function PipelineTypeSummary({ type }: { type: PipelineType }) {
  return <div className="panel pipeline-type-picker detail-type-picker"><div className="type-tabs" aria-label="Pipeline 유형">
    <div className={`detail-type-option ${type === 'CUSTOM_JAR' ? 'active' : ''}`}><b>Custom JAR</b><span>직접 빌드한 Flink Job 등록</span></div>
    <div className={`detail-type-option ${type === 'AI_SQL' ? 'active' : ''}`}><b>AI SQL</b><span>자연어로 분석을 요청하고 SQL 검토</span></div>
  </div></div>;
}

function PipelineResponseSource({ item }: { item: PipelineDetailData }) {
  return <details className="pipeline-response-source"><summary>상세 API 응답 원본 보기</summary><pre>{JSON.stringify(item, null, 2)}</pre></details>;
}

function CustomJarRegisteredDetails({ item, topics, name, description, setName, setDescription, save, busy }: {
  item: PipelineDetailData; topics: Topic[]; name: string; description: string;
  setName: (value: string) => void; setDescription: (value: string) => void; save: (event: FormEvent) => void; busy: boolean;
}) {
  const detail = customJarDetail(item);
  const incomplete = canRegisterCustomJar(item);
  const registered = isCustomJarRegistered(item);
  return <form className="panel connected-create detail-registration" onSubmit={save}>
    <div className="panel-heading"><div><h3>{incomplete ? 'Pipeline 기본 정보' : 'Custom JAR 등록 정보'}</h3><p>{incomplete ? 'JAR 등록에 사용할 Pipeline의 기본 정보입니다.' : '등록 화면과 같은 순서로 저장된 설정을 확인합니다.'}</p></div><button className="secondary-button" disabled={busy}>기본 정보 저장</button></div>
    <div className="connected-form">
      <Field label="Pipeline 이름"><input value={name} onChange={(event) => setName(event.target.value)} required /></Field>
      <Field label="설명"><input value={description} onChange={(event) => setDescription(event.target.value)} /></Field>
      {!incomplete && <>
      <Field label="JAR 파일" wide hint={detail.storedFileName ? `서버 저장 파일: ${detailText(detail.storedFileName)}` : registered && !detail.originalFileName ? '등록은 완료되었지만 현재 조회 응답에는 파일명이 포함되어 있지 않습니다.' : '등록된 JAR 파일명'}><input value={detailText(detail.originalFileName, registered ? 'JAR 등록 완료 · 파일명 확인 불가' : '등록 정보 없음')} readOnly /></Field>
      <Field label="Entry Class"><input value={detailText(detail.entryClass)} readOnly /></Field>
      <Field label="Parallelism"><input value={detailText(detail.parallelism)} readOnly /></Field>
      <Field label="Input Topic"><input value={detail.inputTopicIds.length ? topicNames(detail.inputTopicIds, topics) : registered ? '입력 Topic 조회 정보 없음' : '선택 안 함'} readOnly /></Field>
      <Field label="Program Arguments" wide><textarea rows={5} value={programArgsText(detail.programArgs) || '등록 정보 없음'} readOnly /></Field>
      {detail.flinkJarId !== null && detail.flinkJarId !== undefined && <Field label="Flink JAR ID" wide><input value={detailText(detail.flinkJarId)} readOnly /></Field>}
      </>}
    </div>
  </form>;
}

function AiSqlRegisteredDetails({ item, topics }: { item: PipelineDetailData; topics: Topic[] }) {
  const detail = aiSqlDetail(item);
  const [topicDetail, setTopicDetail] = useState<Topic | null>(null);
  const listedTopic = topics.find((candidate) => candidate.topicId === detail.inputTopicId);
  const topic = topicDetail || listedTopic;
  useEffect(() => {
    if (!detail.inputTopicId || listedTopic?.schemaJson) return;
    let active = true;
    void platformApi.getTopic(detail.inputTopicId).then((value) => { if (active) setTopicDetail(value); }).catch(() => {});
    return () => { active = false; };
  }, [detail.inputTopicId, listedTopic?.schemaJson]);
  return <div className="ai-registration ai-detail">
    <section className="panel ai-section">
      <header><span>01</span><div><h3>기본 정보</h3><p>등록 시 입력한 이름과 입력 Topic입니다.</p></div><small className="detail-section-action">읽기 전용</small></header>
      <div className="ai-grid">
        <label>Pipeline 이름 <input value={item.pipelineName} readOnly /></label>
        <label>설명 <input value={item.description || ''} readOnly /></label>
      </div>
      <fieldset className="ai-topic-fieldset"><legend>입력 Topic <em>하나만 선택</em></legend>
        <div className="ai-topic-options"><label className="selected"><input type="radio" checked readOnly aria-label="등록된 입력 Topic" /><span><strong>{topic?.displayName || topic?.topicName || (detail.inputTopicId ? `Topic #${detail.inputTopicId}` : '등록 정보 없음')}</strong><small>{topic?.topicName || (detail.inputTopicId ? `Topic #${detail.inputTopicId}` : '입력 Topic 정보가 없습니다')}</small></span><i>{topic?.messageFormat || 'Schema'}</i></label></div>
      </fieldset>
      {topic?.schemaJson && <div className="ai-topic-schema"><div><strong>{topic.topicName}</strong><span>Topic Schema</span></div><details><summary>Schema 확인 <span>읽기 전용</span></summary><pre>{formattedJson(topic.schemaJson)}</pre></details></div>}
    </section>
    <section className="panel ai-section"><header><span>02</span><div><h3>처리 요청</h3><p>등록 시 사용한 자연어 요청입니다.</p></div></header><label className="ai-prompt">자연어 요청<textarea rows={5} value={detailText(detail.naturalLanguageRequest)} readOnly /></label></section>
    <section className="panel ai-preview" aria-label="저장된 Pipeline Plan 및 SQL"><div className="panel-heading"><div><h3>Plan · SQL 미리보기</h3><p>등록된 Pipeline Plan과 현재 저장된 Flink SQL입니다.</p></div></div><div className="ai-preview-grid"><article><h4>Pipeline Plan</h4>{isPipelinePlan(detail.pipelinePlan) ? <PlanPreview plan={detail.pipelinePlan} topics={topics} /> : detail.pipelinePlan ? <pre>{formattedJson(detail.pipelinePlan)}</pre> : <p className="detail-missing">저장된 Pipeline Plan이 없습니다.</p>}</article><article><h4>Flink SQL</h4>{typeof detail.generatedSql === 'string' && detail.generatedSql.trim() ? <pre>{detail.generatedSql}</pre> : <p className="detail-missing">저장된 Flink SQL이 없습니다.</p>}</article></div></section>
  </div>;
}

function PipelineDetail({ id, type, userId, topics, success, fail, refreshList, openResults }: { id: number; type: PipelineType; userId: number; topics: Topic[]; success: (m: string) => void; fail: (e: unknown) => void; refreshList: () => Promise<void>; openResults: (id: number) => void }) {
  const [item, setItem] = useState<PipelineDetailData | null>(null); const [name, setName] = useState(''); const [description, setDescription] = useState(''); const [busy, setBusy] = useState(false); const [loadError, setLoadError] = useState(false);
  const stop = usePipelineStop({ id, status: item?.pipelineStatus, onStatusUpdated: (pipelineStatus) => setItem((current) => current ? { ...current, pipelineStatus } : current), refreshList });
  const actionBusy = busy || stop.pending || stop.phase === 'confirm';
  async function load() { setBusy(true); setLoadError(false); try { const value = await platformApi.getPipeline(id, type); if (!value || value.pipelineId !== id || value.pipelineType !== type) throw new Error('Pipeline 상세 응답의 ID 또는 유형이 요청과 다릅니다.'); setItem(value); setName(value.pipelineName); setDescription(value.description || ''); return value; } catch (e) { setLoadError(true); fail(e); return null; } finally { setBusy(false); } }
  useEffect(() => { void load(); }, [id, type]);
  async function save(event: FormEvent) { event.preventDefault(); if (actionBusy || !item || item.pipelineType !== 'CUSTOM_JAR') return; setBusy(true); try { await platformApi.updatePipeline({ pipelineId: item.pipelineId, ownerUserId: item.ownerUserId, pipelineName: name, description, pipelineType: item.pipelineType }); await Promise.all([load(), refreshList()]); success('Pipeline 정보를 수정했습니다.'); } catch (e) { fail(e); } finally { setBusy(false); } }
  async function deploy() { if (actionBusy || !item) return; setBusy(true); try { const result = item.pipelineType === 'AI_SQL' ? await platformApi.deployAiSqlPipeline(item.pipelineId) : await platformApi.deployPipeline(item.pipelineId); success(`Deployment #${result.deploymentId} · ${result.status}`); await Promise.all([load(), refreshList()]); } catch (e) { fail(e); } finally { setBusy(false); } }
  if (!item) return <section className="panel connected-unavailable"><span>API</span><h2>{loadError ? 'Pipeline 상세조회에 실패했습니다' : 'Pipeline 조회 중'}</h2><p>{loadError ? '유형별 상세 API 연결 상태를 확인한 후 다시 시도해 주세요.' : '상세 API 응답을 기다리고 있습니다.'}</p>{loadError && <button className="secondary-button" type="button" onClick={() => void load()}>다시 조회</button>}</section>;
  const deployable = item.pipelineType === 'AI_SQL'
    ? ['DRAFT', 'CREATED'].includes(item.pipelineStatus)
    : ['ARTIFACT_UPLOADED', 'STOPPED', 'FAILED'].includes(item.pipelineStatus) && isCustomJarRegistered(item);
  const deployLabel = busy ? '처리 중…' : item.pipelineType === 'AI_SQL' ? 'AI SQL 배포' : '배포 실행';
  return <><div className="welcome-row"><div><h2>{item.pipelineName}</h2><p>Pipeline #{item.pipelineId} · Owner #{item.ownerUserId}</p></div><div className="connected-heading-actions"><Status value={item.pipelineStatus} /><button className="secondary-button" onClick={() => void load()} disabled={actionBusy}>상태 조회</button><button className="secondary-button" onClick={() => openResults(id)}>결과 Dashboard →</button><button className="primary-button compact" onClick={() => void deploy()} disabled={!deployable || actionBusy}>{deployLabel}</button><button className="secondary-button pipeline-stop-button" onClick={stop.confirm} disabled={actionBusy || !canStopPipeline(item.pipelineStatus)} title="실행 중(RUNNING)인 Pipeline을 중지합니다">{stop.pending ? '중지 처리 중…' : '배포 중지'}</button></div></div>
    {stop.phase === 'confirm' && <section className="pipeline-stop-message" aria-label="Pipeline 배포 중지 확인"><h3>배포를 중지할까요?</h3><p><strong>{item.pipelineName}</strong>의 실행 중인 Flink Job과 실시간 데이터 처리가 중지됩니다.</p><div><button className="secondary-button" onClick={stop.cancel}>취소</button><button className="secondary-button pipeline-stop-button" onClick={() => void stop.stop()}>중지 확인</button></div></section>}
    {(stop.notice || stop.error || stop.pending) && <section className="pipeline-stop-message" aria-label="Pipeline 중지 상태">{stop.notice && <p role="status">{stop.notice}</p>}{stop.error && <p className="form-error" role="alert">{stop.error}</p>}{stop.pending && <><p>서버에서 확인한 현재 상태: {item.pipelineStatus}</p><button className="secondary-button" onClick={stop.refresh} disabled={stop.checking || stop.phase === 'submitting'}>{stop.checking ? '상태 조회 중…' : '상태 다시 조회'}</button></>}</section>}
    <PipelineTypeSummary type={type} />{type === 'AI_SQL'
    ? <AiSqlRegisteredDetails item={item} topics={topics} />
    : <><CustomJarRegisteredDetails item={item} topics={topics} name={name} description={description} setName={setName} setDescription={setDescription} save={save} busy={actionBusy} />{canRegisterCustomJar(item) && <CustomJarRecovery item={item} topics={topics} userId={userId} busy={actionBusy} onBusyChange={setBusy} onUploaded={async () => { const [updated] = await Promise.all([load(), refreshList()]); if (!updated) throw new Error('JAR 등록은 완료됐지만 상세 조회에 실패했습니다. 등록 상태를 다시 조회해 주세요.'); success('JAR 등록이 완료되었습니다. Pipeline 상태를 확인해 주세요.'); }} />}</>}
    <PipelineResponseSource item={item} />
    <section className="connected-availability"><h3>운영 기능 연결 상태</h3><p><b>연결됨</b> 유형별 상세 조회, 상태 조회, CUSTOM_JAR 기본 정보 수정, 유형별 배포·중지 및 결과 Dashboard</p><p><span>API 필요</span> Deployment 이력, 실패 분석</p></section></>;
}

function Unavailable({ title, children }: { title: string; children: ReactNode }) { return <section className="panel connected-unavailable"><span>API</span><h2>{title}</h2><p>{children}</p></section>; }

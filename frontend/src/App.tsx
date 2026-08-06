import { FormEvent, useEffect, useState, type ReactNode } from 'react';
import { api, ApiError, unwrap } from './api/client';
import { useAuth } from './auth/AuthContext';
import { demoPipelines, demoTopics, isDemoMode } from './api/demo';
import { PipelineCreateView, type PipelineDraft } from './PipelineCreateView';

type Topic = { topicId: number; topicName: string; displayName?: string; description?: string; messageFormat?: string };
type Pipeline = { pipelineId: number; pipelineName: string; description?: string; pipelineType?: string; pipelineStatus?: string };
type View = 'overview' | 'topics' | 'pipelines' | 'pipeline-create';

const navItems: { id: View; label: string; icon: string }[] = [
  { id: 'overview', label: '개요', icon: 'grid' },
  { id: 'topics', label: '토픽', icon: 'database' },
  { id: 'pipelines', label: '파이프라인', icon: 'flow' },
];

export function App() {
  const { user, isLoading } = useAuth();
  if (isLoading) return <LoadingScreen />;
  return user ? <Console /> : <LoginScreen />;
}

function LoadingScreen() {
  return <main className="loading-screen"><Brand /><span className="spinner" aria-label="세션을 확인하는 중" /></main>;
}

function LoginScreen() {
  const { signIn } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      // Spring Security decides the lifetime of its session/refresh cookie. Bearer tokens stay in memory.
      await signIn(username, password, remember);
    } catch (cause) {
      setError(cause instanceof ApiError && cause.status === 401
        ? '아이디 또는 비밀번호를 다시 확인해 주세요.'
        : '로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-art" aria-label="StreamCell 소개">
        <Brand inverse />
        <div className="art-copy">
          <p className="eyebrow">REAL-TIME DATA PLATFORM</p>
          <h1>데이터의 흐름을<br /><em>한눈에.</em></h1>
          <p>Kafka부터 Flink까지, 복잡한 실시간 데이터 파이프라인을 더 빠르고 안정적으로 운영하세요.</p>
        </div>
        <div className="stream-diagram" aria-hidden="true">
          <div className="stream-node source"><span>Kafka</span><small>Orders</small></div>
          <i className="stream-line first" /><i className="stream-dot one" />
          <div className="stream-node process"><span>Flink</span><small>Transform</small></div>
          <i className="stream-line second" /><i className="stream-dot two" />
          <div className="stream-node sink"><span>Live</span><small>Insights</small></div>
        </div>
        <p className="auth-footer">© 2026 StreamCell. Built for the flow.</p>
      </section>

      <section className="auth-panel">
        <div className="login-card">
          <div className="mobile-brand"><Brand /></div>
          <div className="login-heading"><p className="eyebrow">WELCOME BACK</p><h2>다시 만나서 반가워요.</h2><p>StreamCell 워크스페이스로 계속하세요.</p></div>
          {isDemoMode && <p className="demo-login-hint">데모 모드 · 이메일과 비밀번호를 자유롭게 입력해 로그인해 보세요.</p>}
          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="username">아이디 또는 이메일</label>
            <div className="field"><Icon name="user" /><input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" placeholder="name@company.com" required /></div>
            <div className="label-row"><label htmlFor="password">비밀번호</label><button type="button" className="text-button">비밀번호 찾기</button></div>
            <div className="field"><Icon name="lock" /><input id="password" type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" placeholder="비밀번호를 입력하세요" required /><button className="icon-button" type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}><Icon name={showPassword ? 'eyeOff' : 'eye'} /></button></div>
            {error && <p className="form-error" role="alert"><Icon name="alert" />{error}</p>}
            <label className="check-label"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /><span>로그인 상태 유지</span></label>
            <button className="primary-button" disabled={isSubmitting}>{isSubmitting ? '로그인 중…' : <>로그인 <Icon name="arrow" /></>}</button>
          </form>
          <p className="support-copy">계정이 없으신가요? <a href="mailto:admin@streamcell.io">관리자에게 문의하기</a></p>
        </div>
      </section>
    </main>
  );
}

function Console() {
  const { user, signOut } = useAuth();
  if (!user) return null;
  const currentUser = user;
  const [view, setView] = useState<View>('overview');
  const [topics, setTopics] = useState<Topic[]>(isDemoMode ? demoTopics : []);
  const [pipelines, setPipelines] = useState<Pipeline[]>(isDemoMode ? demoPipelines : []);
  const [notice, setNotice] = useState('');
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    if (isDemoMode) return;
    if (!user.userId) return;
    Promise.all([
      api<Topic[] | { data: Topic[] }>('/api/v1/platform/topic/topics').then(unwrap),
      api<Pipeline[] | { data: Pipeline[] }>(`/api/v1/web/my/pipeline/pipelines?userId=${user.userId}`).then(unwrap),
    ]).then(([nextTopics, nextPipelines]) => {
      setTopics(nextTopics ?? []); setPipelines(nextPipelines ?? []);
    }).catch(() => setLoadError(true));
  }, [user?.userId]);

  const activePipelines = pipelines.filter((item) => item.pipelineStatus === 'RUNNING' || item.pipelineStatus === 'DEPLOYED').length;
  const pageTitle = view === 'pipeline-create' ? '새 파이프라인' : navItems.find((item) => item.id === view)?.label ?? '개요';

  async function createPipeline(draft: PipelineDraft) {
    let created: Pipeline;
    if (isDemoMode) {
      created = { pipelineId: Date.now(), pipelineName: draft.pipelineName, description: draft.description, pipelineType: draft.pipelineType, pipelineStatus: 'DRAFT' };
    } else {
      created = unwrap(await api<Pipeline | { data: Pipeline }>('/api/v1/platform/pipeline/pipelines', {
        method: 'POST',
        body: JSON.stringify({ ownerUserId: currentUser.userId, pipelineName: draft.pipelineName, description: draft.description, pipelineType: draft.pipelineType }),
      }));
    }
    setPipelines((current) => [created, ...current]);
    setNotice(isDemoMode ? '데모 파이프라인이 등록되었습니다.' : '파이프라인이 등록되었습니다.');
    setView('pipelines');
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <Brand inverse />
      <nav aria-label="주 메뉴">{navItems.map((item) => <button key={item.id} className={`nav-item ${view === item.id ? 'active' : ''}`} onClick={() => setView(item.id)}><Icon name={item.icon} /><span>{item.label}</span></button>)}</nav>
      <div className="sidebar-bottom"><div className="help-card"><Icon name="spark" /><p>도움이 필요하신가요?</p><a href="mailto:support@streamcell.io">지원 센터</a></div><button className="account-button"><span className="avatar">{(user.displayName ?? user.username).slice(0, 1).toUpperCase()}</span><span><strong>{user.displayName ?? user.username}</strong><small>{user.roles[0]?.replace('ROLE_', '') || 'MEMBER'}</small></span><Icon name="dots" /></button></div>
    </aside>
    <main className="workspace">
      <header className="topbar"><div><p className="breadcrumb">워크스페이스 <span>/</span> {pageTitle}</p><h1>{pageTitle}</h1></div><div className="top-actions"><button className="round-button" aria-label="알림"><Icon name="bell" /><b /></button><button className="signout" onClick={() => void signOut()}>로그아웃</button></div></header>
      {isDemoMode && <div className="demo-banner"><Icon name="spark" />데모 모드입니다. 현재 표시되는 데이터는 샘플이며, 실제 API를 호출하지 않습니다.</div>}
      {notice && <div className="toast" role="status">{notice}</div>}
      {loadError && <div className="api-notice"><Icon name="alert" /><span>운영 데이터를 불러오지 못했습니다. 로그인 후 API 권한과 엔드포인트를 확인해 주세요.</span></div>}
      {view === 'overview' && <Overview topics={topics} pipelines={pipelines} activePipelines={activePipelines} setView={setView} />}
      {view === 'topics' && <TopicsView topics={topics} onSync={async () => { if (!isDemoMode) await api('/api/v1/platform/topic/sync', { method: 'POST' }); setNotice(isDemoMode ? '데모 토픽은 이미 최신 상태입니다.' : '토픽 동기화가 시작되었습니다.'); }} />}
      {view === 'pipelines' && <PipelinesView pipelines={pipelines} onCreate={() => setView('pipeline-create')} />}
      {view === 'pipeline-create' && <PipelineCreateView topics={topics} onCancel={() => setView('pipelines')} onCreate={createPipeline} />}
    </main>
  </div>;
}

function Overview({ topics, pipelines, activePipelines, setView }: { topics: Topic[]; pipelines: Pipeline[]; activePipelines: number; setView: (view: View) => void }) {
  const cards = [
    { label: '연결된 토픽', value: topics.length, hint: 'Kafka topic', icon: 'database', tone: 'blue' },
    { label: '실행 중 파이프라인', value: activePipelines, hint: `${pipelines.length}개 전체 파이프라인`, icon: 'activity', tone: 'green' },
    { label: '검토 필요', value: pipelines.filter((item) => item.pipelineStatus === 'FAILED').length, hint: '실패한 작업', icon: 'alert', tone: 'amber' },
  ];
  return <section className="page-content"><div className="welcome-row"><div><h2>좋은 하루예요, 데이터가 흐르고 있습니다.</h2><p>StreamCell 워크스페이스의 현재 상태를 확인하세요.</p></div><button className="primary-button compact" onClick={() => setView('pipeline-create')}>새 파이프라인 <Icon name="plus" /></button></div><div className="stats-grid">{cards.map((card) => <article className="stat-card" key={card.label}><div className={`stat-icon ${card.tone}`}><Icon name={card.icon} /></div><p>{card.label}</p><strong>{card.value}</strong><small>{card.hint}</small></article>)}</div><div className="dashboard-grid"><section className="panel activity-panel"><div className="panel-heading"><div><h3>파이프라인 현황</h3><p>최근 등록된 작업</p></div><button className="text-button" onClick={() => setView('pipelines')}>전체 보기 <Icon name="arrow" /></button></div>{pipelines.length ? <div className="pipeline-list">{pipelines.slice(0, 4).map((pipeline) => <PipelineRow pipeline={pipeline} key={pipeline.pipelineId} />)}</div> : <EmptyState icon="flow" title="아직 파이프라인이 없습니다" text="첫 파이프라인을 만들고 데이터 흐름을 시작해 보세요." action="파이프라인 만들기" onAction={() => setView('pipeline-create')} />}</section><section className="panel flow-panel"><div className="panel-heading"><div><h3>데이터 흐름</h3><p>워크스페이스 리소스</p></div><span className="live-pill"><i /> LIVE</span></div><div className="flow-visual"><div><b>{topics.length}</b><span>Topics</span></div><i /><div><b>{pipelines.length}</b><span>Pipelines</span></div><i /><div><b>{activePipelines}</b><span>Running</span></div></div><p className="flow-caption">Kafka 토픽에서 파이프라인까지 연결 상태를 관리합니다.</p></section></div></section>;
}

function TopicsView({ topics, onSync }: { topics: Topic[]; onSync: () => Promise<void> }) {
  const [isSyncing, setIsSyncing] = useState(false);
  async function sync() { setIsSyncing(true); try { await onSync(); } finally { setIsSyncing(false); } }
  return <section className="page-content"><div className="welcome-row"><div><h2>Kafka 토픽</h2><p>파이프라인에서 사용할 데이터 소스를 관리하세요.</p></div><button className="secondary-button" disabled={isSyncing} onClick={() => void sync()}><Icon name="refresh" />{isSyncing ? '동기화 중…' : '토픽 동기화'}</button></div><section className="panel table-panel">{topics.length ? <table><thead><tr><th>토픽</th><th>형식</th><th>설명</th><th /></tr></thead><tbody>{topics.map((topic) => <tr key={topic.topicId}><td><strong>{topic.displayName ?? topic.topicName}</strong><small>{topic.topicName}</small></td><td><span className="format-chip">{topic.messageFormat ?? 'JSON'}</span></td><td>{topic.description ?? '설명 없음'}</td><td><button className="row-action">상세 <Icon name="chevron" /></button></td></tr>)}</tbody></table> : <EmptyState icon="database" title="표시할 토픽이 없습니다" text="Kafka에서 토픽을 가져오려면 동기화를 실행하세요." action="토픽 동기화" onAction={() => void sync()} />}</section></section>;
}

function PipelinesView({ pipelines, onCreate }: { pipelines: Pipeline[]; onCreate: () => void }) {
  return <section className="page-content"><div className="welcome-row"><div><h2>파이프라인</h2><p>실시간 데이터 처리 작업을 설계하고 운영하세요.</p></div><button className="primary-button compact" onClick={onCreate}><Icon name="plus" />새 파이프라인</button></div><section className="panel table-panel">{pipelines.length ? <table><thead><tr><th>파이프라인</th><th>유형</th><th>상태</th><th /></tr></thead><tbody>{pipelines.map((pipeline) => <tr key={pipeline.pipelineId}><td><strong>{pipeline.pipelineName}</strong><small>{pipeline.description ?? '설명 없음'}</small></td><td>{pipeline.pipelineType ?? '—'}</td><td><Status status={pipeline.pipelineStatus} /></td><td><button className="row-action">열기 <Icon name="chevron" /></button></td></tr>)}</tbody></table> : <EmptyState icon="flow" title="아직 파이프라인이 없습니다" text="SQL 또는 Custom Job 기반의 새 처리 흐름을 만드세요." action="새 파이프라인" onAction={onCreate} />}</section></section>;
}

function PipelineRow({ pipeline }: { pipeline: Pipeline }) { return <div className="pipeline-row"><span className="pipeline-mark"><Icon name="flow" /></span><div><strong>{pipeline.pipelineName}</strong><small>{pipeline.pipelineType ?? 'Pipeline'} · {pipeline.description ?? '설명 없음'}</small></div><Status status={pipeline.pipelineStatus} /><button className="row-action" aria-label={`${pipeline.pipelineName} 열기`}><Icon name="chevron" /></button></div>; }
function Status({ status }: { status?: string }) { const text = status === 'RUNNING' || status === 'DEPLOYED' ? '실행 중' : status === 'FAILED' ? '실패' : status === 'STOPPED' ? '중지됨' : status ?? '초안'; return <span className={`status ${text === '실행 중' ? 'running' : text === '실패' ? 'failed' : ''}`}><i />{text}</span>; }
function EmptyState({ icon, title, text, action, onAction }: { icon: string; title: string; text: string; action?: string; onAction?: () => void }) { return <div className="empty-state"><span><Icon name={icon} /></span><h3>{title}</h3><p>{text}</p>{action && <button className="secondary-button" onClick={onAction}>{action}</button>}</div>; }
function Brand({ inverse = false }: { inverse?: boolean }) { return <div className={`brand ${inverse ? 'inverse' : ''}`}><span className="brand-symbol"><i /><i /><i /></span><strong>streamcell</strong></div>; }

function Icon({ name }: { name: string }) {
  const paths: Record<string, ReactNode> = {
    user: <><circle cx="12" cy="8" r="3" /><path d="M5 20c.8-3.2 3.1-5 7-5s6.2 1.8 7 5" /></>, lock: <><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" /></>, eye: <><path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2" /></>, eyeOff: <><path d="m3 3 18 18M10.6 6.2A10.8 10.8 0 0 1 12 6c6.3 0 9.5 6 9.5 6a17.7 17.7 0 0 1-3 3.5M6.1 6.2C3.7 7.7 2.5 10 2.5 12c0 0 3.2 6 9.5 6 1.2 0 2.3-.2 3.3-.6" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></>, arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>, alert: <><path d="M12 3 2.8 20h18.4L12 3Z" /><path d="M12 9v4M12 17h.01" /></>, grid: <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="4" y="14" width="6" height="6" rx="1" /><rect x="14" y="14" width="6" height="6" rx="1" /></>, database: <><ellipse cx="12" cy="5" rx="7" ry="3" /><path d="M5 5v7c0 1.7 3.1 3 7 3s7-1.3 7-3V5M5 12v7c0 1.7 3.1 3 7 3s7-1.3 7-3v-7" /></>, flow: <><circle cx="5" cy="6" r="2" /><circle cx="19" cy="6" r="2" /><circle cx="12" cy="18" r="2" /><path d="M7 7.3 10.5 16M17 7.3 13.5 16" /></>, activity: <path d="M3 12h4l2-5 4 10 2-5h6" />, spark: <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Zm6 13 .7 2.3L21 19l-2.3.7L18 22l-.7-2.3L15 19l2.3-.7L18 16Z" />, dots: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>, bell: <><path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 22h4" /></>, plus: <path d="M12 5v14M5 12h14" />, refresh: <><path d="M20 11a8 8 0 0 0-14.5-4.7L3 9" /><path d="M3 4v5h5M4 13a8 8 0 0 0 14.5 4.7L21 15" /><path d="M21 20v-5h-5" /></>, chevron: <path d="m9 18 6-6-6-6" />,
  };
  return <svg className={`icon icon-${name}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

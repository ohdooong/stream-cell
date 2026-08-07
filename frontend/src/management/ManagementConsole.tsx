import { FormEvent, useMemo, useState, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { PipelineBuilder } from './PipelineBuilder';
import {
  chartValues,
  clusterSummary,
  deployments,
  failures,
  initialPermissions,
  initialPipelines,
  initialTopics,
  resultRows,
  taskManagers,
  type PermissionItem,
  type PipelineItem,
  type TopicItem,
} from './demoData';
import './management.css';

type View = 'cluster' | 'topics' | 'permissions' | 'pipelines' | 'pipeline-builder' | 'pipeline-detail' | 'results' | 'failures';

const navGroups: { label: string; items: { id: View; label: string; icon: string }[] }[] = [
  { label: 'PLATFORM', items: [
    { id: 'cluster', label: 'Cluster Dashboard', icon: 'cluster' },
    { id: 'topics', label: 'Topic 관리', icon: 'database' },
    { id: 'permissions', label: 'Topic 권한', icon: 'shield' },
  ] },
  { label: 'PIPELINE', items: [
    { id: 'pipelines', label: 'Pipeline 운영', icon: 'flow' },
    { id: 'results', label: '결과 Dashboard', icon: 'chart' },
    { id: 'failures', label: '실패 분석', icon: 'alert' },
  ] },
];

const pageNames: Record<View, string> = {
  cluster: 'Flink Cluster Dashboard', topics: 'Topic 관리', permissions: 'Topic 권한 관리', pipelines: 'Pipeline 운영',
  'pipeline-builder': '새 Pipeline', 'pipeline-detail': 'Pipeline 상세', results: '결과 Dashboard', failures: '실패 분석',
};

export function ManagementConsole() {
  const { user, signOut } = useAuth();
  const [view, setView] = useState<View>('cluster');
  const [topics, setTopics] = useState(initialTopics);
  const [pipelines, setPipelines] = useState(initialPipelines);
  const [permissions, setPermissions] = useState(initialPermissions);
  const [selectedPipelineId, setSelectedPipelineId] = useState(1);
  const [toast, setToast] = useState('');

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(''), 2600);
  }

  function navigate(nextView: View) {
    setView(nextView);
    setToast('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function createPipeline(pipeline: PipelineItem) {
    setPipelines((current) => [pipeline, ...current]);
    setSelectedPipelineId(pipeline.id);
    setView('pipeline-detail');
    notify(`${pipeline.name}이(가) 초안으로 등록되었습니다.`);
  }

  function updatePipelineStatus(id: number, status: PipelineItem['status']) {
    setPipelines((current) => current.map((pipeline) => pipeline.id === id ? { ...pipeline, status, updatedAt: '방금 전', jobId: pipeline.jobId || `demo-${Date.now().toString(16)}` } : pipeline));
    notify(status === 'RUNNING' ? 'Pipeline 배포가 완료되어 실행 중입니다.' : 'Pipeline이 안전하게 중지되었습니다.');
  }

  const selectedPipeline = pipelines.find((pipeline) => pipeline.id === selectedPipelineId) ?? pipelines[0];
  const activeNav = view === 'pipeline-builder' || view === 'pipeline-detail' ? 'pipelines' : view;

  return <div className="mc-shell">
    <aside className="mc-sidebar">
      <Logo />
      <div className="mc-environment"><i /> DEMO WORKSPACE <span>DEV</span></div>
      <nav>{navGroups.map((group) => <div className="mc-nav-group" key={group.label}><p>{group.label}</p>{group.items.map((item) => <button className={activeNav === item.id ? 'active' : ''} onClick={() => navigate(item.id)} key={item.id}><MIcon name={item.icon} /><span>{item.label}</span>{item.id === 'failures' && <b>1</b>}</button>)}</div>)}</nav>
      <div className="mc-sidebar-bottom"><div className="mc-cluster-mini"><span><i /> FLINK CLUSTER</span><strong>Healthy</strong><small>3 TaskManagers · 7/12 Slots</small></div><button className="mc-profile"><span className="mc-avatar">{(user?.displayName ?? '데').slice(0, 1)}</span><span><strong>{user?.displayName ?? '데모 사용자'}</strong><small>ADMIN</small></span><MIcon name="dots" /></button></div>
    </aside>
    <main className="mc-workspace">
      <header className="mc-topbar"><div><p>StreamCell <span>/</span> {pageNames[view]}</p><h1>{pageNames[view]}</h1></div><div className="mc-top-actions"><span className="mc-demo-pill">✦ DEMO MODE</span><button className="mc-icon-button" aria-label="알림"><MIcon name="bell" /><i /></button><button className="mc-logout" onClick={() => void signOut()}>로그아웃</button></div></header>
      {toast && <div className="mc-toast"><span>✓</span>{toast}</div>}
      {view === 'cluster' && <ClusterDashboard pipelines={pipelines} onOpenPipeline={(id) => { setSelectedPipelineId(id); navigate('pipeline-detail'); }} />}
      {view === 'topics' && <TopicsView topics={topics} onSync={() => notify('Kafka Topic 동기화가 완료되었습니다.')} onSave={(next) => { setTopics((current) => current.map((topic) => topic.id === next.id ? next : topic)); notify('Topic Schema와 Event Time 설정을 저장했습니다.'); }} />}
      {view === 'permissions' && <PermissionsView topics={topics} permissions={permissions} onGrant={(permission) => { setPermissions((current) => [...current, permission]); notify('새 Topic 권한을 부여했습니다.'); }} />}
      {view === 'pipelines' && <PipelinesView pipelines={pipelines} onCreate={() => navigate('pipeline-builder')} onOpen={(id) => { setSelectedPipelineId(id); navigate('pipeline-detail'); }} />}
      {view === 'pipeline-builder' && <PipelineBuilder topics={topics} onCancel={() => navigate('pipelines')} onCreate={createPipeline} />}
      {view === 'pipeline-detail' && selectedPipeline && <PipelineDetail pipeline={selectedPipeline} onBack={() => navigate('pipelines')} onStatusChange={(status) => updatePipelineStatus(selectedPipeline.id, status)} onResults={() => navigate('results')} onFailures={() => navigate('failures')} />}
      {view === 'results' && <ResultsDashboard pipelines={pipelines} />}
      {view === 'failures' && <FailuresView onOpenPipeline={(id) => { setSelectedPipelineId(id); navigate('pipeline-detail'); }} />}
    </main>
  </div>;
}

function ClusterDashboard({ pipelines, onOpenPipeline }: { pipelines: PipelineItem[]; onOpenPipeline: (id: number) => void }) {
  const running = pipelines.filter((pipeline) => pipeline.status === 'RUNNING');
  return <div className="mc-page">
    <div className="mc-page-intro"><div><span className="mc-live"><i /> LIVE</span><h2>Streaming Platform 상태</h2><p>Flink Cluster와 실행 중인 Job의 현재 상태를 확인하세요.</p></div><button className="mc-btn secondary"><MIcon name="refresh" /> 새로고침</button></div>
    <div className="mc-cluster-hero"><div className="mc-health-ring"><span><i>✓</i></span><div><small>CLUSTER STATUS</small><strong>Healthy</strong><p>모든 컴포넌트가 정상입니다.</p></div></div><div className="mc-cluster-meta"><span><small>FLINK VERSION</small><b>{clusterSummary.version}</b></span><span><small>JOB MANAGER</small><b>{clusterSummary.jobManager}</b></span><span><small>UPTIME</small><b>{clusterSummary.uptime}</b></span></div></div>
    <div className="mc-metric-grid four"><Metric icon="server" label="TaskManagers" value={clusterSummary.taskManagers} meta="모두 연결됨" tone="blue" /><Metric icon="slots" label="Available Slots" value={clusterSummary.slotsAvailable} suffix={`/ ${clusterSummary.slotsTotal}`} meta={`${clusterSummary.slotsTotal - clusterSummary.slotsAvailable}개 사용 중`} tone="cyan" /><Metric icon="play" label="Running Jobs" value={running.length} meta="실시간 처리 중" tone="green" /><Metric icon="alert" label="Failed Jobs" value={clusterSummary.jobsFailed} meta="최근 24시간" tone="red" /></div>
    <div className="mc-two-column cluster"><section className="mc-panel"><div className="mc-panel-title"><div><h3>실행 중인 Job</h3><p>Flink Cluster에서 현재 실행 중인 Streaming Job</p></div><span className="mc-count">{running.length} Running</span></div><div className="mc-job-list">{running.map((pipeline, index) => <button onClick={() => onOpenPipeline(pipeline.id)} key={pipeline.id}><span className="mc-job-icon"><MIcon name="activity" /></span><span><strong>{pipeline.name}</strong><small>{pipeline.jobId} · {pipeline.type}</small></span><span><small>UPTIME</small><b>{index === 0 ? '5h 18m' : '6h 27m'}</b></span><span><small>RECORDS / SEC</small><b>{index === 0 ? '2,842' : '2,126'}</b></span><Status status="RUNNING" /><MIcon name="chevron" /></button>)}</div></section><section className="mc-panel"><div className="mc-panel-title"><div><h3>Slot 사용률</h3><p>TaskManager 전체 Slot 분포</p></div><b className="mc-big-percent">58%</b></div><div className="mc-slot-chart"><div className="mc-donut"><span><b>7</b><small>Used</small></span></div><div><p><i className="used" />사용 중 <b>7 Slots</b></p><p><i className="available" />사용 가능 <b>5 Slots</b></p><p><i className="total" />전체 <b>12 Slots</b></p></div></div></section></div>
    <section className="mc-panel mc-table-panel"><div className="mc-panel-title"><div><h3>TaskManagers</h3><p>Cluster에 연결된 Worker Node 상태</p></div></div><table><thead><tr><th>ID / Host</th><th>Slots</th><th>CPU</th><th>Memory</th><th>Last Heartbeat</th><th>Status</th></tr></thead><tbody>{taskManagers.map((tm) => <tr key={tm.id}><td><strong>{tm.id}</strong><small>{tm.host}</small></td><td>{tm.slots}</td><td><Bar value={tm.cpu} /></td><td><Bar value={tm.memory} /></td><td>{tm.heartbeat}</td><td><span className="mc-online"><i /> Online</span></td></tr>)}</tbody></table></section>
  </div>;
}

function TopicsView({ topics, onSync, onSave }: { topics: TopicItem[]; onSync: () => void; onSave: (topic: TopicItem) => void }) {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState(topics[0]?.id);
  const selected = topics.find((topic) => topic.id === selectedId) ?? topics[0];
  const [schema, setSchema] = useState(selected?.schema ?? '');
  const [eventTime, setEventTime] = useState(selected?.eventTimeField ?? '');
  const filtered = topics.filter((topic) => `${topic.name} ${topic.displayName}`.toLowerCase().includes(query.toLowerCase()));

  function select(topic: TopicItem) { setSelectedId(topic.id); setSchema(topic.schema); setEventTime(topic.eventTimeField); }
  return <div className="mc-page"><div className="mc-page-intro"><div><h2>Kafka Topic</h2><p>Topic 목록을 동기화하고 Schema와 Event Time을 관리하세요.</p></div><button className="mc-btn primary" onClick={onSync}><MIcon name="refresh" /> Topic Sync</button></div><div className="mc-topic-layout"><section className="mc-panel mc-topic-list"><div className="mc-list-tools"><label><MIcon name="search" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Topic 검색" /></label><span>{filtered.length} Topics</span></div>{filtered.map((topic) => <button className={selected?.id === topic.id ? 'active' : ''} onClick={() => select(topic)} key={topic.id}><span className="mc-topic-icon"><MIcon name="database" /></span><span><strong>{topic.displayName}</strong><small>{topic.name}</small></span><span><b>{topic.throughput}</b><small>{topic.partitions} partitions</small></span><MIcon name="chevron" /></button>)}</section>{selected && <section className="mc-panel mc-topic-detail"><div className="mc-detail-head"><div><span className="mc-topic-icon"><MIcon name="database" /></span><div><h3>{selected.displayName}</h3><p>{selected.name}</p></div></div><span className="mc-format">{selected.format}</span></div><p className="mc-description">{selected.description}</p><div className="mc-topic-facts"><span><small>PARTITIONS</small><b>{selected.partitions}</b></span><span><small>RETENTION</small><b>{selected.retention}</b></span><span><small>THROUGHPUT</small><b>{selected.throughput}</b></span></div><div className="mc-setting-title"><div><h4>Schema & Event Time</h4><p>Flink SQL이 이벤트 구조와 시간을 해석하는 기준입니다.</p></div><span>Schema Registry</span></div><label className="mc-schema-editor">Topic Schema<textarea value={schema} onChange={(event) => setSchema(event.target.value)} spellCheck={false} rows={15} /></label><div className="mc-event-time"><label>Event Time Field<input value={eventTime} onChange={(event) => setEventTime(event.target.value)} /></label><div><MIcon name="clock" /><span><strong>Watermark Strategy</strong><small>Bounded out-of-orderness · 5 seconds</small></span></div></div><div className="mc-detail-actions"><button className="mc-btn secondary" onClick={() => { setSchema(selected.schema); setEventTime(selected.eventTimeField); }}>초기화</button><button className="mc-btn primary" onClick={() => onSave({ ...selected, schema, eventTimeField: eventTime })}>설정 저장</button></div></section>}</div></div>;
}

function PermissionsView({ topics, permissions, onGrant }: { topics: TopicItem[]; permissions: PermissionItem[]; onGrant: (item: PermissionItem) => void }) {
  const [topicId, setTopicId] = useState(topics[0]?.id ?? 1);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<PermissionItem['role']>('READ');
  const visible = permissions.filter((permission) => permission.topicId === topicId);
  const myTopics = topics.filter((topic) => permissions.some((permission) => permission.topicId === topic.id && permission.user === '오승환'));

  function grant(event: FormEvent) {
    event.preventDefault();
    if (!email.trim()) return;
    onGrant({ id: Date.now(), topicId, email: email.trim(), user: email.split('@')[0], role, grantedAt: '2026-08-08' });
    setEmail('');
  }
  return <div className="mc-page"><div className="mc-page-intro"><div><h2>Topic 권한</h2><p>Topic별 사용자 권한을 조회하고 새로운 접근 권한을 부여하세요.</p></div></div><section className="mc-my-topics"><div><span><MIcon name="user" /></span><div><h3>내가 사용할 수 있는 Topic</h3><p>현재 계정에 부여된 Topic 권한입니다.</p></div></div><div>{myTopics.map((topic) => <span key={topic.id}><i />{topic.displayName}<small>{permissions.find((permission) => permission.topicId === topic.id && permission.user === '오승환')?.role}</small></span>)}</div></section><div className="mc-permission-layout"><section className="mc-panel"><div className="mc-panel-title"><div><h3>Topic별 권한 목록</h3><p>Topic을 선택해 현재 권한을 확인하세요.</p></div><select value={topicId} onChange={(event) => setTopicId(Number(event.target.value))}>{topics.map((topic) => <option value={topic.id} key={topic.id}>{topic.displayName}</option>)}</select></div><div className="mc-permission-list"><div className="mc-permission-row heading"><span>사용자</span><span>권한</span><span>부여일</span><span /></div>{visible.map((permission) => <div className="mc-permission-row" key={permission.id}><span><i className="mc-user-dot">{permission.user.slice(0, 1).toUpperCase()}</i><span><strong>{permission.user}</strong><small>{permission.email}</small></span></span><span><Role role={permission.role} /></span><span>{permission.grantedAt}</span><button><MIcon name="dots" /></button></div>)}</div></section><section className="mc-panel mc-grant-card"><span className="mc-grant-icon"><MIcon name="shield" /></span><h3>새 권한 부여</h3><p>{topics.find((topic) => topic.id === topicId)?.displayName} Topic에 접근할 사용자를 추가합니다.</p><form onSubmit={grant}><label>사용자 이메일<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="user@streamcell.io" /></label><label>권한<select value={role} onChange={(event) => setRole(event.target.value as PermissionItem['role'])}><option value="READ">READ · 조회</option><option value="WRITE">WRITE · 조회/쓰기</option><option value="ADMIN">ADMIN · 권한 관리</option></select></label><div className="mc-role-help"><p><b>READ</b> Pipeline 입력 Topic으로 사용</p><p><b>WRITE</b> 결과를 Topic에 발행</p><p><b>ADMIN</b> Schema와 권한까지 관리</p></div><button className="mc-btn primary">권한 부여</button></form></section></div></div>;
}

function PipelinesView({ pipelines, onCreate, onOpen }: { pipelines: PipelineItem[]; onCreate: () => void; onOpen: (id: number) => void }) {
  const [filter, setFilter] = useState('ALL');
  const visible = pipelines.filter((pipeline) => filter === 'ALL' || pipeline.status === filter);
  return <div className="mc-page"><div className="mc-page-intro"><div><h2>Pipeline 운영</h2><p>Pipeline 상태를 조회하고 실행, 중지 및 Deployment 이력을 관리하세요.</p></div><button className="mc-btn primary" onClick={onCreate}>＋ 새 Pipeline</button></div><div className="mc-metric-grid four"><Metric icon="flow" label="Total Pipelines" value={pipelines.length} meta="전체 Pipeline" tone="blue" /><Metric icon="play" label="Running" value={pipelines.filter((p) => p.status === 'RUNNING').length} meta="정상 처리 중" tone="green" /><Metric icon="stop" label="Stopped / Draft" value={pipelines.filter((p) => p.status === 'STOPPED' || p.status === 'DRAFT').length} meta="배포 대기" tone="gray" /><Metric icon="alert" label="Failed" value={pipelines.filter((p) => p.status === 'FAILED').length} meta="조치 필요" tone="red" /></div><section className="mc-panel mc-table-panel"><div className="mc-list-tools pipelines"><div>{['ALL', 'RUNNING', 'STOPPED', 'FAILED', 'DRAFT'].map((status) => <button className={filter === status ? 'active' : ''} onClick={() => setFilter(status)} key={status}>{status === 'ALL' ? '전체' : status}</button>)}</div><label><MIcon name="search" /><input placeholder="Pipeline 검색" /></label></div><table><thead><tr><th>Pipeline</th><th>Type</th><th>Input Topic</th><th>Parallelism</th><th>Updated</th><th>Status</th><th /></tr></thead><tbody>{visible.map((pipeline) => <tr key={pipeline.id} onClick={() => onOpen(pipeline.id)}><td><strong>{pipeline.name}</strong><small>{pipeline.description}</small></td><td><span className={`mc-type ${pipeline.type === 'AI_SQL' ? 'ai' : 'jar'}`}>{pipeline.type}</span></td><td>{pipeline.inputTopics[0] ?? '—'}{pipeline.inputTopics.length > 1 && <small>+{pipeline.inputTopics.length - 1}</small>}</td><td>{pipeline.parallelism}</td><td>{pipeline.updatedAt}</td><td><Status status={pipeline.status} /></td><td><button className="mc-row-open"><MIcon name="chevron" /></button></td></tr>)}</tbody></table></section></div>;
}

function PipelineDetail({ pipeline, onBack, onStatusChange, onResults, onFailures }: { pipeline: PipelineItem; onBack: () => void; onStatusChange: (status: PipelineItem['status']) => void; onResults: () => void; onFailures: () => void }) {
  const history = deployments.filter((deployment) => deployment.pipelineId === pipeline.id);
  return <div className="mc-page"><button className="mc-back" onClick={onBack}>← Pipeline 목록</button><div className="mc-pipeline-heading"><div><span className={`mc-type ${pipeline.type === 'AI_SQL' ? 'ai' : 'jar'}`}>{pipeline.type}</span><h2>{pipeline.name}</h2><p>{pipeline.description}</p></div><div><button className="mc-btn secondary" onClick={onResults}><MIcon name="chart" /> 결과 보기</button>{pipeline.status === 'FAILED' && <button className="mc-btn danger-outline" onClick={onFailures}>실패 분석</button>}{pipeline.status === 'RUNNING' ? <button className="mc-btn danger-outline" onClick={() => onStatusChange('STOPPED')}><MIcon name="stop" /> 중지</button> : <button className="mc-btn primary" onClick={() => onStatusChange('RUNNING')}><MIcon name="play" /> {pipeline.status === 'DRAFT' ? '배포 및 실행' : '다시 실행'}</button>}</div></div><div className="mc-job-hero"><div><span className="mc-job-state"><i className={pipeline.status.toLowerCase()} /><small>CURRENT STATUS</small><Status status={pipeline.status} /></span><span><small>FLINK JOB ID</small><b>{pipeline.jobId ?? '배포 후 생성됩니다'}</b></span><span><small>PARALLELISM</small><b>{pipeline.parallelism}</b></span><span><small>INPUT TOPICS</small><b>{pipeline.inputTopics.length}</b></span><span><small>LAST UPDATED</small><b>{pipeline.updatedAt}</b></span></div></div><div className="mc-metric-grid four compact"><Metric icon="activity" label="Records In" value="2.84K" suffix="/s" meta="평균 처리량" tone="blue" /><Metric icon="activity" label="Records Out" value="2.83K" suffix="/s" meta="99.6% output" tone="cyan" /><Metric icon="clock" label="End-to-end Latency" value="184" suffix="ms" meta="p95 latency" tone="green" /><Metric icon="refresh" label="Checkpoints" value="128" meta="마지막 38초 전" tone="gray" /></div><div className="mc-two-column detail"><section className="mc-panel"><div className="mc-panel-title"><div><h3>{pipeline.type === 'CUSTOM_JAR' ? 'Custom JAR 설정' : 'AI Pipeline Plan'}</h3><p>현재 배포에 사용된 Pipeline 구성</p></div></div>{pipeline.type === 'CUSTOM_JAR' ? <div className="mc-config-list"><p><span>Artifact</span><b>streamcell-session-job-1.4.2.jar</b></p><p><span>Entry Class</span><b>com.streamcell.jobs.SessionAnalysisJob</b></p><p><span>Parallelism</span><b>{pipeline.parallelism}</b></p><p><span>Program Args</span><code>--checkpoint.interval=60000</code></p></div> : <div className="mc-plan-summary"><ol><li><i>1</i><span><b>Kafka Source</b><small>{pipeline.inputTopics[0]}</small></span></li><li><i>2</i><span><b>Event Time Window</b><small>1 minute tumbling window</small></span></li><li><i>3</i><span><b>Aggregation</b><small>COUNT, SUM, AVG</small></span></li><li><i>4</i><span><b>PostgreSQL Sink</b><small>Upsert minute summary</small></span></li></ol><button className="mc-btn secondary">Flink SQL 보기</button></div>}</section><section className="mc-panel"><div className="mc-panel-title"><div><h3>입력 Topic</h3><p>Job에서 사용 중인 데이터 소스</p></div></div><div className="mc-input-topics">{pipeline.inputTopics.map((topic) => <div key={topic}><span className="mc-topic-icon"><MIcon name="database" /></span><span><strong>{topic}</strong><small>Event Time · createdAt</small></span><i>READ</i></div>)}</div></section></div><section className="mc-panel mc-table-panel"><div className="mc-panel-title"><div><h3>Deployment 이력</h3><p>최근 Pipeline 배포와 실행 결과</p></div><button className="mc-btn secondary">전체 이력</button></div>{history.length ? <table><thead><tr><th>Deployment</th><th>Version</th><th>Started At</th><th>Duration</th><th>Operator</th><th>Status</th></tr></thead><tbody>{history.map((deployment) => <tr key={deployment.id}><td><strong>{deployment.id}</strong></td><td>{deployment.version}</td><td>{deployment.startedAt}</td><td>{deployment.duration}</td><td>{deployment.operator}</td><td><Status status={deployment.status} /></td></tr>)}</tbody></table> : <div className="mc-empty"><MIcon name="deploy" /><h3>아직 Deployment가 없습니다</h3><p>상단의 ‘배포 및 실행’ 버튼으로 첫 Deployment를 시작하세요.</p></div>}</section></div>;
}

function ResultsDashboard({ pipelines }: { pipelines: PipelineItem[] }) {
  const running = pipelines.filter((pipeline) => pipeline.status === 'RUNNING');
  const [pipelineId, setPipelineId] = useState(running[0]?.id ?? pipelines[0]?.id);
  const pipeline = pipelines.find((item) => item.id === pipelineId) ?? pipelines[0];
  const points = chartValues.map((value, index) => `${(index / (chartValues.length - 1)) * 100},${105 - value}`).join(' ');
  return <div className="mc-page"><div className="mc-page-intro"><div><span className="mc-live"><i /> STREAMING</span><h2>처리 결과 Dashboard</h2><p>Pipeline 상태와 실시간 집계 결과를 함께 확인하세요.</p></div><select className="mc-pipeline-select" value={pipelineId} onChange={(event) => setPipelineId(Number(event.target.value))}>{pipelines.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></div><div className="mc-result-status"><div><span className="mc-pipeline-mark"><MIcon name="flow" /></span><div><strong>{pipeline?.name}</strong><small>{pipeline?.jobId ?? 'No Job ID'} · {pipeline?.type}</small></div></div><Status status={pipeline?.status ?? 'DRAFT'} /><span><small>LAST UPDATE</small><b>방금 전</b></span><span><small>WATERMARK</small><b>14:34:55</b></span><span><small>LATENCY</small><b>184 ms</b></span></div><div className="mc-metric-grid four"><Metric icon="activity" label="분당 주문" value="1,284" meta="▲ 12.4% 이전 윈도우" tone="blue" /><Metric icon="chart" label="총 주문 금액" value="₩48.2M" meta="최근 1분" tone="cyan" /><Metric icon="chart" label="평균 주문 금액" value="₩37,611" meta="▲ 2.8% 평균 대비" tone="green" /><Metric icon="clock" label="처리 이벤트" value="2.84K" suffix="/s" meta="Kafka input rate" tone="gray" /></div><section className="mc-panel mc-chart-panel"><div className="mc-panel-title"><div><h3>실시간 주문 처리량</h3><p>최근 20분 · 1분 Tumbling Window</p></div><div className="mc-chart-legend"><span><i /> 주문 건수</span><button>20분⌄</button></div></div><div className="mc-chart"><div className="mc-y-labels"><span>1.5K</span><span>1.0K</span><span>500</span><span>0</span></div><svg viewBox="0 0 100 110" preserveAspectRatio="none" aria-label="실시간 주문 처리량 차트"><defs><linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#35b8c6" stopOpacity=".28"/><stop offset="100%" stopColor="#35b8c6" stopOpacity="0"/></linearGradient></defs><polygon points={`0,110 ${points} 100,110`} fill="url(#areaGradient)"/><polyline points={points} fill="none" stroke="#2da9b9" strokeWidth="1.4" vectorEffect="non-scaling-stroke" /></svg><div className="mc-x-labels"><span>14:16</span><span>14:21</span><span>14:26</span><span>14:31</span><span>14:35</span></div></div></section><section className="mc-panel mc-table-panel"><div className="mc-panel-title"><div><h3>실시간 집계 결과</h3><p>order_minute_summary · 자동 갱신</p></div><div><span className="mc-auto-refresh"><i /> 5초마다 갱신</span><button className="mc-btn secondary">CSV 내보내기</button></div></div><table><thead><tr><th>Updated At</th><th>Window</th><th>Order Count</th><th>Total Amount</th><th>Average Amount</th><th>Change</th></tr></thead><tbody>{resultRows.map((row) => <tr key={row.time}><td><strong>{row.time}</strong></td><td>{row.window}</td><td>{row.orders.toLocaleString()}</td><td>{row.amount}</td><td>{row.avg}</td><td><span className={row.change.startsWith('+') ? 'mc-positive' : 'mc-negative'}>{row.change}</span></td></tr>)}</tbody></table></section></div>;
}

function FailuresView({ onOpenPipeline }: { onOpenPipeline: (id: number) => void }) {
  const [selectedId, setSelectedId] = useState(failures[0].id);
  const selected = failures.find((failure) => failure.id === selectedId) ?? failures[0];
  return <div className="mc-page"><div className="mc-page-intro"><div><h2>Pipeline 실패 분석</h2><p>원본 Exception과 AI 분석을 바탕으로 실패 원인과 권장 조치를 확인하세요.</p></div></div><div className="mc-failure-layout"><section className="mc-panel mc-failure-list"><div className="mc-panel-title"><div><h3>최근 실패</h3><p>최근 7일 · {failures.length}건</p></div></div>{failures.map((failure) => <button className={selected.id === failure.id ? 'active' : ''} onClick={() => setSelectedId(failure.id)} key={failure.id}><span className="mc-failure-mark">!</span><span><strong>{failure.pipeline}</strong><small>{failure.stage}</small><small>{failure.occurredAt}</small></span><i>{failure.severity}</i></button>)}</section><div className="mc-failure-content"><section className="mc-panel mc-failure-head"><div><span className="mc-failure-mark large">!</span><div><span>{selected.id} · {selected.severity}</span><h3>{selected.pipeline}</h3><p>{selected.stage}에서 Job이 실패했습니다.</p></div></div><button className="mc-btn secondary" onClick={() => onOpenPipeline(selected.pipelineId)}>Pipeline 열기</button></section><section className="mc-panel"><div className="mc-panel-title"><div><h3>원본 Exception</h3><p>Flink JobManager에서 수집한 Stack Trace</p></div><button>Copy</button></div><pre className="mc-exception"><code>{selected.exception}</code></pre></section><section className="mc-panel mc-ai-analysis"><div className="mc-analysis-head"><span>✦</span><div><h3>AI 실패 분석</h3><p>Exception, Job 설정 및 Metric을 함께 분석했습니다.</p></div><b>92% confidence</b></div><div className="mc-analysis-summary"><small>ROOT CAUSE</small><p>{selected.summary}</p></div><div className="mc-analysis-columns"><div><h4>발견된 원인</h4>{selected.causes.map((cause, index) => <p key={cause}><i>{index + 1}</i>{cause}</p>)}</div><div><h4>권장 조치</h4>{selected.actions.map((action, index) => <p key={action}><i>✓</i>{action}</p>)}</div></div><div className="mc-analysis-actions"><button className="mc-btn secondary">분석 리포트 저장</button><button className="mc-btn primary">권장 설정 적용하기</button></div></section></div></div></div>;
}

function Metric({ icon, label, value, suffix, meta, tone }: { icon: string; label: string; value: string | number; suffix?: string; meta: string; tone: string }) { return <article className="mc-metric"><span className={`mc-metric-icon ${tone}`}><MIcon name={icon} /></span><div><p>{label}</p><strong>{value}<small>{suffix}</small></strong><span>{meta}</span></div></article>; }
function Bar({ value }: { value: number }) { return <span className="mc-bar"><i style={{ width: `${value}%` }} /><b>{value}%</b></span>; }
function Status({ status }: { status: string }) { return <span className={`mc-status ${status.toLowerCase()}`}><i />{status}</span>; }
function Role({ role }: { role: PermissionItem['role'] }) { return <span className={`mc-role ${role.toLowerCase()}`}>{role}</span>; }
function Logo() { return <div className="mc-logo"><span><i /><i /><i /></span><strong>streamcell</strong></div>; }

function MIcon({ name }: { name: string }) {
  const paths: Record<string, ReactNode> = {
    cluster: <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="9" y="14" width="6" height="6" rx="1" /><path d="M7 10v2h10v-2M12 12v2" /></>, database: <><ellipse cx="12" cy="5" rx="7" ry="3" /><path d="M5 5v7c0 1.7 3.1 3 7 3s7-1.3 7-3V5M5 12v7c0 1.7 3.1 3 7 3s7-1.3 7-3v-7" /></>, shield: <path d="M12 3 5 6v5c0 4.7 2.8 8.2 7 10 4.2-1.8 7-5.3 7-10V6l-7-3Zm-3 9 2 2 4-4" />, flow: <><circle cx="5" cy="6" r="2" /><circle cx="19" cy="6" r="2" /><circle cx="12" cy="18" r="2" /><path d="M7 7.3 10.5 16M17 7.3 13.5 16" /></>, chart: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /></>, alert: <><path d="M12 3 2.8 20h18.4L12 3Z" /><path d="M12 9v4M12 17h.01" /></>, bell: <><path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 22h4" /></>, dots: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>, refresh: <><path d="M20 11a8 8 0 0 0-14.5-4.7L3 9" /><path d="M3 4v5h5M4 13a8 8 0 0 0 14.5 4.7L21 15" /><path d="M21 20v-5h-5" /></>, server: <><rect x="4" y="3" width="16" height="7" rx="2" /><rect x="4" y="14" width="16" height="7" rx="2" /><path d="M8 6.5h.01M8 17.5h.01" /></>, slots: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>, play: <path d="m8 5 11 7-11 7V5Z" />, stop: <rect x="6" y="6" width="12" height="12" rx="1" />, activity: <path d="M3 12h4l2-5 4 10 2-5h6" />, chevron: <path d="m9 18 6-6-6-6" />, search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></>, clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>, user: <><circle cx="12" cy="8" r="3" /><path d="M5 20c.8-3.2 3.1-5 7-5s6.2 1.8 7 5" /></>, deploy: <><path d="M12 3v12M8 11l4 4 4-4" /><path d="M4 17v3h16v-3" /></>,
  };
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

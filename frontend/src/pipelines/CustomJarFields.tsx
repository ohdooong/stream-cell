import type { Topic } from '../api/platform';
import type { CustomJarDraft } from './customJarRegistration';

export function CustomJarFields({ draft, onChange, topics, disabled }: {
  draft: CustomJarDraft; onChange: (draft: CustomJarDraft) => void; topics: Topic[]; disabled: boolean;
}) {
  return <>
    <label className="connected-field wide"><span>JAR 파일</span><input required type="file" accept=".jar,application/java-archive" disabled={disabled} onChange={(event) => onChange({ ...draft, file: event.target.files?.[0] || null })} /><small>등록 실패 시 선택한 파일과 입력값을 유지합니다.</small></label>
    <label className="connected-field"><span>Entry Class</span><input required value={draft.entryClass} disabled={disabled} onChange={(event) => onChange({ ...draft, entryClass: event.target.value })} placeholder="com.example.StreamJob" /></label>
    <label className="connected-field"><span>Parallelism</span><input type="number" required min="1" max="8" step="1" value={draft.parallelism} disabled={disabled} onChange={(event) => onChange({ ...draft, parallelism: Number(event.target.value) })} /></label>
    <label className="connected-field wide"><span>Input Topic</span><select required value={topics.some((topic) => topic.topicId === draft.inputId) ? draft.inputId : ''} disabled={disabled} onChange={(event) => onChange({ ...draft, inputId: Number(event.target.value) })}><option value="" disabled>입력 Topic을 선택하세요</option>{topics.map((topic) => <option key={topic.topicId} value={topic.topicId}>{topic.topicName}</option>)}</select><small>DEPLOY 권한이 있는 Topic 하나를 선택해 등록합니다.</small></label>
    <label className="connected-field wide"><span>Program Arguments</span><textarea rows={5} value={draft.args} disabled={disabled} onChange={(event) => onChange({ ...draft, args: event.target.value })} /><small>한 줄에 key=value 형식</small></label>
  </>;
}

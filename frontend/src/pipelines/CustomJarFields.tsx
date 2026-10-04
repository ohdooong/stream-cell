import type { Topic } from '../api/platform';
import type { CustomJarDraft } from './customJarRegistration';

export function CustomJarFields({ draft, onChange, topics, disabled }: {
  draft: CustomJarDraft; onChange: (draft: CustomJarDraft) => void; topics: Topic[]; disabled: boolean;
}) {
  return <>
    <label className="connected-field wide"><span>JAR 파일</span><input required type="file" accept=".jar,application/java-archive" disabled={disabled} onChange={(event) => onChange({ ...draft, file: event.target.files?.[0] || null })} /><small>등록 실패 시 선택한 파일과 입력값을 유지합니다.</small></label>
    <label className="connected-field"><span>Entry Class</span><input required value={draft.entryClass} disabled={disabled} onChange={(event) => onChange({ ...draft, entryClass: event.target.value })} placeholder="com.example.StreamJob" /></label>
    <label className="connected-field"><span>Parallelism</span><input type="number" required min="1" max="8" step="1" value={draft.parallelism} disabled={disabled} onChange={(event) => onChange({ ...draft, parallelism: Number(event.target.value) })} /></label>
    <label className="connected-field"><span>Input Topic</span><select value={draft.inputId} disabled={disabled} onChange={(event) => onChange({ ...draft, inputId: Number(event.target.value) })}><option value="0">선택 안 함</option>{draft.inputId > 0 && !topics.some((topic) => topic.topicId === draft.inputId) && <option value={draft.inputId}>Topic #{draft.inputId}</option>}{topics.map((topic) => <option key={topic.topicId} value={topic.topicId}>{topic.topicName}</option>)}</select></label>
    <label className="connected-field"><span>Output Topic</span><select value={draft.outputId} disabled={disabled} onChange={(event) => onChange({ ...draft, outputId: Number(event.target.value) })}><option value="0">선택 안 함</option>{draft.outputId > 0 && !topics.some((topic) => topic.topicId === draft.outputId) && <option value={draft.outputId}>Topic #{draft.outputId}</option>}{topics.map((topic) => <option key={topic.topicId} value={topic.topicId}>{topic.topicName}</option>)}</select></label>
    <label className="connected-field wide"><span>Program Arguments</span><textarea rows={5} value={draft.args} disabled={disabled} onChange={(event) => onChange({ ...draft, args: event.target.value })} /><small>한 줄에 key=value 형식</small></label>
  </>;
}

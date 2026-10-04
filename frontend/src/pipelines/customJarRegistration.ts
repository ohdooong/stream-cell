import { platformApi, type Pipeline, type PipelineDetail } from '../api/platform';
import { customJarDetail, programArgsText } from './detailData';

export type CustomJarDraft = {
  file: File | null; entryClass: string; inputId: number; outputId: number; parallelism: number; args: string;
};

export function customJarDraft(item?: PipelineDetail): CustomJarDraft {
  const detail = item ? customJarDetail(item) : null;
  return {
    file: null, entryClass: typeof detail?.entryClass === 'string' ? detail.entryClass : '',
    inputId: detail?.inputTopicIds[0] ?? 0, outputId: detail?.outputTopicIds[0] ?? 0,
    parallelism: Number(detail?.parallelism) || 1, args: programArgsText(detail?.programArgs),
  };
}

export function hasCustomJarArtifact(item: PipelineDetail) {
  const detail = customJarDetail(item);
  return [detail.artifactId, detail.originalFileName, detail.storedFileName, detail.flinkJarId]
    .some((value) => (typeof value === 'string' && Boolean(value.trim())) || (typeof value === 'number' && value > 0));
}

export function canRegisterCustomJar(item: PipelineDetail) {
  return item.pipelineType === 'CUSTOM_JAR' && ['DRAFT', 'CREATED', 'FAILED'].includes(item.pipelineStatus) && !hasCustomJarArtifact(item);
}

export function validateCustomJarDraft(draft: CustomJarDraft) {
  if (!draft.file) throw new Error('JAR 파일을 선택해 주세요.');
  if (!draft.file.name.toLowerCase().endsWith('.jar')) throw new Error('.jar 파일을 선택해 주세요.');
  if (!/^[a-zA-Z_$][a-zA-Z0-9_$]*(\.[a-zA-Z_$][a-zA-Z0-9_$]*)*$/.test(draft.entryClass.trim())) throw new Error('Entry Class를 올바른 클래스 경로로 입력해 주세요.');
  if (!Number.isInteger(draft.parallelism) || draft.parallelism < 1 || draft.parallelism > 8) throw new Error('Parallelism은 1부터 8까지의 정수로 입력해 주세요.');
}

type RegistrationApi = Pick<typeof platformApi, 'createPipeline' | 'getPipeline' | 'uploadCustomJar'>;
type PipelineInput = Pick<Pipeline, 'ownerUserId' | 'pipelineName' | 'description' | 'pipelineType'>;

export async function registerCustomJar(input: {
  pipelineId?: number; pipelineInput?: PipelineInput; userId: number; draft: CustomJarDraft;
  onCreated?: (id: number) => void;
}, api: RegistrationApi = platformApi) {
  validateCustomJarDraft(input.draft);
  let pipelineId = input.pipelineId;
  if (pipelineId) {
    // A lost upload response does not mean the server failed to save the JAR.
    const latest = await api.getPipeline(pipelineId, 'CUSTOM_JAR');
    if (!latest || latest.pipelineId !== pipelineId || latest.pipelineType !== 'CUSTOM_JAR') throw new Error('Pipeline 등록 상태를 확인할 수 없습니다. 상세 정보를 다시 조회해 주세요.');
    if (hasCustomJarArtifact(latest)) {
      if (['DRAFT', 'CREATED'].includes(latest.pipelineStatus)) throw new Error('JAR 정보는 있지만 등록 완료 상태가 아닙니다. 백엔드의 미완료 등록 정보 복구가 필요합니다.');
      return { pipelineId, alreadyRegistered: true };
    }
    if (!canRegisterCustomJar(latest)) throw new Error(`현재 ${latest.pipelineStatus} 상태에서는 JAR를 등록할 수 없습니다. 상세 정보를 다시 조회해 주세요.`);
  } else {
    if (!input.pipelineInput || input.pipelineInput.pipelineType !== 'CUSTOM_JAR') throw new Error('Custom JAR Pipeline 기본 정보가 필요합니다.');
    const pipeline = await api.createPipeline(input.pipelineInput);
    pipelineId = pipeline.pipelineId;
    if (!Number.isSafeInteger(pipelineId) || pipelineId <= 0) throw new Error('생성된 Pipeline ID가 올바르지 않습니다. 목록에서 생성 여부를 확인해 주세요.');
    // Remember the ID before uploading so a failure can never recreate the Pipeline.
    input.onCreated?.(pipelineId);
  }
  const { draft } = input;
  const programArgs = Object.fromEntries(draft.args.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
    const index = line.indexOf('=');
    return index < 0 ? [line, ''] : [line.slice(0, index).trim(), line.slice(index + 1).trim()];
  }));
  await api.uploadCustomJar(pipelineId, draft.file!, {
    userId: input.userId, entryClass: draft.entryClass.trim(),
    inputTopicIds: draft.inputId ? [draft.inputId] : [], outputTopicIds: draft.outputId ? [draft.outputId] : [],
    parallelism: draft.parallelism, programArgs,
  });
  return { pipelineId, alreadyRegistered: false };
}

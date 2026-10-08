import { api, unwrap } from './client';
import { pipelineDetailPath } from './pipelineDetail';
import { rememberCustomJarReceipt, withCustomJarReceipt } from './customJarReceipt';
import { getInputTopics } from './inputTopics';

type BaseResponse<T> = { status: number; message: string; timestamp: string; body: T };

export type ClusterOverview = {
  'flink-version': string;
  taskmanagers: number;
  'slots-total': number;
  'slots-available': number;
  'jobs-running': number;
  'jobs-finished': number;
  'jobs-failed': number;
  'jobs-cancelled': number;
};

export type Topic = {
  topicId: number; topicName: string; displayName?: string; description?: string;
  schemaJson?: string; timeField?: string; messageFormat?: string;
};
export type TopicPermissionType = 'VIEW' | 'QUERY' | 'DEPLOY' | 'ADMIN';
export type TopicPermission = {
  permissionId: number; topicId: number; topicName: string; userId: number;
  userName: string; topicPermissionType: TopicPermissionType;
};
export type User = { userId: number; loginId?: string; email: string; name: string; status: string };
export type PipelineType = 'AI_SQL' | 'CUSTOM_JAR';
export type PipelineStatus = 'DRAFT' | 'CREATED' | 'ARTIFACT_UPLOADED' | 'DEPLOYING' | 'RUNNING' | 'FAILED' | 'STOPPING' | 'STOPPED' | 'FINISHED' | 'SUSPENDED';
export type Pipeline = {
  pipelineId: number; ownerUserId: number; pipelineName: string; description?: string;
  pipelineType: PipelineType; pipelineStatus: PipelineStatus; naturalLanguageRequest?: string;
  pipelinePlanJson?: string; generatedSql?: string;
};
export type PipelineDetail = Pipeline & Record<string, unknown>;
export type Artifact = { artifactId: number; pipelineId: number; artifactType: 'CUSTOM_JAR'; originalFileName: string; storedFileName: string; storedFilePath: string; flinkJarId?: string };
export type Deployment = { pipelineId: number; deploymentId: number; flinkJarId?: string | null; flinkJobId: string; status: 'DEPLOYING' | 'RUNNING' | 'FAILED' | 'STOPPED' | 'FINISHED' };
export type TopicSchemaInput = { displayName: string; description: string; messageFormat: string; timeField: string; schemaJson: string };

const FLINK = '/api/v1/platform/flink';
const TOPIC = '/api/v1/platform/topic';
const PIPELINE = '/api/v1/platform/pipeline';

export const platformApi = {
  getInputTopics,
  async getClusterOverview() { return unwrap(await api<BaseResponse<ClusterOverview>>(`${FLINK}/cluster-overview`)); },
  async getTopics(signal?: AbortSignal) { return unwrap(await api<BaseResponse<Topic[]>>(`${TOPIC}/topics`, { signal })); },
  async syncTopics() { return unwrap(await api<BaseResponse<Topic[]>>(`${TOPIC}/sync`, { method: 'POST' })); },
  async getTopic(topicId: number, signal?: AbortSignal) { return unwrap(await api<BaseResponse<Topic>>(`${TOPIC}/topics/${topicId}`, { signal })); },
  async updateTopicSchema(topicId: number, input: TopicSchemaInput) {
    return unwrap(await api<BaseResponse<number>>(`${TOPIC}/topics/${topicId}/schema`, { method: 'PUT', body: JSON.stringify(input) }));
  },
  async getTopicPermissions(topicId: number) { return unwrap(await api<BaseResponse<TopicPermission[]>>(`${TOPIC}/topics/${topicId}/permissions`)); },
  async getUserTopicPermissions(userId: number) { return unwrap(await api<BaseResponse<TopicPermission[]>>(`${TOPIC}/topics/permissions?userId=${userId}`)); },
  async grantTopicPermissions(topicId: number, userIds: number[], topicPermissionType: TopicPermissionType) {
    return unwrap(await api<BaseResponse<TopicPermission[]>>(`${TOPIC}/topics/${topicId}/permissions`, { method: 'POST', body: JSON.stringify({ userIds, topicPermissionType }) }));
  },
  async getUsers() { return api<User[]>('/api/v1/web/user/items'); },
  async getPipelines(userId: number) { return unwrap(await api<BaseResponse<Pipeline[]>>(`/api/v1/web/my/pipeline/pipelines?userId=${userId}`)); },
  async getPipeline(pipelineId: number, type: PipelineType) {
    const detail = unwrap(await api<BaseResponse<PipelineDetail>>(pipelineDetailPath(pipelineId, type)));
    return detail ? withCustomJarReceipt(detail) : detail;
  },
  async createPipeline(input: Pick<Pipeline, 'ownerUserId' | 'pipelineName' | 'description' | 'pipelineType'>) {
    return unwrap(await api<BaseResponse<Pipeline>>(`${PIPELINE}/pipelines`, { method: 'POST', body: JSON.stringify(input) }));
  },
  async updatePipeline(input: Pick<Pipeline, 'pipelineId' | 'ownerUserId' | 'pipelineName' | 'description' | 'pipelineType'>) {
    return unwrap(await api<BaseResponse<Pipeline>>(`${PIPELINE}/pipelines`, { method: 'PATCH', body: JSON.stringify(input) }));
  },
  async uploadCustomJar(pipelineId: number, file: File, config: { userId: number; entryClass: string; inputTopicIds: number[]; parallelism: number; programArgs: Record<string, string> }) {
    if (config.inputTopicIds.length !== 1 || !Number.isSafeInteger(config.inputTopicIds[0]) || config.inputTopicIds[0] <= 0) throw new Error('입력 Topic을 하나 선택해 주세요.');
    const form = new FormData();
    form.append('file', file);
    form.append('createCustomJobConfig', new Blob([JSON.stringify(config)], { type: 'application/json' }));
    const artifact = unwrap(await api<BaseResponse<Artifact>>(`${PIPELINE}/pipelines/${pipelineId}/custom-jar`, { method: 'POST', body: form }));
    rememberCustomJarReceipt(pipelineId, config.userId, artifact);
    return artifact;
  },
  async deployPipeline(pipelineId: number) {
    return unwrap(await api<BaseResponse<Deployment>>(`${PIPELINE}/pipelines/deployment/${pipelineId}/deploy`, { method: 'POST' }));
  },
  async deployAiSqlPipeline(pipelineId: number) {
    return unwrap(await api<BaseResponse<Deployment>>(`${PIPELINE}/pipelines/deployment/${pipelineId}/ai-sql/deploy`, { method: 'POST' }));
  },
  async stopPipeline(pipelineId: number) {
    await api<BaseResponse<unknown>>(`${PIPELINE}/pipelines/deployment/${pipelineId}/stop`, { method: 'POST' });
  },
  async syncPipelineStatus(pipelineId: number) {
    const status = unwrap(await api<BaseResponse<PipelineStatus>>(`${PIPELINE}/pipelines/${pipelineId}/status`, { method: 'PUT' }));
    if (!['DRAFT', 'CREATED', 'ARTIFACT_UPLOADED', 'DEPLOYING', 'RUNNING', 'FAILED', 'STOPPING', 'STOPPED', 'FINISHED', 'SUSPENDED'].includes(status)) {
      throw new Error('Pipeline 상태 응답을 확인할 수 없습니다.');
    }
    return status;
  },
};

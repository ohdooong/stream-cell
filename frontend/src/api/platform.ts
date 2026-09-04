import { api, unwrap } from './client';

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
export type PipelineStatus = 'DRAFT' | 'CREATED' | 'ARTIFACT_UPLOADED' | 'DEPLOYING' | 'RUNNING' | 'FAILED' | 'STOPPED' | 'FINISHED';
export type Pipeline = {
  pipelineId: number; ownerUserId: number; pipelineName: string; description?: string;
  pipelineType: PipelineType; pipelineStatus: PipelineStatus; naturalLanguageRequest?: string;
  pipelinePlanJson?: string; generatedSql?: string;
};
export type Artifact = { artifactId: number; pipelineId: number; artifactType: 'CUSTOM_JAR'; originalFileName: string; storedFileName: string; storedFilePath: string; flinkJarId?: string };
export type Deployment = { pipelineId: number; deploymentId: number; flinkJarId: string; flinkJobId: string; status: 'DEPLOYING' | 'RUNNING' | 'FAILED' | 'STOPPED' | 'FINISHED' };
export type TopicSchemaInput = { displayName: string; description: string; messageFormat: string; timeField: string; schemaJson: string };

const FLINK = '/api/v1/platform/flink';
const TOPIC = '/api/v1/platform/topic';
const PIPELINE = '/api/v1/platform/pipeline';

export const platformApi = {
  async getClusterOverview() { return unwrap(await api<BaseResponse<ClusterOverview>>(`${FLINK}/cluster-overview`)); },
  async getTopics() { return unwrap(await api<BaseResponse<Topic[]>>(`${TOPIC}/topics`)); },
  async syncTopics() { return unwrap(await api<BaseResponse<string>>(`${TOPIC}/sync`, { method: 'POST' })); },
  async getTopic(topicId: number) { return unwrap(await api<BaseResponse<Topic>>(`${TOPIC}/topics/${topicId}`)); },
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
  async getPipeline(pipelineId: number) { return unwrap(await api<BaseResponse<Pipeline>>(`${PIPELINE}/pipelines/${pipelineId}`)); },
  async createPipeline(input: Pick<Pipeline, 'ownerUserId' | 'pipelineName' | 'description' | 'pipelineType'>) {
    return unwrap(await api<BaseResponse<Pipeline>>(`${PIPELINE}/pipelines`, { method: 'POST', body: JSON.stringify(input) }));
  },
  async updatePipeline(input: Pick<Pipeline, 'pipelineId' | 'ownerUserId' | 'pipelineName' | 'description' | 'pipelineType'>) {
    return unwrap(await api<BaseResponse<Pipeline>>(`${PIPELINE}/pipelines`, { method: 'PATCH', body: JSON.stringify(input) }));
  },
  async uploadCustomJar(pipelineId: number, file: File, config: { userId: number; entryClass: string; inputTopicIds: number[]; outputTopicIds: number[]; parallelism: number; programArgs: Record<string, string> }) {
    const form = new FormData();
    form.append('file', file);
    form.append('createCustomJobConfig', new Blob([JSON.stringify(config)], { type: 'application/json' }));
    return unwrap(await api<BaseResponse<Artifact>>(`${PIPELINE}/pipelines/${pipelineId}/custom-jar`, { method: 'POST', body: form }));
  },
  async deployPipeline(pipelineId: number) {
    return unwrap(await api<BaseResponse<Deployment>>(`${PIPELINE}/pipelines/deployment/${pipelineId}/deploy`, { method: 'POST' }));
  },
};

import type { Artifact, PipelineDetail } from './platform';
import { customJarDetail } from '../pipelines/detailData';

// Upload receipts are a session-only fallback until the detail API returns Artifact.
const receipts = new Map<number, { ownerUserId: number; artifact: Artifact }>();

export function clearCustomJarReceipts() { receipts.clear(); }

export function rememberCustomJarReceipt(pipelineId: number, ownerUserId: number, artifact: Artifact) {
  if (artifact && artifact.pipelineId === pipelineId) receipts.set(pipelineId, { ownerUserId, artifact });
}

export function withCustomJarReceipt(item: PipelineDetail): PipelineDetail {
  if (item.pipelineType !== 'CUSTOM_JAR') return item;
  if (['DRAFT', 'CREATED'].includes(item.pipelineStatus)) {
    receipts.delete(item.pipelineId);
    return item;
  }
  const receipt = receipts.get(item.pipelineId);
  if (!receipt || receipt.ownerUserId !== item.ownerUserId) return item;
  const detail = customJarDetail(item);
  if (detail.artifactId && detail.artifactId !== receipt.artifact.artifactId) return item;
  if (detail.originalFileName && detail.originalFileName !== receipt.artifact.originalFileName) return item;
  const serverArtifact = Object.fromEntries(
    ['artifactId', 'originalFileName', 'storedFileName', 'storedFilePath', 'flinkJarId']
      .map((key) => [key, detail[key as keyof typeof detail]])
      .filter(([, value]) => value !== undefined && value !== null),
  );
  return { ...item, pipelineArtifact: { ...receipt.artifact, ...serverArtifact } };
}

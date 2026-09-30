function normalizedJson(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(normalizedJson);
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).sort(([left], [right]) => left.localeCompare(right))
        .map(([key, entry]) => [key, normalizedJson(entry)]),
    );
  }
  return value;
}

export function isPersistedPipelinePlan(storedJson: unknown, expectedPlan: unknown): boolean {
  if (typeof storedJson !== 'string' || !storedJson.trim()) return false;
  try {
    return JSON.stringify(normalizedJson(JSON.parse(storedJson)))
      === JSON.stringify(normalizedJson(expectedPlan));
  } catch {
    return false;
  }
}

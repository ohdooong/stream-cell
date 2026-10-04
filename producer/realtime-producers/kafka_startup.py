"""Check topic metadata before queueing any events."""
import time


def wait_for_topics(admin, topics, timeout, stopped, allow_missing=False):
    deadline = time.monotonic() + timeout
    details = "metadata unavailable"
    while not stopped.is_set():
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        try:
            metadata = admin.list_topics(timeout=min(5, remaining))
        except Exception as exc:
            details = f"metadata request failed: {exc}"
        else:
            missing = [name for name in topics if name not in metadata.topics]
            if missing and not allow_missing:
                raise RuntimeError(
                    f"Missing Kafka topics: {', '.join(missing)}. "
                    "Run with --create-topics or rerun kafka-init; "
                    "check that bootstrap servers point to the intended cluster."
                )
            pending = [f"{name}: missing" for name in missing]
            for name in topics:
                topic = metadata.topics.get(name)
                if topic is None:
                    continue
                if topic.error:
                    pending.append(f"{name}: {topic.error}")
                elif not topic.partitions:
                    pending.append(f"{name}: no partitions")
                else:
                    for partition in topic.partitions.values():
                        if partition.error or partition.leader < 0:
                            pending.append(f"{name}[{partition.id}]: {partition.error or 'no leader'}")
            if not pending:
                return
            details = "; ".join(pending)
        stopped.wait(min(0.5, max(0, deadline - time.monotonic())))
    if stopped.is_set():
        raise InterruptedError("Kafka startup interrupted")
    raise RuntimeError(
        f"Kafka topics were not ready within {timeout}s: {details}. "
        "Check broker connectivity, advertised.listeners and topic leaders."
    )

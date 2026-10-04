"""Run one scenario or all ten; dry-run does not require Kafka libraries."""
import argparse
import json
import math
import os
import random
import signal
import sys
import threading

from events import KINDS, create_event
from kafka_startup import wait_for_topics


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kind", choices=["all", *KINDS], default="all")
    parser.add_argument("--bootstrap-servers", default=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:19092,localhost:19093,localhost:19094"))
    parser.add_argument("--interval", type=float, default=1, help="Seconds between rounds; each selected kind emits once per round")
    parser.add_argument("--count", type=int, default=0, help="Rounds per kind; 0 runs until Ctrl+C")
    parser.add_argument("--anomaly-rate", type=float, default=0.1)
    parser.add_argument("--seed", type=int)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--create-topics", action="store_true")
    parser.add_argument("--startup-timeout", type=float, default=30, help="Seconds to wait for topic metadata and partition leaders")
    parser.add_argument("--partitions", type=int, default=3)
    parser.add_argument("--replication-factor", type=int, default=3)
    args = parser.parse_args(argv)
    if not math.isfinite(args.interval) or args.interval <= 0 or args.count < 0:
        parser.error("interval must be finite and positive; count must be non-negative")
    if not 0 <= args.anomaly_rate <= 1:
        parser.error("anomaly-rate must be between 0 and 1")
    if args.partitions < 1 or args.replication_factor < 1:
        parser.error("partitions and replication-factor must be positive")
    if not math.isfinite(args.startup_timeout) or args.startup_timeout <= 0:
        parser.error("startup-timeout must be finite and positive")
    selected = list(KINDS) if args.kind == "all" else [args.kind]
    stopped = threading.Event()
    for sig in (signal.SIGINT, signal.SIGTERM):
        signal.signal(sig, lambda *_: stopped.set())
    rng = random.Random(args.seed)
    producer = None
    failed = []

    def delivered(error, message):
        if error:
            failed.append(str(error))
            print(f"Delivery failed: topic={message.topic()} partition={message.partition()} error={error}", file=sys.stderr)

    if not args.dry_run:
        from confluent_kafka import Producer
        from confluent_kafka.admin import AdminClient, NewTopic
        admin = AdminClient({"bootstrap.servers": args.bootstrap_servers})
        if args.create_topics:
            from confluent_kafka import KafkaException, KafkaError
            futures = admin.create_topics([NewTopic(KINDS[k][0], num_partitions=args.partitions, replication_factor=args.replication_factor) for k in selected], request_timeout=30)
            for topic, future in futures.items():
                try:
                    future.result()
                except KafkaException as exc:
                    if exc.args[0].code() != KafkaError.TOPIC_ALREADY_EXISTS:
                        raise RuntimeError(f"Cannot create topic {topic}: {exc}") from exc
        topics = [KINDS[k][0] for k in selected]
        print(f"Checking Kafka topics at {args.bootstrap_servers}: {', '.join(topics)}", file=sys.stderr)
        wait_for_topics(admin, topics, args.startup_timeout, stopped, allow_missing=args.create_topics)
        print("Kafka topics ready; starting event delivery.", file=sys.stderr)
        # Match order-producer's delivery policy without idempotent PID allocation.
        producer = Producer({
            "bootstrap.servers": args.bootstrap_servers,
            "client.id": f"streamcell-{args.kind}-producer",
            "enable.idempotence": False,
            "acks": "all",
            "retries": 3,
            "delivery.timeout.ms": 30000,
        })
    rounds = 0
    try:
        while not stopped.is_set() and (args.count == 0 or rounds < args.count):
            for kind in selected:
                if stopped.is_set() or failed:
                    break
                event = create_event(kind, rng, args.anomaly_rate)
                topic, key_field = KINDS[kind]
                value = json.dumps(event, ensure_ascii=False)
                if producer:
                    while not stopped.is_set() and not failed:
                        try:
                            producer.produce(topic, key=event[key_field].encode(), value=value.encode("utf-8"), on_delivery=delivered)
                            break
                        except BufferError:
                            producer.poll(0.2)
                    producer.poll(0)
                else:
                    print(json.dumps({"topic": topic, "key": event[key_field], "value": event}, ensure_ascii=False))
            rounds += 1
            if failed:
                break
            if args.count == 0 or rounds < args.count:
                stopped.wait(args.interval)
    finally:
        if producer:
            remaining = producer.flush(35)
            if remaining:
                failed.append(f"{remaining} messages were not delivered")
            print(f"Stopped after {rounds} rounds; delivery errors={len(failed)}", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"Producer failed: {exc}", file=sys.stderr)
        sys.exit(1)

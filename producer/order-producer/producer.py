import json
import os
import random
import signal
import sys
import time
import uuid
from datetime import datetime
from decimal import Decimal

from confluent_kafka import Producer


BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:19092")
TOPIC_NAME = os.getenv("KAFKA_TOPIC", "orders")
INTERVAL_SECONDS = float(os.getenv("PRODUCE_INTERVAL_SECONDS", "1"))

PRODUCT_IDS = [
    "product-001",
    "product-002",
    "product-003",
    "product-004",
    "product-005",
]

USER_IDS = [
    "user-001",
    "user-002",
    "user-003",
    "user-004",
    "user-005",
    "user-006",
    "user-007",
    "user-008",
    "user-009",
    "user-010",
]


running = True


def shutdown(signum, frame):
    global running
    print("\nShutdown signal received. Flushing producer...")
    running = False


def delivery_report(err, msg):
    if err is not None:
        print(f"[ERROR] Message delivery failed: {err}")
        return

    print(
        "[OK] Delivered "
        f"topic={msg.topic()}, partition={msg.partition()}, offset={msg.offset()}"
    )


def random_payment_amount() -> float:
    amount = Decimal(random.randint(1_000, 100_000))
    return float(amount)


def create_order_event() -> dict:
    now = datetime.now()

    return {
        "order_id": f"ord-{uuid.uuid4().hex[:12]}",
        "user_id": random.choice(USER_IDS),
        "product_id": random.choice(PRODUCT_IDS),
        "payment_amount": random_payment_amount(),
        "event_time": now.strftime("%Y-%m-%d %H:%M:%S"),
    }


def main():
    producer_config = {
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "client.id": "streamcell-order-producer",
        "acks": "all",
        "retries": 3,
    }

    producer = Producer(producer_config)

    print("StreamCell Order Producer started.")
    print(f"bootstrap.servers = {BOOTSTRAP_SERVERS}")
    print(f"topic             = {TOPIC_NAME}")
    print(f"interval          = {INTERVAL_SECONDS}s")
    print("Press Ctrl+C to stop.\n")

    while running:
        event = create_order_event()

        key = event["order_id"]
        value = json.dumps(event, ensure_ascii=False).encode("utf-8")

        producer.produce(
            topic=TOPIC_NAME,
            key=key,
            value=value,
            callback=delivery_report,
        )

        producer.poll(0)

        print(f"[SEND] {json.dumps(event, ensure_ascii=False)}")

        time.sleep(INTERVAL_SECONDS)

    producer.flush(timeout=10)
    print("Producer stopped.")


if __name__ == "__main__":
    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    try:
        main()
    except KeyboardInterrupt:
        shutdown(None, None)
    except Exception as e:
        print(f"[FATAL] {e}")
        sys.exit(1)
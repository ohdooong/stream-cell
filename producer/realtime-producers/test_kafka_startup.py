import threading
import unittest
from types import SimpleNamespace
from unittest.mock import Mock

from kafka_startup import wait_for_topics


def metadata(leader=1):
    partition = SimpleNamespace(id=0, leader=leader, error=None)
    return SimpleNamespace(topics={"gas-detection-events": SimpleNamespace(error=None, partitions={0: partition})})


class StartupTest(unittest.TestCase):
    def test_missing_topic_explains_how_to_create_it(self):
        admin = Mock()
        admin.list_topics.return_value = SimpleNamespace(topics={})
        with self.assertRaisesRegex(RuntimeError, "gas-detection-events.*--create-topics"):
            wait_for_topics(admin, ["gas-detection-events"], 1, threading.Event())

    def test_new_topic_waits_for_metadata_and_leader(self):
        admin = Mock()
        admin.list_topics.side_effect = [SimpleNamespace(topics={}), metadata(-1), metadata()]
        stopped = Mock()
        stopped.is_set.return_value = False
        wait_for_topics(admin, ["gas-detection-events"], 1, stopped, allow_missing=True)
        self.assertEqual(admin.list_topics.call_count, 3)

    def test_missing_leader_times_out_with_topic_name(self):
        admin = Mock()
        admin.list_topics.return_value = metadata(-1)
        with self.assertRaisesRegex(RuntimeError, r"gas-detection-events\[0\]: no leader"):
            wait_for_topics(admin, ["gas-detection-events"], 0.01, threading.Event())

    def test_unreachable_broker_keeps_failure_reason(self):
        admin = Mock()
        admin.list_topics.side_effect = RuntimeError("all brokers down")
        with self.assertRaisesRegex(RuntimeError, "all brokers down"):
            wait_for_topics(admin, ["gas-detection-events"], 0.01, threading.Event())

    def test_interrupt_stops_startup(self):
        stopped = threading.Event()
        stopped.set()
        with self.assertRaises(InterruptedError):
            wait_for_topics(Mock(), ["gas-detection-events"], 30, stopped)


if __name__ == "__main__":
    unittest.main()

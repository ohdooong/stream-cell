import json
import random
import subprocess
import sys
import unittest
from datetime import datetime
from pathlib import Path
from unittest.mock import patch

from events import KINDS, create_event


class EventsTest(unittest.TestCase):
    def test_scenarios_and_anomaly_modes(self):
        for kind, (_, key) in KINDS.items():
            for rate in (0, 1):
                with self.subTest(kind=kind, rate=rate):
                    event = create_event(kind, random.Random(42), rate)
                    self.assertEqual(event["is_anomaly"], bool(rate))
                    self.assertTrue(event[key])
                    self.assertRegex(event["event_time"], r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$")
                    datetime.strptime(event["event_time"], "%Y-%m-%d %H:%M:%S")
                    json.dumps(event, allow_nan=False)
                    if kind == "energy":
                        self.assertAlmostEqual(event["active_power_kw"], event["voltage_v"] * event["current_a"] * event["power_factor"] / 1000, places=3)
                    if kind in ("gas", "fire"):
                        self.assertEqual(event["alarm_active"], bool(rate))

    def test_all_scenarios_use_orders_event_time_format(self):
        # Orders uses local wall time with second precision, including on day boundaries.
        for now, expected in [
            (datetime(2026, 10, 19, 0, 0, 0, 123456), "2026-10-19 00:00:00"),
            (datetime(2026, 12, 31, 23, 59, 59, 999999), "2026-12-31 23:59:59"),
        ]:
            with patch("events.datetime") as clock:
                clock.now.return_value = now
                for kind in KINDS:
                    with self.subTest(kind=kind, expected=expected):
                        self.assertEqual(create_event(kind, random.Random(42))["event_time"], expected)

    def test_all_cli_routes_and_counts(self):
        script = Path(__file__).with_name("producer.py")
        result = subprocess.run([sys.executable, str(script), "--dry-run", "--count", "2", "--interval", "0.001"], capture_output=True, text=True, check=True)
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(rows), 20)
        for row in rows:
            self.assertRegex(row["value"]["event_time"], r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$")
        for kind, (topic, key) in KINDS.items():
            matches = [row for row in rows if row["topic"] == topic]
            self.assertEqual(len(matches), 2)
            self.assertTrue(all(row["key"] == row["value"][key] for row in matches))

    def test_invalid_options(self):
        script = Path(__file__).with_name("producer.py")
        for option, value in [("--interval", "nan"), ("--count", "-1"), ("--anomaly-rate", "1.1")]:
            result = subprocess.run([sys.executable, str(script), "--dry-run", option, value], capture_output=True)
            self.assertEqual(result.returncode, 2)


if __name__ == "__main__":
    unittest.main()

import json
import random
import subprocess
import sys
import unittest
from pathlib import Path

from events import KINDS, create_event


class EventsTest(unittest.TestCase):
    def test_scenarios_and_anomaly_modes(self):
        for kind, (_, key) in KINDS.items():
            for rate in (0, 1):
                with self.subTest(kind=kind, rate=rate):
                    event = create_event(kind, random.Random(42), rate)
                    self.assertEqual(event["is_anomaly"], bool(rate))
                    self.assertTrue(event[key])
                    self.assertTrue(event["event_time"].endswith("Z"))
                    json.dumps(event, allow_nan=False)
                    if kind == "energy":
                        self.assertAlmostEqual(event["active_power_kw"], event["voltage_v"] * event["current_a"] * event["power_factor"] / 1000, places=3)
                    if kind in ("gas", "fire"):
                        self.assertEqual(event["alarm_active"], bool(rate))

    def test_all_cli_routes_and_counts(self):
        script = Path(__file__).with_name("producer.py")
        result = subprocess.run([sys.executable, str(script), "--dry-run", "--count", "2", "--interval", "0.001"], capture_output=True, text=True, check=True)
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(rows), 20)
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

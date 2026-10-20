"""Check event field names against Flink 1.19 and keep schema artifacts in sync."""
import json
import random
import re
import unittest
from pathlib import Path

from events import KINDS, create_event


ROOT = Path(__file__).parent


class SqlCompatibilityTest(unittest.TestCase):
    def test_all_event_fields_avoid_flink_reserved_keywords(self):
        snapshot = ROOT / "testdata" / "flink-1.19-reserved-keywords.txt"
        keywords = {
            line.strip()
            for line in snapshot.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#")
        }
        self.assertTrue({"METHOD", "PATH", "LEVEL", "ACTION"}.issubset(keywords))
        for kind in KINDS:
            for rate in (0, 1):
                with self.subTest(kind=kind, rate=rate):
                    event = create_event(kind, random.Random(42), rate)
                    conflicts = {field for field in event if field.upper() in keywords}
                    self.assertFalse(conflicts, f"{kind}: reserved field names {conflicts}")

    def test_schema_artifacts_match_generated_events(self):
        documentation = (ROOT / "SCHEMAS.md").read_text(encoding="utf-8")
        blocks = re.findall(
            r"Producer: `([^`]+)`.*?```json\n(.*?)\n```", documentation, re.S
        )
        documented = {kind: json.loads(block) for kind, block in blocks}
        self.assertEqual(set(documented), set(KINDS))
        json_types = {str: "string", int: "integer", bool: "boolean", type(None): "null"}
        display_types = {"string": "STRING", "integer": "INT", "boolean": "BOOLEAN", "null": "NULL"}
        for kind in KINDS:
            schema = json.loads((ROOT / "schemas" / f"{kind}.schema.json").read_text(encoding="utf-8"))
            properties = schema["properties"]
            with self.subTest(kind=kind):
                self.assertEqual(set(properties), set(schema["required"]))
                self.assertEqual(set(properties), set(documented[kind]))
                for field, spec in properties.items():
                    types = spec["type"] if isinstance(spec["type"], list) else [spec["type"]]
                    display = "TIMESTAMP(3)" if field == "event_time" else " | ".join(display_types[t] for t in types)
                    self.assertEqual(documented[kind][field], display, field)
            for rate in (0, 1):
                with self.subTest(kind=kind, rate=rate):
                    event = create_event(kind, random.Random(42), rate)
                    self.assertEqual(set(event), set(properties))
                    for field, value in event.items():
                        declared = properties[field]["type"]
                        allowed = declared if isinstance(declared, list) else [declared]
                        self.assertIn(json_types[type(value)], allowed, field)


if __name__ == "__main__":
    unittest.main()

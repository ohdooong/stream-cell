import base64
from copy import deepcopy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from preflight import validate_config


def valid_config():
    return {"services": {
        "backend": {"image": "streamcell/backend:20261016-1", "environment": {
            "POSTGRES_URL": "jdbc:postgresql://postgres:5432/streamcell",
            "FLINK_JDBC_URL": "jdbc:postgresql://postgres:5432/streamcell",
            "POSTGRES_USERNAME": "streamcell", "POSTGRES_PASSWORD": "test-db-secret",
            "KAFKA_BOOTSTRAP_SERVERS": "kafka-1:9092,kafka-2:9092",
            "FLINK_JOB_MANAGER_URL": "http://flink-jobmanager:8081",
            "FLINK_SQL_GATEWAY": "http://flink-sql-gateway:8083",
            "ATLAS_URL": "https://atlas.internal", "ATLAS_AGENT_ID": "test-agent",
            "ATLAS_API_KEY": "test-api-key",
            "JWT_SECRET": base64.b64encode(bytes(range(64))).decode("ascii"),
        }},
        "frontend": {"image": "streamcell/frontend:20261016-1"},
        "caddy": {"environment": {"APP_DOMAIN": "streamcell.company.test", "ACME_EMAIL": "ops@company.test"}},
    }}


class DeploymentValidationTests(unittest.TestCase):
    def test_existing_docker_and_remote_service_configurations(self):
        config = valid_config()
        self.assertEqual(validate_config(config), [])
        del config["services"]["caddy"]
        config["services"]["backend"]["environment"]["POSTGRES_URL"] = "jdbc:postgresql://10.20.0.5:5432/streamcell?sslmode=require"
        self.assertEqual(validate_config(config), [])

    def test_invalid_jwt_keys_are_rejected_without_disclosing_them(self):
        for key in ("not-base64-private", base64.b64encode(b"short-private").decode("ascii"), ""):
            with self.subTest(key_length=len(key)):
                config = valid_config()
                config["services"]["backend"]["environment"]["JWT_SECRET"] = key
                errors = validate_config(config)
                self.assertTrue(any("JWT_SECRET" in error for error in errors))
                if key:
                    self.assertNotIn(key, "\n".join(errors))

    def test_container_loopback_and_development_jdbc_driver_are_rejected(self):
        for value in ("jdbc:log4jdbc:postgresql://postgres:5432/streamcell", "jdbc:postgresql://localhost:5432/streamcell", "jdbc:postgresql://postgres:bad/streamcell"):
            config = valid_config()
            config["services"]["backend"]["environment"]["POSTGRES_URL"] = value
            self.assertTrue(any("POSTGRES_URL" in error for error in validate_config(config)))

    def test_kafka_requires_addresses_usable_by_containers(self):
        for value in ("http://kafka:9092", "localhost:19092", "kafka", "kafka:70000", "kafka:9092,"):
            config = valid_config()
            config["services"]["backend"]["environment"]["KAFKA_BOOTSTRAP_SERVERS"] = value
            self.assertTrue(any("KAFKA_BOOTSTRAP_SERVERS" in error for error in validate_config(config)))

    def test_placeholder_credentials_and_public_domain_are_rejected(self):
        config = valid_config()
        config["services"]["backend"]["environment"]["POSTGRES_PASSWORD"] = "replace-with-existing-db-password"
        config["services"]["caddy"]["environment"]["APP_DOMAIN"] = "streamcell.example.com"
        errors = validate_config(config)
        self.assertTrue(any("POSTGRES_PASSWORD" in error for error in errors))
        self.assertTrue(any("APP_DOMAIN" in error for error in errors))

    def test_rollback_requires_a_named_release(self):
        for tag in ("latest", "replace-with-release-tag", "invalid/tag"):
            config = deepcopy(valid_config())
            config["services"]["backend"]["image"] = "streamcell/backend:" + tag
            self.assertTrue(any("IMAGE_TAG" in error for error in validate_config(config)))


if __name__ == "__main__":
    unittest.main()

"""Exercise production images against an isolated temporary DB; never use real infrastructure."""
import argparse
import base64
from http.client import HTTPConnection
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import uuid

DIRECTORY = Path(__file__).resolve().parents[1]
ROOT = DIRECTORY.parents[1]


def run(command, **kwargs):
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", **kwargs)
    if result.returncode:
        raise RuntimeError("Isolated validation command failed: " + result.stderr)
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend-image", default="streamcell-deployment-validation-backend")
    parser.add_argument("--frontend-image", default="streamcell-deployment-validation-frontend")
    parser.add_argument("--schema-only", action="store_true", help="Only test DDL, schema gate and optional migration")
    args = parser.parse_args()
    project = "streamcell-smoke-" + uuid.uuid4().hex[:10]
    network = project + "-infra"
    postgres = project + "-postgres"
    prefix = "streamcell-smoke"
    tag = project
    environment = os.environ.copy()
    template = (DIRECTORY / ".env.example").read_text(encoding="utf-8")
    for line in template.splitlines():
        if line and not line.startswith("#") and "=" in line:
            environment.pop(line.split("=", 1)[0], None)
    environment.pop("COMPOSE_PROFILES", None)
    # Child Python utilities print Unicode paths consistently on Windows too.
    environment["PYTHONUTF8"] = "1"
    environment["PYTHONIOENCODING"] = "utf-8"
    values = {
        "COMPOSE_FILE": "compose.yaml:compose.infra.yaml",
        "COMPOSE_PATH_SEPARATOR": ":", "COMPOSE_PROJECT_NAME": project,
        "IMAGE_PREFIX": prefix, "IMAGE_TAG": tag, "INFRA_NETWORK": network,
        "FRONTEND_BIND_ADDRESS": "127.0.0.1", "FRONTEND_HTTP_PORT": "0",
        "POSTGRES_URL": "jdbc:postgresql://postgres:5432/streamcell",
        "POSTGRES_USERNAME": "streamcell", "POSTGRES_PASSWORD": "isolated-smoke-db-password",
        "FLINK_JDBC_URL": "jdbc:postgresql://postgres:5432/streamcell",
        # Only startup/login/proxy are tested; no real broker/Flink/Atlas is contacted.
        "KAFKA_BOOTSTRAP_SERVERS": "postgres:9092",
        "FLINK_JOB_MANAGER_URL": "http://postgres:8081",
        "FLINK_SQL_GATEWAY": "http://postgres:8083",
        "ATLAS_URL": "http://postgres:8080", "ATLAS_AGENT_ID": "isolated-smoke-agent",
        "ATLAS_API_KEY": "isolated-smoke-api-key",
        "JWT_SECRET": base64.b64encode(os.urandom(64)).decode("ascii"),
    }
    network_created = False
    postgres_created = False
    app_attempted = False
    with tempfile.TemporaryDirectory(prefix=".validation-smoke-", dir=DIRECTORY) as temporary:
        override = Path(temporary) / "smoke.yaml"
        override.write_text("services:\n  backend:\n    environment:\n      LOGGING_LEVEL_ORG_APACHE_KAFKA: ERROR\n", encoding="utf-8")
        values["COMPOSE_FILE"] += ":" + override.relative_to(DIRECTORY).as_posix()
        env_file = Path(temporary) / ".env"
        env_file.write_text("\n".join(f"{key}={value}" for key, value in values.items()) + "\n", encoding="utf-8")
        compose = ["docker", "compose", "--project-directory", str(DIRECTORY), "--env-file", str(env_file)]

        def app(command):
            return run(compose + command, cwd=DIRECTORY, env=environment)

        def request(path, body=None, token=None):
            command = ["exec", "-T", "backend", "curl", "--silent", "--show-error", "--max-time", "10", "-w", "\n%{http_code}"]
            if body is not None:
                command += ["-H", "Content-Type: application/json", "--data", json.dumps(body)]
            if token:
                command += ["-H", "Authorization: Bearer " + token]
            result = app(command + ["http://frontend:8080" + path])
            content, status = result.rsplit("\n", 1)
            return int(status), content

        try:
            run(["docker", "tag", args.backend_image, f"{prefix}/backend:{tag}"])
            run(["docker", "tag", args.frontend_image, f"{prefix}/frontend:{tag}"])
            run(["docker", "network", "create", "--label", "streamcell.smoke=" + project, network])
            network_created = True
            run(["docker", "run", "-d", "--name", postgres, "--network", network, "--network-alias", "postgres",
                 "--label", "streamcell.smoke=" + project, "--tmpfs", "/var/lib/postgresql/data:rw,size=512m",
                 "-e", "POSTGRES_DB=streamcell", "-e", "POSTGRES_USER=streamcell",
                 "-e", "POSTGRES_PASSWORD=isolated-smoke-db-password", "postgres:16-alpine"])
            postgres_created = True
            for _ in range(60):
                result = subprocess.run(["docker", "exec", postgres, "pg_isready", "-h", "127.0.0.1", "-U", "streamcell", "-d", "streamcell"], capture_output=True)
                if result.returncode == 0:
                    break
                time.sleep(1)
            else:
                raise RuntimeError("Temporary database did not start")

            # Keep production-like DDL; exclude the development plaintext accounts/topics.
            schema = (ROOT / "sql/ddl/V2__init_schema.sql").read_text(encoding="utf-8").split("INSERT INTO web.roles", 1)[0]
            run(["docker", "exec", "-i", postgres, "psql", "-v", "ON_ERROR_STOP=1", "-U", "streamcell", "-d", "streamcell"], input=schema)
            run(["docker", "exec", "-i", postgres, "psql", "-U", "streamcell", "-d", "streamcell"], input=(DIRECTORY / "check-schema.sql").read_text(encoding="utf-8"))
            # Prove a legacy missing key is detected and the optional migration repairs it.
            psql = ["docker", "exec", "-i", postgres, "psql", "-v", "ON_ERROR_STOP=1", "-U", "streamcell", "-d", "streamcell"]
            run(psql, input="ALTER TABLE platform.ai_config DROP CONSTRAINT uk_ai_config_pipeline_topic;")
            missing_key = subprocess.run(psql, input=(DIRECTORY / "check-schema.sql").read_text(encoding="utf-8"), capture_output=True, text=True, encoding="utf-8")
            assert missing_key.returncode != 0, "Schema gate must catch a missing AI_SQL conflict key"
            run(psql, input=(DIRECTORY / "migrations/001-ai-config-unique.sql").read_text(encoding="utf-8"))
            run(psql, input=(DIRECTORY / "check-schema.sql").read_text(encoding="utf-8"))
            print("OK: schema gate detects a missing key; optional migration repairs it", flush=True)
            if args.schema_only:
                return
            password_hash = run(["docker", "exec", "-i", postgres, "psql", "-At", "-v", "ON_ERROR_STOP=1", "-U", "streamcell", "-d", "streamcell"],
                                input="CREATE EXTENSION pgcrypto;\nSELECT crypt('isolated-login-password', gen_salt('bf', 12));\n").splitlines()[-1]
            bootstrap_env = environment.copy()
            bootstrap_env.update({"ADMIN_LOGIN_ID": "smoke-admin", "ADMIN_EMAIL": "smoke@company.test", "ADMIN_PASSWORD_HASH": password_hash})
            bootstrap_command = ["docker", "exec", "-i", "-e", "ADMIN_LOGIN_ID", "-e", "ADMIN_EMAIL", "-e", "ADMIN_PASSWORD_HASH",
                                 postgres, "psql", "-U", "streamcell", "-d", "streamcell"]
            bootstrap_sql = (DIRECTORY / "bootstrap-admin.sql").read_text(encoding="utf-8")
            run(bootstrap_command, input=bootstrap_sql, env=bootstrap_env)
            duplicate = subprocess.run(bootstrap_command, input=bootstrap_sql, env=bootstrap_env, capture_output=True, text=True, encoding="utf-8")
            assert duplicate.returncode != 0, "Bootstrap must not reset an existing administrator"
            print("OK: V2 schema and BCrypt administrator bootstrap", flush=True)

            app_attempted = True
            app(["up", "-d", "--no-build", "--wait", "--wait-timeout", "180"])
            published = app(["port", "frontend", "8080"]).splitlines()[0]
            host, port = published.rsplit(":", 1)
            connection = HTTPConnection(host, int(port), timeout=10)
            try:
                connection.request("GET", "/healthz")
                response = connection.getresponse()
                assert response.status == 200 and response.read().strip() == b"ok"
            finally:
                connection.close()
            print("OK: direct HTTP access through the published frontend port", flush=True)
            assert request("/healthz")[0] == 200
            assert request("/actuator/health")[0] == 404
            assert request("/api/v1/web/user/items")[0] in (401, 403)
            assert request("/pipelines/42")[0] == 200, "SPA deep links must load"
            assert request("/assets/missing.js")[0] == 404
            status, content = request("/api/v1/web/auth/login", {"loginId": "smoke-admin", "password": "isolated-login-password"})
            assert status == 200, f"Login failed with HTTP {status}"
            token = json.loads(content)["body"]["accessToken"]
            status, content = request("/api/v1/web/user/items", token=token)
            assert status == 200 and "smoke-admin" in content
            logs = app(["logs", "--no-color", "backend"])
            assert values["ATLAS_API_KEY"] not in logs and token not in logs
            print("OK: readiness, login, protected API, SPA routes and private actuator", flush=True)

            app(["exec", "-T", "backend", "sh", "-c", "printf persisted > /data/uploads/smoke-persistence.txt"])
            app(["up", "-d", "--no-build", "--force-recreate", "--wait", "--wait-timeout", "180", "backend"])
            assert app(["exec", "-T", "backend", "cat", "/data/uploads/smoke-persistence.txt"]) == "persisted"
            for _ in range(20):
                if request("/api/v1/web/user/items", token=token)[0] == 200:
                    break
                time.sleep(1)
            else:
                raise RuntimeError("Proxy did not recover after backend recreation")
            print("OK: upload volume and API proxy survive backend recreation", flush=True)

            run([sys.executable, str(DIRECTORY / "backup.py"), "--env-file", str(env_file), "--output", str(Path(temporary) / "backups")], cwd=DIRECTORY, env=environment)
            backup = next((Path(temporary) / "backups").iterdir())
            assert (backup / "database.dump").stat().st_size > 0
            assert (backup / "uploads.tar.gz").stat().st_size > 0
            assert (backup / "manifest.json").is_file()
            assert not list(backup.glob("*.partial"))
            assert request("/api/v1/web/user/items", token=token)[0] == 200
            print("OK: database/upload backup and API resume", flush=True)
            print("Smoke test passed. Kafka, Flink, Atlas and remote HTTP access were not exercised.", flush=True)
        except Exception:
            if app_attempted:
                # Only synthetic secrets and test data exist in this isolated stack.
                result = subprocess.run(compose + ["logs", "--no-color", "--tail", "60", "backend"], cwd=DIRECTORY, env=environment, capture_output=True, text=True, encoding="utf-8")
                print(result.stdout, file=sys.stderr)
            raise
        finally:
            if app_attempted:
                subprocess.run(compose + ["down", "--volumes", "--remove-orphans"], cwd=DIRECTORY, env=environment, capture_output=True)
            if postgres_created:
                subprocess.run(["docker", "rm", "--force", postgres], capture_output=True)
            if network_created:
                subprocess.run(["docker", "network", "rm", network], capture_output=True)
            for kind in ("backend", "frontend"):
                subprocess.run(["docker", "image", "rm", f"{prefix}/{kind}:{tag}"], capture_output=True)


if __name__ == "__main__":
    main()

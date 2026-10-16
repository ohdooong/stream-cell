"""Check resolved Compose configuration without printing credentials."""
import argparse
import base64
import binascii
import json
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import urlsplit

DIRECTORY = Path(__file__).resolve().parent


def compose_command(env_file):
    return ["docker", "compose", "--project-directory", str(DIRECTORY), "--env-file", str(env_file)]


def load_config(env_file):
    result = subprocess.run(
        compose_command(env_file) + ["config", "--format", "json"],
        cwd=DIRECTORY, capture_output=True, text=True, encoding="utf-8",
    )
    if result.returncode:
        # Compose stderr may contain interpolated values, so keep it private.
        raise ValueError("Compose could not resolve the configuration. Check COMPOSE_FILE and required .env variables.")
    return json.loads(result.stdout)


def validate_config(config):
    errors = []
    backend = config.get("services", {}).get("backend", {})
    environment = backend.get("environment", {})

    def required(name, values=environment):
        value = str(values.get(name) or "").strip()
        if not value or "replace-with" in value.lower() or value.lower() in {"changeme", "password"}:
            errors.append(f"{name}: supply the real deployment value.")
        return value

    def service_url(name, jdbc=False):
        value = required(name)
        if jdbc and not value.startswith("jdbc:postgresql://"):
            errors.append(f"{name}: use jdbc:postgresql:// with the production PostgreSQL driver.")
            return
        try:
            parsed = urlsplit(value.removeprefix("jdbc:") if jdbc else value)
            if not parsed.hostname or parsed.scheme not in ({"postgresql"} if jdbc else {"http", "https"}):
                raise ValueError()
            if parsed.hostname in {"localhost", "127.0.0.1", "::1"}:
                errors.append(f"{name}: loopback refers to the container; use a reachable service hostname or IP.")
            if jdbc and not parsed.path.strip("/"):
                errors.append(f"{name}: specify a database name.")
            if parsed.username or parsed.password:
                errors.append(f"{name}: use separate credential settings, not credentials in the URL.")
            _ = parsed.port
        except ValueError:
            errors.append(f"{name}: invalid service URL.")

    for name in ("POSTGRES_URL", "FLINK_JDBC_URL"):
        service_url(name, jdbc=True)
    for name in ("FLINK_JOB_MANAGER_URL", "FLINK_SQL_GATEWAY", "ATLAS_URL"):
        service_url(name)
    for name in ("POSTGRES_USERNAME", "POSTGRES_PASSWORD", "ATLAS_AGENT_ID", "ATLAS_API_KEY"):
        required(name)

    secret = required("JWT_SECRET")
    try:
        if len(base64.b64decode(secret, validate=True)) < 32:
            raise ValueError()
    except (ValueError, binascii.Error):
        errors.append("JWT_SECRET: use standard Base64 encoding of at least 32 random bytes (64 recommended).")

    brokers = required("KAFKA_BOOTSTRAP_SERVERS")
    for broker in brokers.split(","):
        try:
            parsed = urlsplit("//" + broker.strip())
            if "://" in broker or not parsed.hostname or not parsed.port or parsed.path:
                raise ValueError()
            if parsed.hostname in {"localhost", "127.0.0.1", "::1"}:
                raise ValueError()
        except ValueError:
            errors.append("KAFKA_BOOTSTRAP_SERVERS: use reachable host:port entries without http:// or loopback.")
            break

    for name in ("backend", "frontend"):
        image = config.get("services", {}).get(name, {}).get("image", "")
        tag = image.rsplit(":", 1)[-1]
        if ":" not in image or tag == "latest" or "replace-with" in tag or not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}", tag):
            errors.append(f"{name}: set a valid, unique IMAGE_TAG; do not use latest.")

    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=DIRECTORY / ".env")
    parser.add_argument("--config-only", action="store_true", help="Skip Docker daemon/network checks")
    args = parser.parse_args()
    env_file = args.env_file.resolve()
    if not env_file.is_file():
        raise SystemExit("Missing .env. Run python3 infra/production/init-env.py, then fill the deployment values.")
    try:
        config = load_config(env_file)
        errors = validate_config(config)
        if not args.config_only:
            result = subprocess.run(["docker", "info"], capture_output=True)
            if result.returncode:
                errors.append("Docker engine is unavailable. Start Docker and check this user's Docker access.")
            else:
                for network in config.get("networks", {}).values():
                    if network.get("external"):
                        result = subprocess.run(["docker", "network", "inspect", network["name"]], capture_output=True)
                        if result.returncode:
                            errors.append("The existing INFRA_NETWORK does not exist on this Docker host.")
        if errors:
            print("Deployment checks failed:\n" + "\n".join(f"- {error}" for error in errors), file=sys.stderr)
            return 1
        print("Deployment configuration passed." + (" Docker runtime checks were skipped." if args.config_only else " Docker engine and external networks are available."))
        print("Verify existing DB schema, credentials, Kafka advertised listeners, Flink/Atlas reachability and DNS on the server.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print("Deployment checks could not run: " + (str(error) if isinstance(error, ValueError) else type(error).__name__), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())

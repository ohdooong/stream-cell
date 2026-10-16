"""Resolve each deployment variant using fake values; no Docker daemon required."""
import base64
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

DIRECTORY = Path(__file__).resolve().parents[1]


def main():
    template = (DIRECTORY / ".env.example").read_text(encoding="utf-8")
    replacements = {
        "replace-with-existing-db-password": "validation-db-secret",
        "replace-with-atlas-url": "http://atlas.company.test:8080",
        "replace-with-atlas-agent-id": "validation-agent",
        "replace-with-atlas-api-key": "validation-api-key",
        "replace-with-base64-jwt-secret": base64.b64encode(bytes(range(64))).decode("ascii"),
    }
    for old, new in replacements.items():
        template = template.replace(old, new)
    environment = os.environ.copy()
    names = {line.split("=", 1)[0] for line in template.splitlines() if line and not line.startswith("#") and "=" in line}
    for name in names | {"COMPOSE_PROFILES"}:
        environment.pop(name, None)
    with tempfile.TemporaryDirectory(prefix=".validation-", dir=DIRECTORY) as temporary:
        env_file = Path(temporary) / ".env"
        for local_infra in (False, True):
            for bind_address in ("0.0.0.0", "127.0.0.1"):
                files = ["compose.yaml"]
                if local_infra:
                    files.append("compose.infra.yaml")
                lines = template.splitlines()
                lines = ["COMPOSE_FILE=" + ":".join(files) if line.startswith("COMPOSE_FILE=") else line for line in lines]
                lines = ["FRONTEND_BIND_ADDRESS=" + bind_address if line.startswith("FRONTEND_BIND_ADDRESS=") else line for line in lines]
                env_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
                command = ["docker", "compose", "--project-directory", str(DIRECTORY), "--env-file", str(env_file)]
                result = subprocess.run(command + ["config", "--format", "json"], cwd=DIRECTORY, capture_output=True, text=True, encoding="utf-8", env=environment)
                if result.returncode:
                    raise RuntimeError("Compose resolution failed for " + ":".join(files) + ": " + result.stderr)
                config = json.loads(result.stdout)
                backend = config["services"]["backend"]
                frontend = config["services"]["frontend"]
                assert not backend.get("ports"), "Backend must stay private"
                assert frontend["ports"][0]["host_ip"] == bind_address
                assert backend["read_only"] and frontend["read_only"]
                assert backend["environment"]["SPRING_PROFILES_ACTIVE"] == "prod"
                assert set(config["services"]) == {"backend", "frontend"}
                assert not any(int(port["target"]) == 443 for service in config["services"].values() for port in service.get("ports", []))
                assert ("infrastructure" in backend["networks"]) == local_infra
                result = subprocess.run([sys.executable, str(DIRECTORY / "preflight.py"), "--env-file", str(env_file), "--config-only"], cwd=DIRECTORY, capture_output=True, text=True, encoding="utf-8", env=environment)
                if result.returncode:
                    raise RuntimeError("Preflight rejected valid configuration for " + ":".join(files))
                print("OK: " + ":".join(files) + " HTTP bind=" + bind_address)


if __name__ == "__main__":
    main()

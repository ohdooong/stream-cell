"""Back up the existing PostgreSQL database and uploaded JARs during API maintenance."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import parse_qs, unquote, urlsplit

from preflight import DIRECTORY, compose_command, load_config, validate_config


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=DIRECTORY / ".env")
    parser.add_argument("--output", type=Path, default=DIRECTORY / "backups")
    parser.add_argument("--postgres-image", default="postgres:16-alpine", help="Use the existing server major version or newer")
    args = parser.parse_args()
    os.umask(0o077)
    env_file = args.env_file.resolve()
    compose = compose_command(env_file)
    config = load_config(env_file)
    errors = validate_config(config)
    if errors:
        raise SystemExit("Fix deployment settings with preflight.py before backup.")
    backend = config["services"]["backend"]
    environment = backend["environment"]
    db = urlsplit(environment["POSTGRES_URL"].removeprefix("jdbc:"))
    # Prefer the same network used to resolve existing infrastructure names.
    networks = config["networks"]
    network = networks.get("infrastructure", networks["app"])["name"]
    upload_volume = config["volumes"]["backend-uploads"]["name"]
    result = subprocess.run(compose + ["ps", "--status", "running", "-q", "backend"],
                            capture_output=True, text=True, check=True)
    if not result.stdout.strip():
        raise SystemExit("Backend must be running before backup; deploy/start it first.")
    # Fail before maintenance if a required utility image cannot be pulled.
    for image in (args.postgres_image, "alpine:3.22"):
        subprocess.run(["docker", "pull", image], check=True)
    subprocess.run(["docker", "volume", "inspect", upload_volume], stdout=subprocess.DEVNULL, check=True)

    destination = args.output.resolve() / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    destination.mkdir(parents=True, mode=0o700, exist_ok=False)
    pg_env = os.environ.copy()
    pg_env.update({
        "PGHOST": db.hostname,
        "PGPORT": str(db.port or 5432),
        "PGDATABASE": unquote(db.path.lstrip("/")),
        "PGUSER": environment["POSTGRES_USERNAME"],
        "PGPASSWORD": environment["POSTGRES_PASSWORD"],
        "PGSSLMODE": parse_qs(db.query).get("sslmode", ["prefer"])[0],
    })
    dump_command = ["docker", "run", "--rm", "--network", network]
    for name in ("PGHOST", "PGPORT", "PGDATABASE", "PGUSER", "PGPASSWORD", "PGSSLMODE"):
        dump_command += ["--env", name]
    dump_command += [args.postgres_image, "pg_dump", "--format=custom", "--no-owner", "--no-acl"]
    stopped = False
    try:
        print("Stopping the API briefly so uploaded files and metadata remain consistent.", flush=True)
        # Resume even if interruption occurs while Compose is stopping the service.
        stopped = True
        subprocess.run(compose + ["stop", "backend"], check=True)
        with (destination / "database.dump.partial").open("xb") as output:
            subprocess.run(dump_command, env=pg_env, stdout=output, check=True)
        with (destination / "uploads.tar.gz.partial").open("xb") as output:
            subprocess.run([
                "docker", "run", "--rm", "--network", "none", "--user", "10001:10001",
                "--mount", f"type=volume,src={upload_volume},dst=/source,readonly",
                "alpine:3.22", "tar", "-C", "/source", "-czf", "-", ".",
            ], stdout=output, check=True)
        for name in ("database.dump", "uploads.tar.gz"):
            (destination / (name + ".partial")).rename(destination / name)
        (destination / "manifest.json").write_text(json.dumps({
            "project": config["name"], "backend_image": backend["image"],
            "frontend_image": config["services"]["frontend"]["image"],
            "upload_volume": upload_volume, "postgres_client_image": args.postgres_image,
        }, indent=2) + "\n", encoding="utf-8")
        print(f"Backup saved to {destination}. Copy it to a protected off-server location.")
    finally:
        if stopped:
            subprocess.run(compose + ["start", "--wait", "--wait-timeout", "180", "backend"], check=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.CalledProcessError, KeyboardInterrupt) as error:
        # Do not print commands/environment that could contain database information.
        print(f"Backup did not complete ({type(error).__name__}). Check service status and any .partial files.", file=sys.stderr)
        sys.exit(1)

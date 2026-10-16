"""Create a private deployment env file; never overwrite an existing one."""
import os
from pathlib import Path
import secrets
import base64


def main():
    directory = Path(__file__).resolve().parent
    destination = directory / ".env"
    template = (directory / ".env.example").read_text(encoding="utf-8")
    template = template.replace(
        "replace-with-base64-jwt-secret",
        base64.b64encode(secrets.token_bytes(64)).decode("ascii"),
    )
    try:
        fd = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        raise SystemExit(".env already exists; it was not changed.")
    with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
        stream.write(template)
    print("Created infra/production/.env with a new JWT key. Fill the existing service credentials and release tag.")


if __name__ == "__main__":
    main()

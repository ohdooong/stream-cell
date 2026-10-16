"""Package current application sources and deployment tools without local secrets/caches."""
import argparse
import hashlib
import io
import os
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[2]
INPUTS = [
    ROOT / "frontend", ROOT / "backend/streamcell-api/streamcell",
    ROOT / "infra/production", ROOT / "sql/ddl",
    ROOT / ".gitattributes", ROOT / ".gitignore", ROOT / "README.md",
]
SKIP_DIRECTORIES = {".git", ".gradle", ".idea", ".aws", ".ssh", ".codex", ".agents", "node_modules", "dist", "build", "logs", "backups", "docs", "__pycache__"}


def include(path):
    relative = path.relative_to(ROOT)
    if path.is_symlink() or any(part in SKIP_DIRECTORIES or part.startswith((".validation-", ".release-")) for part in relative.parts):
        return False
    if path.name.startswith(".env") and path.name != ".env.example":
        return False
    if path.name == "AGENTS.md" or path.name.endswith("-api-key.txt"):
        return False
    if path.suffix in {".pyc", ".pyo", ".tsbuildinfo", ".iml", ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore"}:
        return False
    if relative.as_posix() in {"frontend/vite.config.js", "frontend/vite.config.d.ts"}:
        return False
    if "src/main/generated" in relative.as_posix():
        return False
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / ".artifacts/streamcell-deploy.tar.gz")
    args = parser.parse_args()
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    files = []
    for source in INPUTS:
        if source.is_file():
            files.append(source)
            continue
        for directory, directories, names in os.walk(source, followlinks=False):
            directories[:] = [name for name in directories if include(Path(directory) / name)]
            files.extend(path for name in names if include(path := Path(directory) / name) and path.is_file())
    with tarfile.open(output, mode="x:gz") as archive:
        for path in sorted(files):
            name = "streamcell-deploy/" + path.relative_to(ROOT).as_posix()
            info = archive.gettarinfo(str(path), arcname=name)
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            data = path.read_bytes()
            if path.suffix == ".sh" or path.name == "gradlew":
                data = data.replace(b"\r\n", b"\n")
                info.mode = 0o755
            else:
                info.mode = 0o644
            info.size = len(data)
            archive.addfile(info, io.BytesIO(data))
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    checksum = output.with_name(output.name + ".sha256")
    checksum.write_text(f"{digest}  {output.name}\n", encoding="ascii")
    print(f"Packaged {len(files)} files: {output}")
    print(f"SHA256: {digest}")
    print("Includes current uncommitted application changes. Review sources before production deployment.")


if __name__ == "__main__":
    main()

#!/usr/bin/env bash
set -euo pipefail

script_path="${BASH_SOURCE[0]}"
if [[ "$script_path" == */* ]]; then
    cd -- "${script_path%/*}"
fi

# Prefer the project's virtual environment without requiring activation.
if [[ -n "${PYTHON_BIN:-}" ]]; then
    python_bin="$PYTHON_BIN"
elif [[ -x ".venv/bin/python" ]]; then
    python_bin=".venv/bin/python"
elif [[ -f ".venv/Scripts/python.exe" ]]; then
    python_bin=".venv/Scripts/python.exe"
elif command -v python3 >/dev/null 2>&1 && python3 --version >/dev/null 2>&1; then
    python_bin="python3"
elif command -v python >/dev/null 2>&1; then
    python_bin="python"
else
    echo "Python 3 is required. Install Python or set PYTHON_BIN to its executable path." >&2
    exit 1
fi

# exec forwards signals and preserves the producer's exit status.
exec "$python_bin" producer.py --kind all "$@"

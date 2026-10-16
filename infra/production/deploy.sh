#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
ENV_FILE="${ENV_FILE:-.env}"
compose=(docker compose --project-directory "$PWD" --env-file "$ENV_FILE")

usage() {
    printf '%s\n' 'Usage: bash infra/production/deploy.sh {check|build|up|deploy|status|logs|rollback TAG}'
}

check() {
    python3 preflight.py --env-file "$ENV_FILE"
}

remember_release() {
    local current
    current="$(docker compose --project-directory "$PWD" --env-file "$ENV_FILE" ps -q backend)"
    if [[ -n "$current" ]]; then
        docker inspect --format '{{.Config.Image}}' "$current" > .release-backend
        "${compose[@]}" ps -q frontend | xargs -r docker inspect --format '{{.Config.Image}}' > .release-frontend
        printf '%s\n' 'Previous image references saved to .release-backend and .release-frontend.'
    fi
}

case "${1:-}" in
    check) check ;;
    build)
        check
        "${compose[@]}" build --pull backend frontend
        ;;
    up)
        check
        remember_release
        "${compose[@]}" up -d --no-build --wait --wait-timeout 180
        "${compose[@]}" ps
        ;;
    deploy)
        check
        "${compose[@]}" build --pull backend frontend
        remember_release
        "${compose[@]}" up -d --no-build --wait --wait-timeout 180
        "${compose[@]}" ps
        ;;
    status) "${compose[@]}" ps ;;
    logs) "${compose[@]}" logs --tail=200 -f ;;
    rollback)
        [[ $# == 2 && "$2" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || { usage; exit 2; }
        export IMAGE_TAG="$2"
        check
        # Fail before stopping anything if either release image is missing.
        "${compose[@]}" config --images | while IFS= read -r image; do
            docker image inspect "$image" > /dev/null
        done
        "${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 180
        printf '%s\n' "Rollback completed. Set IMAGE_TAG=$2 in $ENV_FILE before the next normal up."
        ;;
    *) usage; exit 2 ;;
esac

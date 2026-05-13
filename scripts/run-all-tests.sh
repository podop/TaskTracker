#!/usr/bin/env bash
# Full test cycle: backend unit + integration tests, frontend unit/component
# tests, and Playwright end-to-end tests. Exits non-zero on any failure.

set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

log() {
    printf '\n\033[1;34m==>\033[0m %s\n' "$*"
}

log "Backend tests (unit + Testcontainers integration)"
mvn -B -ntp -f backend/pom.xml verify

log "Frontend unit/component tests (Vitest)"
npm --prefix frontend run test --silent

log "Frontend end-to-end tests (Playwright)"
if ! npm --prefix frontend exec -- playwright --version >/dev/null 2>&1; then
    echo "Playwright not installed; running 'npm install' first." >&2
    npm --prefix frontend install --no-audit --no-fund
fi
if [ ! -d "$HOME/.cache/ms-playwright" ]; then
    log "Installing Playwright Chromium binary"
    npm --prefix frontend run test:e2e:install
fi
npm --prefix frontend run test:e2e --silent

log "All test suites passed."

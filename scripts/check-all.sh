#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

echo "=== Verificação completa do projeto ==="
echo ""

echo "--- Backend ---"
cd "$PROJECT_ROOT/backend"
./mvnw -B -ntp verify && pass "Backend build + testes" || fail "Backend build"

echo ""
echo "--- Frontend ---"
cd "$PROJECT_ROOT/frontend"
npm run typecheck && pass "TypeScript typecheck" || fail "TypeScript typecheck"
npm run lint && pass "ESLint" || fail "ESLint"
npm run build && pass "Next.js build" || fail "Next.js build"

echo ""
echo "=== Verificação completa concluída ==="

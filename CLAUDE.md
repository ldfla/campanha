@AGENTS.md

## Visão geral do codebase

Plataforma de arrecadação com números, sorteio e prêmios, hoje em MVP no modo DEMO, com pagamento simulado. É um monólito modular com frontend separado.

**Stack**: Java 25, Spring Boot 4.1, MySQL 8.4 com Flyway, Next.js 16, React 19, TypeScript e Docker Compose.
**Estrutura**: `backend/` (pacote `la.meleva.fundraiser`), `frontend/`, `infrastructure/`, `scripts/` e `docs/`.

Para a arquitetura detalhada, veja [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).

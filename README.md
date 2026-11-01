# Plataforma de Ação entre Amigos

Aplicação web para uma **ação entre amigos** com venda de números, cobrança Pix (sandbox),
sorteio público e premiação — construída com foco em segurança, auditabilidade e integridade
financeira.

> ⚠️ **Estado: MVP/DEMO.** A plataforma roda em modo demonstração: `LIVE_OPERATIONS_ENABLED=false`,
> pagamentos por provedor **mock** e nenhuma comunicação externa real. Operação real (cobrança,
> campanha pública, premiação) permanece **bloqueada pelos gates humanos** em
> [`docs/compliance/gates-status.md`](docs/compliance/gates-status.md). Não há autorização jurídica
> ou homologação de PSP declarada.

## Stack

- **Backend:** Java 25 · Spring Boot · MySQL 8.4 · Flyway · Maven Wrapper (`backend/`).
- **Frontend:** Next.js 16 · React 19 · TypeScript (`frontend/`).
- **Infra:** Docker Compose (`infrastructure/`) — MySQL, Mailpit e object storage S3 (Garage).
- **Docs:** [`AGENTS.md`](AGENTS.md) (contrato de execução) e
  [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md) (roadmap e status).

## Rodar localmente (DEMO)

Ver o guia completo em [`docs/operations/local-mvp.md`](docs/operations/local-mvp.md). Resumo:

```bash
# 1) infraestrutura (MySQL + Mailpit)
docker compose -f infrastructure/compose.yaml up -d

# 2) backend (profile local; seed de campanhas DEMO)
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run     # http://localhost:8080/api

# 3) frontend
cd frontend && npm install && npm run dev               # http://localhost:3000
```

Object storage S3 (opcional): [`docs/operations/object-storage.md`](docs/operations/object-storage.md).

## Testes

```bash
./scripts/check-all.sh          # backend (verify) + frontend (typecheck, lint, build)
# ou individualmente:
cd backend && ./mvnw -B verify
cd frontend && npm run typecheck && npm run lint && npm run build
```

## Deploy (staging/MVP)

Stack pronta em [`infrastructure/compose.staging.yaml`](infrastructure/compose.staging.yaml)
(backend + frontend + MySQL + Garage + **Cloudflare Tunnel**), incluindo Dockerfiles:
runbook em [`docs/operations/deploy-staging.md`](docs/operations/deploy-staging.md).

## Segurança

- Modelo de ameaças, findings e relatórios: [`docs/security/`](docs/security/).
- Decisões de arquitetura: [`docs/adr/`](docs/adr/).
- Regras de escopo/segurança para agentes: [`AGENTS.md`](AGENTS.md).

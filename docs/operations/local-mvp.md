# Rodar o MVP localmente (DEMO)

> Ambiente local, **sem operação real**: `LIVE_OPERATIONS_ENABLED=false` e pagamentos por
> `MockPaymentProvider`. Nada aqui é autorização jurídica/PSP.

## Pré-requisitos
- Java 25 (`./mvnw` já vem no repo), Docker, Node/`npm` (frontend).

## 1. Subir a infraestrutura
```bash
docker compose -f infrastructure/compose.yaml up -d   # MySQL 8.4, MinIO, Mailpit
```

## 2. Backend (profile `local`)
```bash
cd backend
SURPRISE_ENCRYPTION_KEY="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
app.cpf.encryption-key="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
app.cpf.hmac-key="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
# API em http://localhost:8080/api
```
> As três chaves são **Base64 de 32 bytes** e valores de exemplo **só para local**. Sem elas, os recursos
> de surpresa e CPF ficam indisponíveis (`503`), mas o restante sobe normalmente.

O **seed local** (`LocalDemoSeed`, profile `local`) cria, de forma idempotente:
- `acao-demo-local` — campanha **DEMO** (fluxo gratuito, rankings, carga de prêmios, apuração).
- `acao-capitalizacao-local` — campanha **CAPITALIZATION_PRIZE_TITLE** (ADR-0007) com **autorização
  SUSEP sintética verificada**, **todos os gates aprovados** e **OPEN**, para demonstrar cobrança (mock).
- Usuário admin local: telefone `+5511999990000`.

## 3. Frontend
```bash
cd frontend
npm install
npm run dev    # http://localhost:3000
```

## 4. O que experimentar (via API `http://localhost:8080/api`)
- Cadastro/login: `POST /auth/register`, `POST /auth/login` (telefone E.164, ex.: `+5511999990001`).
- **DEMO (gratuito):** `POST /participations/campaigns/{id}` com `{idempotencyKey, count, entityId}`.
- **Capitalização (ADR-0007, cobrança mock):**
  - ofertas: `GET /participations/campaigns/{id}/offers`
  - checkout: `POST /participations/campaigns/{id}/checkout` `{idempotencyKey, offerId, entityId, referralCode?}`
  - confirmar: `POST /participations/campaigns/{id}/checkout/{requestPublicId}/confirm`
    (o provedor mock precisa estar `PAID` e o pedido `RECONCILED`; sem isso, `502`).
- **Carga de prêmios (público):** `GET /participations/campaigns/{id}/numbers/prize-load` e `/verify`.
- **Rankings (público):** `GET /rankings/campaigns/{id}/{PROMOTER|PARTICIPANT}`.
- **Transparência da apuração (público):** `GET /drawings/campaigns/{id}/draws/{drawPublicId}/transparency`.
- **CPF do contemplado:** `PUT /privacy/me/cpf` `{"cpf":"111.444.777-35"}`; `GET /privacy/me/cpf`.
- **LGPD:** `GET /privacy/me/export`; `POST /privacy/me/anonymize`.

## 5. Rodar os testes
```bash
cd backend && ./mvnw -B verify
```

## Limites do MVP
- Cobrança é **simulada** (mock); nenhum PSP real integrado.
- Modo capitalização é **sandbox**; a liberação real é o gate `G-LAUNCH` com PSP/documento SUSEP reais.
- Fórmula fechada da apuração e data/concurso da Federal: pendentes (D-OPEN-008/009).

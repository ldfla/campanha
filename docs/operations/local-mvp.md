# Rodar o MVP localmente (DEMO)

> Ambiente local, **sem operação real**: `LIVE_OPERATIONS_ENABLED=false` e pagamentos por
> `MockPaymentProvider`. Nada aqui é autorização jurídica/PSP.

## Pré-requisitos
- Java 25 (`./mvnw` já vem no repo), Docker, Node/`npm` (frontend).

## 1. Subir a infraestrutura
```bash
docker compose -f infrastructure/compose.yaml up -d   # MySQL 8.4 + Mailpit
```
> **Object storage é opcional** no MVP: o armazenamento privado de documentos roda **em memória por
> padrão** (`APP_STORAGE_PROVIDER=memory`). Para um bucket compatível com S3, veja
> `docs/operations/object-storage.md` (engine local free: **Garage**).

## 2. Backend (profile `local`)
```bash
cd backend
APP_STORAGE_PROVIDER=memory \
SURPRISE_ENCRYPTION_KEY="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
app.cpf.encryption-key="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
app.cpf.hmac-key="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" \
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
# API em http://localhost:8080/api
```
> As três chaves são **Base64 de 32 bytes** e valores de exemplo **só para local**. Sem `SURPRISE_ENCRYPTION_KEY`,
> o compromisso de surpresa fica indisponível; sem as chaves de CPF, `PUT/GET /privacy/me/cpf` responde `503`.
> O restante sobe normalmente.

O **seed local** (`LocalDemoSeed`, profile `local`) cria, de forma idempotente:
- `acao-demo-local` — campanha **DEMO** (`OPEN`) com beneficiário e prêmio sintético.
- `acao-capitalizacao-local` — campanha **CAPITALIZATION_PRIZE_TITLE** (ADR-0007, `OPEN`) com
  **autorização SUSEP sintética verificada**, **todos os gates aprovados**, **beneficiário** e
  **ofertas/combos** já publicados (R$ 1,00 / R$ 9,50 / R$ 87,00).
- Usuário admin local: telefone `+5511999990000` (exige MFA sandbox, código `000000`).

## 3. Frontend
```bash
cd frontend
npm install
npm run dev    # http://localhost:3000
```

## 4. O que experimentar (via API `http://localhost:8080/api`)
- Cadastro/login: `POST /auth/register`, `POST /auth/login` (telefone E.164, ex.: `+5511999990001`).
  - **Admin** (`+5511999990000`): após o login (`403 AUTH_MFA_REQUIRED`), concluir com
    `POST /auth/mfa/verify` `{"phoneNumber":"+5511999990000","code":"000000"}`.
- **DEMO (gratuito):** `POST /participations/campaigns/{id}` `{idempotencyKey, count, entityId}`.
  - `entityId` = `publicId` de um beneficiário: `GET /participations/campaigns/{id}/beneficiaries`.
- **Capitalização (ADR-0007, cobrança mock):**
  1. ofertas: `GET /participations/campaigns/{id}/offers`
  2. checkout: `POST /participations/campaigns/{id}/checkout` `{idempotencyKey, offerId, entityId}` →
     retorna `requestPublicId` e `orderPublicId` (a reserva expira em 15 min).
  3. simular pagamento (admin, profile `local`): `POST /payments/mock/orders/{orderPublicId}/settle`
     → marca a cobrança mock como `PAID` e reconcilia o pedido.
  4. confirmar (participante): `POST /participations/campaigns/{id}/checkout/{requestPublicId}/confirm`
     → atribui os números. Sem o passo 3, responde `502 PAYMENT_PROVIDER_ERROR`.
- **Carga de prêmios:** `POST /participations/campaigns/{id}/numbers/prize-load` (admin) e, depois,
  `GET .../numbers/prize-load` e `.../verify` (públicos). Antes da carga, o `GET` responde `404`.
- **Rankings (público):** `GET /rankings/campaigns/{id}/{PROMOTER|PARTICIPANT}`.
- **Transparência da apuração (público):** `GET /drawings/campaigns/{id}/draws/{drawPublicId}/transparency`.
- **CPF do contemplado:** `PUT /privacy/me/cpf` `{"cpf":"111.444.777-35"}`; `GET /privacy/me/cpf`.
- **LGPD:** `GET /privacy/me/export`; `POST /privacy/me/anonymize`.

## 5. Rodar os testes
```bash
cd backend && ./mvnw -B verify
```

## Limites do MVP
- Cobrança é **simulada** (mock); nenhum PSP real integrado. O endpoint `settle` só existe no profile `local`.
- Modo capitalização é **sandbox**; a liberação real é o gate `G-LAUNCH` com PSP/documento SUSEP reais.
- Fórmula fechada da apuração e data/concurso da Federal: pendentes (D-OPEN-008/009).

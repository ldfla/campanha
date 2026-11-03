# Runbook — DEMO de operações (modo DEMO, sem pagamento real)

Objetivo: demonstrar **ponta a ponta** — combos, reserva de 15 min, tela de pagamento (mock),
visualização dos números com **indicação de prêmio** — **sem** provedor de pagamento real.
Nada aqui move dinheiro: `LIVE_OPERATIONS_ENABLED=false` e as respostas trazem `"simulation": true`.

> **Nunca** ligue `APP_LIVE_OPERATIONS_ENABLED=true`: os adaptadores DEMO de OTP/MFA se recusam a
> iniciar e o backend **cai**.

## O que o seed cria (perfil `staging`/`local`)
- `acao-demo-local` (`DEMO`, OPEN): participação **grátis** + **carga de prêmios** (10×R$100 + 100×R$10).
- `acao-capitalizacao-local` (`CAPITALIZATION_PRIZE_TITLE`, OPEN): **combos** (1, 10, 100) para o
  fluxo de **checkout** (cobrança mock).
- Admin local: telefone `+5511999990000` (SYSTEM_ADMIN).

## 0) Pegar os IDs das campanhas semeadas
```bash
docker compose ps -q mysql >/dev/null 2>&1
docker exec $(docker compose ps -q mysql) mysql -N -ufundraiser -p"$DB_PASSWORD" fundraiser \
  -e "SELECT public_id, slug, mode, status FROM campaigns;"
```
Use o `public_id` da `acao-demo-local` (grátis) e/ou da `acao-capitalizacao-local` (checkout).
Ponha o id da campanha que a UI deve exibir em `NEXT_PUBLIC_CAMPAIGN_ID` (imagem do frontend).

## 1) Participar (DEMO, grátis)
```bash
BASE=https://SEU_HOST/api
CID=<public_id da acao-demo-local>

# Sessão (o cookie XSRF-TOKEN é obrigatório nas mutações)
curl -s -c cj.txt "$BASE/auth/me" -o /dev/null
CSRF=$(grep XSRF cj.txt | awk '{print $7}')

# Cadastro + verificação (código DEMO 000000) => sessão
curl -s -b cj.txt -c cj.txt -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF" -d '{"firstName":"Maria","surname":"Silva","phoneNumber":"+5511988880001"}'
curl -s -b cj.txt -c cj.txt -X POST "$BASE/auth/login/verify" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF" -d '{"phoneNumber":"+5511988880001","code":"000000"}'

# Beneficiário (entityId) e participação
entity=$(curl -s -b cj.txt "$BASE/participations/campaigns/$CID/beneficiaries" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -1)
curl -s -b cj.txt -X POST "$BASE/participations/campaigns/$CID" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF" -d "{\"idempotencyKey\":\"$(uuidgen | tr A-Z a-z)\",\"count\":5,\"entityId\":\"$entity\"}"
```

## 2) Comprar combos com **reserva de 15 min** (campanha de capitalização)
```bash
CC=<public_id da acao-capitalizacao-local>
curl -s -b cj.txt "$BASE/participations/campaigns/$CC/offers"                 # combos 1/10/100
# checkout => reserva (15 min) + pedido mock AWAITING_PAYMENT (retorna requestPublicId e orderPublicId)
curl -s -b cj.txt -X POST "$BASE/participations/campaigns/$CC/checkout" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d "{\"idempotencyKey\":\"$(uuidgen | tr A-Z a-z)\",\"offerId\":\"<offerId>\",\"entityId\":\"$entity\"}"
```
- Os números ficam **RESERVADOS** (lease de 15 min); o pedido fica `AWAITING_PAYMENT`.
- Tentar confirmar agora → **409 `PAYMENT_NOT_RECONCILED`** (pagamento não concluído — é o esperado).
- Se o tempo expirar, os números voltam ao estoque automaticamente.

## 3) “Como se fosse pago”, sem provedor (simular o PSP — só DEMO)
Com sessão de **SYSTEM_ADMIN** (login `+5511999990000` → `login/verify` `000000` → `mfa/verify` `000000`):
```bash
curl -s -b admin.txt -X POST "$BASE/payments/mock/orders/<orderPublicId>/settle" \
  -H "X-XSRF-TOKEN: $CSRF"           # 204: cobrança mock vira PAID e o pedido é reconciliado
```
Depois, com a sessão do comprador:
```bash
curl -s -b cj.txt -X POST "$BASE/participations/campaigns/$CC/checkout/<requestPublicId>/confirm" \
  -H "X-XSRF-TOKEN: $CSRF"           # 204: números atribuídos
```

## 4) Visualizar números + **prêmio recebido**
```bash
curl -s -b cj.txt "$BASE/participations/campaigns/$CID/numbers/mine"
# { numbers: [ { formattedNumber, status, prizeCents } ], prizeTotalCents, ... }
```
No app: página **Meus números** (`/meus-numeros`) mostra cada número e, quando `prizeCents > 0`, um selo
**“🎉 Prêmio R$ 10,00 / R$ 100,00”**. A soma aparece em `prizeTotalCents`.

## 5) Extras úteis para a demo
- **Rankings** (público): `GET /api/rankings/campaigns/{id}/PROMOTER|PARTICIPANT`.
- **Transparência da carga de prêmios** (público): `GET /api/participations/campaigns/{id}/numbers/prize-load`
  e `.../verify`. O CSV (`.../export`) só é público após `LOCKED` (anti-direcionamento).
- **Carga de prêmios** (se não usar o seed): `POST /api/participations/campaigns/{id}/numbers/prize-load`
  (admin, campanha **DRAFT** e modo **DEMO**).

## Bloqueios (o que **não** funciona — por design)
- Pagamento real: não há PSP; tudo é mock (`simulation: true`). Operação real exige os gates `G-*`.
- Premiação em campanha não-DEMO: a carga de prêmios e a revelação só existem em DEMO.
- `settle` e código OTP `000000` existem **apenas** nos perfis `local`/`staging`.

## Observações de configuração
- `NEXT_PUBLIC_API_URL` e `NEXT_PUBLIC_CAMPAIGN_ID` são **build-time** (variáveis do repositório no
  workflow de imagens). Ao mudar, rode o workflow de novo.
- Endpoints de campanha ficam em `/api/campaigns` (o controller foi corrigido de `/api/campaigns`
  duplicado).

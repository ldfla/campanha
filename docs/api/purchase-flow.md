# Contrato — Fluxo de compra (Bloco A / lacunas P07)

> Marcações exigem sessão autenticada (cookie `SESSION`) e proteção CSRF
> (`X-XSRF-TOKEN`), exceto onde indicado. Todas as rotas vivem sob o context-path `/api`.

## `POST /participations/campaigns/{campaignId}`

Solicitação de participação (DEMO: atribui números imediatamente). O contrato ganhou dois campos.

```json
{
  "idempotencyKey": "0f8fad5b-d9cb-469f-a165-70867728950e",
  "count": 2,
  "entityId": "9c0f2b7e-6b2a-4c2e-9f31-2b1a0d5e7a44",
  "referralCode": "Ab3xY9kLm2Qp"
}
```

| Campo | Obrigatório | Regra |
|---|---|---|
| `idempotencyKey` | sim | UUID; repetir a chave com `count`/`entityId` diferentes → `409 IDEMPOTENCY_CONFLICT` |
| `count` | sim | 1..100 |
| `entityId` | não | UUID público de entidade beneficiada **ativa** da mesma campanha; ausente/inativa/desconhecida → `400 VALIDATION_ERROR` |
| `referralCode` | não | ≤ 20 chars; código inexistente é **ignorado** (sem erro); autoindicação → `409 REFERRAL_SELF_REFERRAL` |

Resposta: `ParticipationAllocation` (inalterada). O voto é persistido uma única vez por requisição em
`beneficiary_selections`; a indicação só é atribuída a entradas elegíveis (`ASSIGNED`).

## `GET /participations/campaigns/{campaignId}/offers`

Lista as ofertas ativas da campanha com o preço definido no servidor.

```json
[ { "id": "…uuid…", "quantity": 2, "totalCents": 250, "currency": "BRL" } ]
```

## `POST /participations/campaigns/{campaignId}/offers`

`CAMPAIGN_MANAGER` ou `SYSTEM_ADMIN`. Só em campanha editável (`DRAFT`…`SCHEDULED`).

```json
{ "quantity": 2, "totalCents": 250 }
```

Validação: `quantity` 1..100 e `totalCents` ≥ 1, senão `400`. Fora da janela editável →
`409 CAMPAIGN_NOT_EDITABLE`. Papel insuficiente → `403`.

## `POST /participations/campaigns/{campaignId}/checkout`

Inicia a **reserva temporária** (TTL 15 min) e cria o pedido de pagamento mock com o valor da oferta.
Somente `DEMO` + `MockPaymentProvider`; outro modo/provedor → `403 PAYMENT_NOT_ALLOWED`.

Corpo:

```json
{
  "idempotencyKey": "…uuid…",
  "offerId": "…uuid…",
  "entityId": "…uuid…",
  "referralCode": "Ab3xY9kLm2Qp"
}
```

Resposta `201`:

```json
{
  "requestPublicId": "…uuid…",
  "expiresAt": "2026-10-31T18:15:00Z",
  "numbers": ["00000", "00001"],
  "orderPublicId": "…uuid…",
  "amountCents": 250,
  "currency": "BRL",
  "simulation": true
}
```

Os números ficam `RESERVED` — **não** contam como participação elegível nem pontuam indicação até a
confirmação. Repetir a mesma `idempotencyKey` com corpo divergente → `409 IDEMPOTENCY_CONFLICT`.

## `POST /participations/campaigns/{campaignId}/checkout/{requestPublicId}/confirm`

Confirma a atribuição após o pagamento mock estar conciliado. Sem cobrança `CONFIRMED` +
pedido `RECONCILED` + `ChargeState.PAID` coerentes → `502 PAYMENT_PROVIDER_ERROR`. Reserva já
expirada → `410 PAYMENT_ORDER_EXPIRED`. Sucesso (idempotente) → `204`.

## Estados e invariantes

- Número: `AVAILABLE → RESERVED → ASSIGNED` (pagamento) ou `RESERVED → AVAILABLE` (lease expirado).
- Expiração por outbox (`RESERVATION_EXPIRY_REQUESTED`) via worker a cada ~30 s.
- Reserva cancelada: entrada e pedido viram `REJECTED`; números voltam a `AVAILABLE`.
- A atribuição por pagamento grava `entry_number_assignments`, `eligibility_events` e, se houver
  código, a atribuição de indicação.

## Erros

`application/problem+json` com `ErrorCode` estável: `403 PAYMENT_NOT_ALLOWED`,
`409 IDEMPOTENCY_CONFLICT`, `410 PAYMENT_ORDER_EXPIRED`, `502 PAYMENT_PROVIDER_ERROR`,
`400 VALIDATION_ERROR`, `403 REFERRAL_SELF_REFERRAL`, `409 PARTICIPATION_LIMIT_EXCEEDED`.

## Migração

`V012__purchase_flow.sql`: `beneficiary_entities`, `beneficiary_selections`,
`campaign_purchase_offers`, `reservation_payment_links`.

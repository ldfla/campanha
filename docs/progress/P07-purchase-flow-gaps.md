# P07 — Lacunas do fluxo de compra (Bloco A)

- Commit/branch ou estado local: `feature/p07-purchase-flow-gaps`, rebaseada sobre `main` (`b748590`).
- Tarefas concluídas e arquivos alterados:
  - **Voto na entidade beneficiada.** `CreateParticipationRequest` ganhou `entityId`; entidades
    `BeneficiaryEntity`/`BeneficiarySelection` + `BeneficiaryService`/`BeneficiaryController`
    (`GET`/`POST /participations/campaigns/{id}/beneficiaries`). Voto validado como ativo e da
    campanha; gravado uma vez por requisição; replay divergente → `IDEMPOTENCY_CONFLICT`.
  - **Indicação conectada ao fluxo.** `CreateParticipationRequest.referralCode` +
    `ReferralService.attributeIfValid`/`validCodeForUser`. Código inexistente é ignorado;
    autoindicação → `REFERRAL_SELF_REFERRAL`; atribuição só em entradas elegíveis.
  - **Reserva com TTL.** `ReservationService` (`RESERVED` + lease de 15 min), `ReservationLease`
    com estados `ACTIVE`/`EXPIRED`/`CONSUMED`, `ReservationExpiryWorker` consumindo
    `RESERVATION_EXPIRY_REQUESTED` do outbox com `FOR UPDATE SKIP LOCKED`, e
    `ReservationCheckoutService` ligando reserva ↔ cobrança mock (atribuição só após pagamento
    `CONFIRMED`/`RECONCILED`).
  - **Contrato HTTP.** `PurchaseFlowController` expõe `offers` (lista/criação) e `checkout`
    (iniciar/confirmar); preço sempre definido no servidor.
  - Migração `V012__purchase_flow.sql` (four novas tabelas, todas com FK e CHECK).
  - Contratos documentados em `docs/api/purchase-flow.md`.
- Comandos executados e resultados:
  - `./mvnw -B verify` — `Tests run: 110, Failures: 0, Errors: 0` / `BUILD SUCCESS` (linha de base
    do trabalho herdado: 108; +2 do `PurchaseFlowControllerIntegrationTest`).
  - `./mvnw -B -Dtest=PurchaseFlowControllerIntegrationTest test` — `Tests run: 2, Failures: 0,
    Errors: 0` / `BUILD SUCCESS`.
- Testes, cobertura e logs relevantes:
  - `BeneficiaryParticipationIntegrationTest`: voto registrado uma vez; entidade inativa/desconhecida
    → `VALIDATION_ERROR`; replay divergente → `IDEMPOTENCY_CONFLICT`; autoindicação bloqueada; código
    inválido ignorado; indicação só pontua após atribuição.
  - `ReservationServiceIntegrationTest`: 100 reservas concorrentes sem número duplicado; 16 workers
    concorrentes expiram o mesmo lease com efeito único; worker de outbox libera estoque; atribuição
    só após conciliação mock; pagamento após prazo não atribui.
  - `PurchaseFlowControllerIntegrationTest`: ofertas com preço do servidor (403 para não gestor);
    checkout exige CSRF/sessão; confirmação antes do pagamento → `502`; após conciliação → `204`
    idempotente; indicação e voto persistidos; campanha não-DEMO → `403`.
- Contratos/ADRs/migrações alterados: `V012__purchase_flow.sql`; `docs/api/purchase-flow.md`.
- Riscos, limitações e bloqueios:
  - Fluxo disponível **somente em DEMO** com `MockPaymentProvider`; `LIVE_OPERATIONS_ENABLED=false`
    e nenhum provedor real de pagamento.
  - TTL fixo de 15 min (constante); parametrização fica para a fase de operação.
  - Ofertas não têm vigência/limite por campanha além de `active`; preço é responsabilidade do gestor.
  - Expiração roda em instância única (worker agendado); múltiplas instâncias continuam corretas por
    causa do `FOR UPDATE SKIP LOCKED`, mas não há backpressure explícito.
- Critérios de aceite: `entityId` PASS; `referralCode` PASS; `ReservationLease` TTL + worker PASS;
  testes de integração PASS; contrato documentado PASS.
- Próxima etapa: Bloco B — completar P11 (commit-reveal, 2 tiers) no PR #12.

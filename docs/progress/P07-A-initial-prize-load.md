# P07-A — Carga inicial de prêmios nos números e auditoria do comprador

- Commit/branch: `feature/p07-initial-prize-load`, empilhada em `feature/p07-purchase-flow-gaps`
  (PR #20). Migração `V020`.
- Tarefas concluídas e arquivos alterados:
  - **Índice de números.** `campaign_numbers` cobre 00000–99999 (uma linha por número) via
    `ParticipationService.initializeSeries`; a carga inicial valida que o inventário está completo.
  - **Prêmios.** `InitialPrizeLoadService.load`: `SecureRandom` escolhe 10 números no tier de
    R$ 100 (`10_000` centavos) e 100 no tier de R$ 10 (`1_000`), **sem colisão** — ao sortear um
    número já premiado, sorteia outro — e grava `prize_defined_at`. Carga idempotente por campanha.
  - **Hash auditável.** `exportCsv` produz arquivo canônico (`number,prize_cents,prize_defined_at`,
    ordenado por número); `verify` recalcula o SHA-256 e confirma contra o valor persistido em
    `campaign_prize_loads`. Endpoints: `POST .../numbers/prize-load` (gestor), `GET .../prize-load`
    (público), `GET .../prize-load/export` (CSV, público), `GET .../prize-load/verify` (público).
  - **Duas camadas de colunas.** Inicialização (`prize_cents`, `prize_defined_at`) cobertas pelo
    hash; operação (`purchased_at`, `purchase_modality`, `correlation_id`, `charity_institution`,
    `referrer_phone`) atualizáveis e fora do hash.
  - **Caridade obrigatória quando vendido.** CHECK `chk_number_charity_sold` exige
    `charity_institution` não nulo para `ASSIGNED`/`FROZEN`. `entityId` passou a **obrigatório** na
    compra (gratuita DEMO e checkout).
  - **Divulgador precisa de cota.** `ReferralService.attributeIfValid`/`promoterUserIdForActiveCode`
    só atribuem/indicam se o divulgador tiver ≥ 1 entrada `ASSIGNED`/`FROZEN` na campanha.
  - **E.164.** `PhoneNumbers` normaliza/valida telefone em ITU-T E.164; usado no cadastro
    (`AuthService`) e nos telefones de comprador/indicador.
  - **PII isolada.** `campaign_number_buyers` (1:1 por número) guarda nome, telefone (E.164) e
    **pseudônimo** (iniciais + 4 últimos dígitos), mantendo a tabela de números sem PII.
- Migração: `V020__initial_prize_load.sql` (colunas + CHECKs + `campaign_number_buyers` +
  `campaign_prize_loads`).
- Testes: `InitialPrizeLoadIntegrationTest` (4) — 2 tiers sem colisão, idempotência, adulteração de
  prêmio detectada, colunas de operação fora do hash, E.164 inválido e cota do divulgador.
- Comandos: `./mvnw -B test` → 165 testes verdes; `./mvnw -B verify` → ver rodapé.
- Riscos e limitações:
  - O hash cobre número/prêmio/timestamp; a PII e a caridade são deliberadamente mutáveis.
  - `documentos/progress` do Bloco A (#20) permanece PR aberto — esta PR depende dele.
  - Operação real continua bloqueada (`DEMO`, `LIVE_OPERATIONS_ENABLED=false`); prêmio em dinheiro
    só existe como simulação.
  - Retenção/expurgo da PII do comprador segue pendente (P16).

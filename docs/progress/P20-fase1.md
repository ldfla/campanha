# P20 — Fase 1: lacunas de teste e segurança

- **Commit/branch ou estado local:** `feature/p20-redteam-finalizacao` @ `91b90bf` + alterações locais desta
  fase (não commitadas no momento do registro). Nada enviado ao remoto.

- **Tarefas concluídas e arquivos alterados:**
  - `P08-07` — `ReferralFraudIntegrationTest` (8): atribuição manual de entrada alheia, cookie stuffing,
    replay de atribuição e de clique, troca de URL (código de outra campanha / inexistente), divulgador sem
    cota, contas vinculadas (autoindicação e conta laranja invalidada), adulteração de snapshot no banco e
    métricas restritas a `PROMOTER`. Correções: P20-F21 (endpoint removido em `ReferralController` e
    `frontend/src/lib/api-client.ts`) e P20-F24 (`RankingService.positionsMatchPayload`).
  - `P09-08` — `PaymentFailureRecoveryIntegrationTest` (5) com provedor que injeta timeout: criação de
    cobrança, falha de commit após cobrança criada (cobrança órfã não vinculada), timeout na verificação do
    webhook (não é reconhecido e a reentrega confirma uma vez), resposta de estorno perdida e worker com
    item envenenado. Correções: P20-F22 (`ReconciliationWorker`, `OutboxEvent.reschedule`) e P20-F23
    (`OrderService.refund`); `OrderService.reconcile` passa a devolver o `PaymentStatus`.
  - `P10-07` — `PaymentRefundedEvent` publicado no estorno; `RefundedParticipationHandler` invalida as
    entradas e números do pedido na mesma transação, com `audit_events` `PARTICIPATION_REFUNDED`
    (`ParticipationEntry.invalidateRefunded`, `CampaignNumber.invalidateRefunded`,
    `ReservationPaymentLinkRepository.findByOrderId`). `RefundRankingIntegrationTest` (2).
  - `P18-02` — `ConcurrencyLimitsIntegrationTest` (3): 500 concorrentes para 400 números, operações
    misturadas em paralelo e controle positivo de deadlock (métrica `lock_deadlocks` lida com root só no
    contêiner de teste; o usuário da aplicação não tem `PROCESS`).
  - `P18-06` — `scripts/backup-mysql.sh` (`DB_DEFAULTS_FILE`, ação `verify`, `restore` recusa hash
    divergente; compatível com bash 3.2) e `BackupRestoreDrillIntegrationTest` (2) executando o script no
    MySQL 8.4; `docs/operations/backup-restore.md` atualizado.
  - Documentação: `docs/security/red-team/P20-findings.md` (F21–F24), `docs/decisions.md` (D-010).

- **Comandos executados e resultados:**
  - `./mvnw -B test -Dtest='ReferralFraudIntegrationTest,RankingServiceIntegrationTest,ReferralServiceIntegrationTest,RankingControllerTest'`
    → 19 testes, 0 falhas.
  - `./mvnw -B test -Dtest='RefundRankingIntegrationTest,PaymentFailureRecoveryIntegrationTest,OrderServiceIntegrationTest'`
    → 15 testes, 0 falhas.
  - `./mvnw -B test -Dtest='ConcurrencyLimitsIntegrationTest'` → 3 testes, 0 falhas. Saída:
    `[P18-02] contenders=500 capacity=400 assigned=400 exhausted=100 elapsed_ms=5691 deadlocks=0` e
    `[P18-02] mixed_tasks=100 elapsed_ms=985 deadlocks=0`.
  - `./mvnw -B test -Dtest='BackupRestoreDrillIntegrationTest'` → 2 testes, 0 falhas. Saída:
    `[P18-06] tables=58 dump_bytes=89169 checksums=identical`.
  - `bash -n` e `shellcheck scripts/backup-mysql.sh` → sem apontamentos; `verify` testado com bash 3.2
    (aceita o dump íntegro, recusa o alterado com código 1).
  - `npm run typecheck` (frontend) → OK.
  - `./mvnw -B verify` (suíte completa) → **223 testes, 0 falhas, 0 erros, BUILD SUCCESS** (antes: 203).

- **Testes, cobertura e logs relevantes:** 20 testes novos nesta fase. Cobertura JaCoCo não medida.
  Os tempos de P18-02 usam estoque de 400 números e **não** são comparáveis aos 38 s da P18 (série de
  100.000 números, onde a busca de disponíveis domina).

- **Contratos/ADRs/migrações alterados:** removido `POST /referrals/campaigns/{id}/attributions/{code}/{entryId}`
  (D-010). Sem migração Flyway. Interno: construtor de `OrderService` recebe `ApplicationEventPublisher`.

- **Riscos, limitações e bloqueios:**
  - Cobrança órfã no PSP após falha de commit fica sem vínculo (o comprador não recebe o QR); com PSP real é
    preciso conciliar cobranças órfãs e cancelá-las.
  - Número estornado não volta ao estoque (decisão conservadora D-010).
  - Estorno depois da apuração publicada não altera o resultado lacrado; procedimento de contestação fica
    para os runbooks da P19.
  - Objetos privados (documentos de compliance) não ensaiados: storage `memory` no MVP.
  - Mesmo revisor encontrou e corrigiu F21–F24 (sem segregação, ADR-0009).

- **Critérios de aceite:**
  | Item | Resultado |
  |---|---|
  | P08-07 cookie stuffing, replay, troca de URL, contas vinculadas, manipulação de relatório | PASS |
  | P09-08 timeout de transporte e falha parcial PSP↔banco | PASS |
  | P10-07 estornos com efeito consistente nos rankings | PASS |
  | P18-02 500 concorrentes, último número, falta de espaço, deadlock | PASS |
  | P18-06 backup/restauração com integridade de hashes (MySQL) | PASS |
  | P18-06 objetos privados | NÃO EXECUTADO |
  | Suíte completa `./mvnw -B verify` | PASS (223/223) |

- **Próxima etapa:** Fase 2 do `plano-finalizacao.md` (P06-08 CTA por capability, P05A-09, P15, P06-07/P17).

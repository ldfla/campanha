# P20 — Fase 2: produto, notificações e E2E

- **Commit/branch ou estado local:** `feature/p20-fase2`, criada de `feature/p20-redteam-finalizacao` @ `3d89104`
  (PR #39 aberto). Alterações desta fase não commitadas no momento do registro. Nada enviado ao remoto.

- **Tarefas concluídas e arquivos alterados:**
  - `P06-08` — `PublicCampaignController` (`GET /campaigns/public/{id}`: `ctaState`, `purchaseEnabled`, sem PII);
    `use-campaign-state.ts` + `CampaignStateProvider`; `CTAButton`, `FloatingCTA`, `ComboCards`, `numeros` e
    `participar` usam o estado do servidor e falham fechado. Removidos `CAMPAIGN_STATUS`, `isSoldOut`, `isSuspended`.
  - Correções encontradas no caminho: rota com `/api` duplicado (`CampaignController` → `/campaigns`); leitura
    pública de ofertas e instituições (o visitante escolhe antes do cadastro); compra pela UI só com oferta
    publicada; **P20-F25** (escrita de campanha/prêmios sem checar organização), **P20-F26** (cookie CSRF com
    `Path=/api`, todo POST do frontend em 403), **P20-F27** (UI dizia "números atribuídos" sem confirmação).
  - `P05A-09` — migração `V024` (`campaign_prizes.ranking_scope/ranking_position`, entrega com
    `ranking_snapshot_id/ranking_position/prize_id`); `AwardDeliveryService.createRankingSimulations`.
  - `P15-02/03/06` — `CampaignStatusChangedEvent`, `DrawPublishedEvent`, `AwardDeliveryChangedEvent`;
    `NotificationEnqueuer`, `DemoNotificationProcessor` (5 templates essenciais, retry com backoff, inbox),
    `NotificationTransport` (`RecordingOnly` padrão, `Mailpit` opt-in), `NotificationWorker` (desligado por padrão).
  - `P17-01/06` e parciais de `P06-07`, `P17-05`, `P17-07` — Playwright 1.64.1 (`frontend/tests/e2e`, 15 testes).
  - Rodapé da home quebrava 40 px em 360/390 px → `flex-wrap`.
  - Docs: `docs/operations/local-mvp.md` (CORS, campanha com ofertas, notificações), `P20-findings.md`
    (F25–F27, F16 fechado), `docs/decisions.md` (D-011).

- **Comandos executados e resultados:**
  - `./mvnw -B test -Dtest='PublicCampaignStateIntegrationTest,AuthControllerTest,RbacTest'` → 29 testes, 0 falhas;
    `CsrfCookieTest` → 1 teste, 0 falhas.
  - `./mvnw -B test -Dtest='CampaignControllerTest,CapitalizationModeIntegrationTest'` → 19 testes, 0 falhas.
  - `./mvnw -B test -Dtest='RankingAwardIntegrationTest,AwardDeliveryServiceIntegrationTest'` → 9 testes, 0 falhas.
  - `./mvnw -B test -Dtest='NotificationFlowIntegrationTest,MailpitTransportIntegrationTest,DemoNotificationProcessorIntegrationTest,RankingAwardIntegrationTest,DrawingIntegrationTest'`
    → 12 testes, 0 falhas (inclui envio real ao contêiner Mailpit).
  - `npm run typecheck` → OK; `npm run lint` → 0 apontamentos; `npm run build` → OK.
  - `npx playwright test` contra backend `local` + frontend dev → **15 passed** (antes das correções: 12 passed,
    3 failed — rolagem horizontal em 360/390 px e checkout 403 por CSRF).
  - Verificação manual no navegador: CTA "Em breve" para campanha sem ofertas, "Construir junto" com ofertas,
    "Indisponível no momento" quando o CORS bloqueava a leitura; fluxo anônimo até a escolha da instituição.
  - `./mvnw -B verify` (suíte completa), 1ª execução → **FAIL**: 244 testes, 1 falha, 19 erros. Os 19 erros
    eram em cascata ("container … is not running"): o MySQL do contexto compartilhado parou no meio da suíte. Causa
    provável (não confirmada): memória do Docker (~3 GB) com um contêiner MySQL por contexto Spring; a fase tinha
    acrescentado mais um contexto. A falha era o teste MockMvc do cookie CSRF com `contextPath("/api")`, que só
    passava isolado (com o contexto aquecido o MockMvc não aplicava a cadeia de segurança a esse request).
  - Correções: `MailpitTransportIntegrationTest` e `ConcurrencyLimitsIntegrationTest` passaram a reutilizar
    contextos existentes (um MySQL a menos cada); o teste do cookie virou unitário (`CsrfCookieTest`), e o
    comportamento real segue coberto pelo E2E de compra no navegador.
  - `./mvnw -B verify`, 2ª execução → **244 testes, 0 falhas, 0 erros, BUILD SUCCESS** (Fase 1: 225).

- **Testes, cobertura e logs relevantes:** cobertura JaCoCo não medida. O E2E roda contra o ambiente local já de
  pé (não sobe servidores); requer `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000` e
  `NEXT_PUBLIC_CAMPAIGN_ID` da campanha `acao-capitalizacao-local`.

- **Contratos/ADRs/migrações alterados:** `CampaignController` em `/campaigns`; `PUT/DELETE
  /campaigns/{id}/prizes/{prizeId}` agora conferem a campanha; `CreateCampaignRequest.organizationId` opcional;
  `PrizeRequest/PrizeResponse` com `rankingScope/rankingPosition`; novo `GET /campaigns/public/{id}`; `GET` de
  ofertas e instituições públicos; migração `V024`; dependências `spring-boot-starter-mail` e `@playwright/test`.

- **Riscos, limitações e bloqueios:**
  - Prêmios de ranking configurados hoje são em dinheiro e seguem bloqueados (G-LEGAL); só bens geram entrega.
  - Leitor de tela não testado; localização de datas, milhar e fuso não verificada no E2E.
  - P17-02 (indicação), P17-03, P17-04 e P17-08 não executados.
  - E2E não roda no CI (P02-05 pendente) e depende do seed local.
  - Mesmo revisor encontrou e corrigiu F25–F27 (ADR-0009).

- **Critérios de aceite:**
  | Item | Resultado |
  |---|---|
  | P06-08 CTA por estado do servidor | PASS |
  | P05A-09 premiação por posição ligada à entrega | PASS (dinheiro bloqueado) |
  | P15-02 / P15-03 / P15-06 | PASS |
  | P17-01 / P17-06 | PASS |
  | P06-07 / P17-05 / P17-07 | PARCIAL |
  | P17-02 / P17-03 / P17-04 / P17-08 | NÃO EXECUTADO |
  | Suíte completa `./mvnw -B verify` | PASS (244/244, na 2ª execução) |

- **Próxima etapa:** Fase 3 (P02-05 CI com frontend e E2E, P19, P20-01..08) e os itens NÃO EXECUTADO acima.

# Red Team P20 — revisão adversarial antes da finalização

- Data: 2026-11-01 (America/Sao_Paulo), sessão automática agendada.
- Base: `feature/p09-frontend-integration` @ `fa6ecac`; correções em `feature/p20-redteam-finalizacao`
  (`e4e315d`, `0158d79`, `d07ad36`, `2c66d98`). Nada foi enviado ao remoto.
- Ambiente: **somente local** — MySQL 8.4 (`infrastructure/compose.yaml`), backend `java -jar` no perfil
  `local`, `curl` contra `http://localhost:8080/api`, Testcontainers nos testes. Nenhum teste contra
  terceiros, nenhum pagamento real, nenhuma mensagem enviada, nenhum segredo real.
- **Limitação de segregação (ADR-0009):** o mesmo agente que revisou também corrigiu. Isso reduz o valor
  independente da revisão; um re-teste por pessoa diferente continua necessário para `G-SECURITY`.

## Veredito

**APROVADO para a Fase 1** — zero achados críticos/altos abertos após correção e re-teste.
Foram encontrados **1 crítico e 4 altos**, todos corrigidos com testes negativos e re-testados ao vivo.
Achados médios/baixos remanescentes estão listados com risco residual.

## Resumo

| ID | Severidade | Título | Status |
|---|---|---|---|
| P20-F01 | **CRÍTICO** | Login só com o telefone: tomada de qualquer conta | CLOSED |
| P20-F02 | **ALTO** | MFA de admin sem vínculo ao 1º fator, código fixo em todos os perfis, sem bloqueio | CLOSED |
| P20-F03 | **ALTO** | Participante cria pedido com valor escolhido no cliente | CLOSED |
| P20-F04 | **ALTO** | Participação gratuita atribui número elegível em campanha paga (não-DEMO) | CLOSED |
| P20-F05 | **ALTO** | Lista pública de números premiados + atribuição sequencial → compra direcionada | CLOSED (residual F11) |
| P20-F06 | MÉDIO | Enumeração de telefone (login 200/401; cadastro 409) | CLOSED |
| P20-F07 | MÉDIO | `55DDDNÚMERO` sem `+` virava `+5555…` (identidade divergente) | CLOSED |
| P20-F08 | MÉDIO | "Meus números" listava números da campanha inteira e calculava "Investido" no cliente | CLOSED |
| P20-F09 | MÉDIO | `/actuator/metrics` acessível a qualquer autenticado | CLOSED |
| P20-F10 | MÉDIO | Preço de quantidade personalizada e fallback de combos calculados no navegador | CLOSED |
| P20-F11 | MÉDIO | Atribuição de números sequencial, não aleatória/equitativa (AGENTS §5.2) | OPEN |
| P20-F12 | MÉDIO | Rate limit confiava em `X-Forwarded-For` (bypass trocando o cabeçalho) | CLOSED |
| P20-F13 | BAIXO | Frontend não enviava `X-XSRF-TOKEN`: toda mutação autenticada falhava (403) | CLOSED |
| P20-F14 | BAIXO | Casca do painel `/contribua` exibida a qualquer usuário autenticado | CLOSED (só UI) |
| P20-F15 | BAIXO | 401/403/429 sem `charset` (texto corrompido) | CLOSED |
| P20-F16 | MÉDIO | CTA/esgotado/suspenso decididos por constantes do frontend, não por capabilities | OPEN (P06-08) |
| P20-F17 | BAIXO | Pagamento não conciliado responde 502 em vez de 409 na confirmação | OPEN |
| P20-F18 | BAIXO | Sessões criadas antes da correção sobrevivem ao reinício (Spring Session JDBC) | MITIGADO (runbook) |
| P20-F19 | BAIXO | Perfil `default` ativa adaptadores mock | MITIGADO |
| P20-F20 | BAIXO | `OrderReceipt` expõe `paymentId` interno sequencial | OPEN |

## Detalhes

### P20-F01 — CRÍTICO — Login só com o telefone (CWE-287 / OWASP API2)
- **PoC local:** vítima `POST /auth/register`; atacante, em sessão nova, `POST /auth/login {"phoneNumber": <telefone da vítima>}` →
  **200** com nome completo; `GET /auth/me` → 200. Com a sessão, o atacante via "Meus números", podia comprar e
  participar em nome da vítima. O mesmo valia para o administrador semeado (ver F02).
- **Correção:** `/auth/login` e `/auth/register` só solicitam código (202, corpo uniforme `CODE_SENT`); a sessão nasce
  apenas em `POST /auth/login/verify {phoneNumber, code}`. 5 falhas → bloqueio de 15 min; envio de código limitado por
  telefone. Cadastro não cria sessão (evita "ocupar" o telefone de terceiros). ADR-0010.
- **Testes:** `AuthControllerTest.loginRequestWithPhoneOnlyNeverAuthenticates`, `verifyLocksAfterFiveWrongCodes`,
  `verifyRejectsWrongCodeAndUnknownPhoneWithSameError`, `registerCreatesUserWithoutAuthenticatedSession`.
- **Re-teste ao vivo:** login só com telefone → **202** sem dados; `/auth/me` → **401**; 6 códigos errados → `401×5, 429`.
- **Residual:** código DEMO fixo `000000` (somente perfis `default/local/test`); provedor real de OTP (Twilio/WhatsApp)
  depende de G-PSP/G-FINANCE. Bloqueio em memória (instância única).

### P20-F02 — ALTO — MFA desvinculado do primeiro fator (CWE-308)
- **PoC local:** após qualquer login do admin (que cria o enrollment), `POST /auth/mfa/verify {phone, "000000"}` em
  **outra** sessão → 200 e papel `SYSTEM_ADMIN`. 12 códigos errados seguidos sem bloqueio nem 429. `MfaService` aceitava
  `000000` em **qualquer** perfil.
- **Correção:** o primeiro fator grava `PendingMfa(userId, expiresAt 5 min)` na sessão do servidor; `/auth/mfa/verify`
  só aceita código nessa sessão e com o mesmo telefone; 5 falhas → bloqueio. Verificador `DemoMfaCodeVerifier` só existe
  nos perfis locais e recusa iniciar com `app.live-operations-enabled=true` (falha fechada); o mesmo para
  `MockContactVerificationProvider`. Rate limit passou a cobrir `/auth/mfa` e `/auth/verify-contact`.
- **Testes:** `mfaWithoutFirstFactorInSessionIsRejected`, `mfaLocksAfterFiveWrongCodes`,
  `systemAdminMustCompleteDemoMfaBeforeSessionIsCreated`.
- **Re-teste ao vivo:** MFA direto em sessão nova → **403**; `GET /_cx/x` com essa sessão → **401**.
- **Residual:** MFA real (TOTP/WebAuthn) não implementado — ADR-0005; obrigatório antes de G-LAUNCH.

### P20-F03 — ALTO — Valor do pedido definido pelo cliente (CWE-602 / OWASP API6)
- **PoC local:** participante `POST /payments/orders {"campaignId":1,"amountCents":1}` → **201** `AWAITING_PAYMENT`,
  cobrança criada no PSP mock, sem vínculo com participação. O frontend usava esse caminho como fallback com
  `total` calculado no navegador.
- **Correção:** endpoint restrito a `SYSTEM_ADMIN` (ferramenta sandbox); o participante compra apenas via checkout,
  cujo valor é o da oferta no servidor. Fallback removido do frontend (F10).
- **Teste:** `PaymentControllerTest.participantCannotCreateOrderWithClientDefinedAmount`.
- **Re-teste ao vivo:** mesmo PoC → **403**.

### P20-F04 — ALTO — Participação gratuita em campanha paga (CWE-841)
- **PoC local:** `POST /participations/campaigns/{campanha CAPITALIZATION_PRIZE_TITLE OPEN}` → **201**, número `00000`
  atribuído e evento `ELIGIBLE/DEMO_SIMULATION`, sem pagamento — viola AGENTS §5.2/§5.3.
- **Correção:** a participação direta só é aceita em `DEMO` (`CAMPAIGN_LIVE_OPERATIONS_DISABLED`, 403); modos pagos
  usam checkout conciliado (que já rejeitava não-DEMO sem autorização).
- **Teste:** `P20RedTeamRegressionTest.freeParticipationIsRejectedOutsideDemo` (nenhum número/entrada criado).
- **Re-teste ao vivo:** mesmo PoC → **403**.

### P20-F05 — ALTO — Direcionamento a número premiado (CWE-200 / abuso de negócio)
- **Análise:** `GET .../numbers/prize-load/export` era público e lista quais números valem R$ 100/R$ 10; a alocação
  usa `ORDER BY number_value` (o próximo número livre é previsível pela disponibilidade pública). Basta comprar a
  quantidade exata até o número premiado. Demonstrado localmente a sequência `00000, 00001, 00002`; o CSV não estava
  carregado na base local, por isso a exploração completa foi demonstrada por análise + teste.
- **Correção:** exportação pública lacrada até `LOCKED` (e estados posteriores) com `PRIZE_LOAD_SEALED` (409). O
  SHA-256 continua público em `GET .../prize-load` e `.../verify` (compromisso antes, revelação depois).
- **Teste:** `prizeListIsSealedWhileSalesAreOpen` (409 em `OPEN`, 200 em `LOCKED`, `00000` presente no CSV).
- **Re-teste ao vivo:** CSV em campanha `OPEN` → **409 `prize-load-sealed`**.
- **Residual:** F11 (atribuição sequencial).

### P20-F06 — MÉDIO — Enumeração por telefone
- **PoC:** login de telefone inexistente → 401; existente → 200; cadastro duplicado → 409. O teste
  `antiEnumerationLoginDoesNotRevealExistence` anterior só checava o caso inexistente (verde falso).
- **Correção/teste:** respostas idênticas (202 `CODE_SENT`) comparadas byte a byte no teste reescrito;
  cadastro duplicado não altera dados nem gera segundo evento.

### P20-F07 — MÉDIO — Normalização de telefone
- **PoC:** o frontend enviava `5511987650001`; o backend prefixava `+55` → `+555511987650001` (conta "fantasma").
- **Correção:** 12–13 dígitos iniciados por `55` são tratados como já contendo o DDI; frontend envia E.164 com `+`.
- **Teste:** `phoneWithCountryCodeWithoutPlusMapsToSameAccount`.

### P20-F08 — MÉDIO — "Meus números"
- **Achado:** a página chamava `/numbers` (status de **todos** os números da campanha, sem PII) e calculava
  "Investido" = confirmados × preço fixo no cliente; usava a campanha inexistente `demo`.
- **Correção:** `GET /participations/campaigns/{id}/numbers/mine` devolve só os números do titular e o valor pago
  (`orders` `CONFIRMED/RECONCILED`) calculado no servidor; sem sessão → 401 sem corpo de dados; o frontend não faz
  nenhuma chamada sem sessão.
- **Teste:** `myNumbersIsScopedToSessionOwner` (Alice vê 2 números, Bruno vê 1, sem dados cruzados).

### P20-F09 — MÉDIO — Actuator
- **PoC:** participante `GET /actuator/metrics` → 200. **Correção:** `/actuator/**` (exceto `health`) exige
  `SYSTEM_ADMIN`/`SECURITY_ADMIN`. **Teste:** `actuatorMetricsRequireAdministrativeRole`. **Re-teste:** 403.

### P20-F10 — MÉDIO — Preço no cliente
- `participar` exibia combos de `campaign-config.ts` quando não havia ofertas, permitia quantidade personalizada
  precificada como `qtd × PRICE_UNIT_CENTS` e criava pedido com esse total. Agora só ofertas do servidor são
  selecionáveis; sem ofertas, a compra fica bloqueada com aviso. Rótulos de combo seguem cosméticos.
- **Residual:** textos de preço/prêmios em páginas de marketing ainda vêm de `campaign-config.ts` (F16).

### P20-F11 — MÉDIO — Atribuição sequencial (OPEN)
- `lockAvailableByCampaignId` ordena por `number_value`. Com o lacre (F05) o direcionamento a prêmios deixa de ser
  explorável, mas o requisito de atribuição aleatória/equitativa (AGENTS §5.2) não está atendido.
- **Recomendação:** início aleatório (`SecureRandom`) com varredura circular sob `FOR UPDATE` (sem `SKIP LOCKED`),
  testado com 100+ concorrentes e preservando `00000`. Responsável: participação. Fica para P18/P20.

### P20-F12 — MÉDIO — Rate limit e `X-Forwarded-For`
- **PoC:** a chave do limite vinha do primeiro valor de `X-Forwarded-For`, controlado pelo cliente.
- **Correção:** usa somente `getRemoteAddr()`; atrás de proxy confiável usar `server.forward-headers-strategy`.
- **Re-teste:** 12 requisições com `X-Forwarded-For` variável → `202×8, 429×4` (janela compartilhada).

### P20-F13/F14/F15 — BAIXO
- F13: `api-client` passa a enviar `X-XSRF-TOKEN` lido do cookie `XSRF-TOKEN` em métodos não seguros.
- F14: `/contribua` só mostra o painel a papéis operacionais — barreira de UI; o backend continua sendo a autorização.
- F15: `application/problem+json;charset=UTF-8` nos handlers de 401/403/429.

### P20-F16 — MÉDIO — CTA e capabilities (OPEN)
- `isSoldOut()/isSuspended()` e `CAMPAIGN_MODE` são constantes do frontend. O backend bloqueia a operação de fato
  (checkout/participação), mas a UI pode anunciar venda que não existe. Tratar em P06-08 com
  `CampaignResponse.capabilities`.

### P20-F17 — BAIXO — Semântica 502 (OPEN)
- Confirmar checkout antes da conciliação devolve `PAYMENT_PROVIDER_ERROR` (502). Não vaza dado nem altera estado
  (teste `checkoutConfirmationIsBoundToOwnerAndToReconciledPayment`), mas confunde monitoração. Sugestão: 409.

### P20-F18 — BAIXO — Sessões anteriores à correção
- Uma sessão obtida pelo PoC F01 **antes** da correção continuou válida após reiniciar o backend (Spring Session
  JDBC). Localmente foram apagadas (`DELETE FROM SPRING_SESSION…`, 16 linhas) e a sessão passou a 401.
- **Ação obrigatória de implantação:** invalidar todas as sessões ao publicar esta correção.

### P20-F19 — BAIXO — Perfil `default`
- Adaptadores mock existem nos perfis `default/local/test`. Mitigação adicionada: recusam iniciar com
  `app.live-operations-enabled=true`. Recomendação: perfil `prod` explícito sem `default` no deploy (P19).

### P20-F20 — BAIXO — ID interno exposto (OPEN)
- `OrderReceipt.paymentId` é `BIGINT` sequencial. Endpoint agora só para admin (F03), risco baixo.

## Achados da Fase 1 (2026-11-01, após a aprovação)

Encontrados ao escrever os testes de P08-07/P09-08/P10-07. Corrigidos, com teste negativo; mesmo revisor
que corrigiu (sem segregação, ADR-0009).

### P20-F21 — ALTO — Atribuição manual de indicação de terceiros (CWE-639 / OWASP API1)
- **PoC:** `POST /referrals/campaigns/{id}/attributions/{code}/{entryId}` aceitava qualquer sessão; `entryId`
  é sequencial. Um divulgador atribuía a si as entradas ainda não atribuídas de outros compradores (sem
  checar dono da entrada nem cota do divulgador) e subia no ranking `PROMOTER`, que tem premiação.
- **Correção:** endpoint removido (e `referrals.attribute` do `api-client.ts`). A atribuição só ocorre no
  servidor, no checkout/participação, com o código fixado na reserva.
- **Re-teste:** `ReferralFraudIntegrationTest.manualAttributionOfSomeoneElsesEntryIsNotExposed` → 4xx, ranking vazio.

### P20-F22 — ALTO — Reconciliação travada por um único pagamento (disponibilidade / integridade financeira)
- **PoC:** `ReconciliationWorker` reconciliava até 100 pagamentos numa única transação; um timeout do PSP ou um
  pagamento inconsistente fazia rollback do lote inteiro em todas as execuções. Pagamento ainda pendente era
  marcado como processado e nunca mais reconciliado sem webhook.
- **Correção:** cada pagamento em transação própria (`REQUIRES_NEW`), falha registra tentativa com backoff
  exponencial (até 30 min, depois `DEAD_LETTER`), pendente é reagendado sem consumir tentativa.
- **Re-teste:** `PaymentFailureRecoveryIntegrationTest.workerIsolatesAFailingPaymentRetriesTransientTimeoutsAndKeepsPendingOnesScheduled`.

### P20-F23 — MÉDIO — Estorno repetido após resposta perdida (CWE-837)
- **PoC:** se a resposta do estorno se perdia depois de o PSP estornar, a nova tentativa chamava o estorno no PSP
  de novo (com PSP real, risco de estorno em dobro).
- **Correção:** `OrderService.refund` consulta a fonte antes; se já estornado, converge sem nova chamada.
- **Re-teste:** `lostRefundResponseIsRetriedWithoutRefundingTwice` → uma única chamada de estorno ao PSP.

### P20-F24 — MÉDIO — Linhas de posição do snapshot fora da verificação
- **PoC:** `ranking_positions` não entra no hash; a exportação só conferia a contagem. `UPDATE` direto de
  pontuação ou posição passava como verificado.
- **Correção:** exportação confere posição e pontuação de cada linha contra o payload lacrado.
- **Re-teste:** `ReferralFraudIntegrationTest.directDatabaseEditOfSnapshotPositionsOrPayloadIsDetected`.

## Vetores atacados sem achado novo

| Vetor | Evidência | Resultado |
|---|---|---|
| `/_cx/**` sem sessão / participante | `curl` → 401 / 403; `RbacTest` | Bloqueado |
| Ranking público | payload ao vivo `{"pseudonym":"M.V. 0001", …}`; `RankingPseudonymizerTest` | Só pseudônimo |
| Settle mock / webhook mock por participante | `curl` → 403 / 403 | Bloqueado |
| IDOR na confirmação do checkout | `checkoutConfirmationIsBoundToOwnerAndToReconciledPayment` → 403 | Bloqueado |
| Confirmação antes do pagamento | mesmo teste → 502, nenhum número `ASSIGNED` | Bloqueado (F17) |
| Replay/reordenação de webhook, valor divergente | `OrderServiceIntegrationTest` (8 testes) | Bloqueado |
| 100+ reservas/alocações concorrentes | `ParticipationConcurrencyIntegrationTest`, `ReservationServiceIntegrationTest` | Zero duplicatas |
| `00000` na apuração | `DrawingEngineTest.preservesZeroNumberAndSupportsDifferentDigitCounts` | Preservado |
| Autoindicação | `ReferralServiceIntegrationTest.attributesOnlyOnceAndBlocksSelfReferral` | Bloqueado |
| CSRF em mutação | `curl` sem token → 403 | Bloqueado |
| Fixação de sessão | `verifyRotatesExistingSessionCookie` | ID rotacionado |
| PII/tokens em log | `grep` de telefones, nomes, `SESSION`, `XSRF` nos logs locais → 0 ocorrências | OK |
| Segredos no git | `git grep` heurístico (AWS/Stripe/GitHub/Google/chaves privadas) em até 200 commits; `git ls-files` sem `.env`/`.pem`/`.key` | Nada encontrado (gitleaks não instalado) |
| `NEXT_PUBLIC_*` | só `NEXT_PUBLIC_API_URL` e `NEXT_PUBLIC_CAMPAIGN_ID` (públicos); bundle `.next/static` sem credenciais de dev | OK |

## Não executado (honestidade de evidência)
- Cookie stuffing e replay de clique: cobertos na Fase 1 (`ReferralFraudIntegrationTest`), em nível de
  serviço/MockMvc, não com navegador real.
- Verificação visual do frontend no navegador: o preview foi recusado nesta sessão; só `typecheck` e `build`.
- gitleaks/trufflehog, SAST/DAST, SBOM: ferramentas não instaladas; nada foi executado.
- Revisor independente (segregação ADR-0009) não disponível.

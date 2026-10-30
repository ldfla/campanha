# CODEX.md — Instruções para o Codex finalizar o Backend

> **Última atualização:** 2026-10-30  
> **Contexto:** O backend da Plataforma de Ação entre Amigos está parcialmente implementado (P00–P05).  
> **Objetivo deste documento:** orientar o Codex a completar **todas as fases backend restantes (P03 pendências, P04 pendências, P05 pendências, P07–P16, P18)** antes de qualquer trabalho de frontend (P06) ou produção (P19–P20).

---

## 1. Estado atual do repositório

### Já implementado e testado (60 testes verdes)

| Fase | O que existe | Arquivos principais |
|------|-------------|-------------------|
| P00 | Estrutura, ADRs, documentação | `docs/adr/`, `docs/architecture/` |
| P01 | Enums de domínio, CapabilityPolicy, ErrorCode | `campaign/CampaignMode.java`, `campaign/CapabilityPolicy.java`, `shared/ErrorCode.java` |
| P02 | Bootstrap Spring Boot 4.1.2, Next.js 16, Docker Compose | `pom.xml`, `compose.yaml`, `.env.example` |
| P03 | Migrações V001–V007 (44 tabelas), outbox/inbox | `db/migration/V001–V007` |
| P04 | Auth por WhatsApp, sessões Spring Session JDBC, RBAC 10 papéis, rate limiting | `identity/*`, `shared/SecurityConfig.java`, `shared/RateLimitFilter.java` |
| P05 | CRUD campanhas, CampaignStateMachine (12 estados), AdminGateMatrix, ApprovalService, CampaignPrize | `campaign/*`, `compliance/Approval*.java` |

### Stack obrigatória

- **Java 25**, Spring Boot 4.1.2, Spring Security 7.x (BOM-managed), Maven Wrapper
- **MySQL 8.4 LTS**, Flyway forward-only, Testcontainers para testes
- **Spring Session JDBC** (cookies HttpOnly/Secure/SameSite, NÃO JWT em localStorage)
- **Dinheiro:** `long` centavos (`Money.java`), nunca `float`/`double`/`BigDecimal` para armazenamento
- **API:** OpenAPI 3.1, `application/problem+json` (RFC 7807) via `ApiProblem.java`
- **Autenticação:** sessão por cookie, CSRF via `CookieCsrfTokenRepository`, excluído para `/auth/*`
- **Testes:** Testcontainers MySQL 8.4, cookie-based session tracking (extrair cookie `SESSION` da response, NÃO usar `MockHttpSession`)
- **Import do AutoConfigureMockMvc:** `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (Spring Boot 4 mudou o pacote)
- **CSRF em testes:** usar `.with(csrf())` de `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors` para POSTs/PUTs/DELETEs em endpoints protegidos

### Convenções de código existentes

- Entidades JPA com `protected` no-arg constructor, construtor público para criação
- Records imutáveis para DTOs (`RegisterRequest`, `CampaignResponse`, etc.)
- `@Transactional` explícito nos serviços, `readOnly = true` para consultas
- Validação com Jakarta Validation nos DTOs, `@Valid` nos controllers
- Erros mapeados via `ErrorCode.toException()` → `BusinessException` → `GlobalExceptionHandler`
- IDs públicos: UUID string (`publicId`), IDs internos: `Long` auto-increment
- Timestamp: `Instant` em JPA, UTC no banco, `America/Sao_Paulo` na UI

---

## 2. Regras obrigatórias para o Codex

### 2.1. Branching e commits

- **NUNCA commitar na `main` diretamente.** Toda alteração em branch `feature/pXX-nome`.
- **NUNCA fazer merge na `main` sem aprovação humana.** Criar PR e aguardar.
- Commits em português com prefixo convencional: `feat(P07):`, `fix(P05):`, `test(P09):`, `docs:`.
- Co-author obrigatório: `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`.

### 2.2. Qualidade mínima

- Todo código novo **deve ter testes** — não marcar checkbox sem teste verde.
- Rodar `./backend/mvnw -B verify` antes de cada commit — BUILD SUCCESS obrigatório.
- Não desabilitar testes existentes, não usar `@Disabled` sem ADR.
- Não criar stubs vazios e marcar como concluído.
- Não usar `any`, `@SuppressWarnings`, `@ts-ignore` sem justificativa.

### 2.3. Segurança (sem exceção)

- Input validation em todo DTO; nunca confiar no frontend.
- Nunca logar CPF completo, tokens, cookies, segredos ou dados de conta.
- `SecureRandom` para qualquer geração de segredos/tokens.
- Todos os endpoints protegidos por `@PreAuthorize` ou regra no `SecurityConfig`.
- Webhooks: verificar assinatura, replay protection, idempotência.
- Prêmio em dinheiro (`CASH`): bloqueado em todos os modos por `allowsCashPrizes() → false`.

### 2.4. Domínio financeiro

- Dinheiro SEMPRE em centavos (`long`), moeda `BRL`, sem arredondamento implícito.
- Participação liberada SOMENTE após evento financeiro confirmado e conciliado (quando modelo exigir pagamento).
- Nunca liberar por QR Code gerado, screenshot ou resposta do frontend.
- Rankings baseados em aquisições pagas: **desativados** por padrão até confirmação legal.

### 2.5. Apuração e sorteio

- Motor determinístico: mesmas entradas → mesmos resultados, sempre.
- Compromisso SHA-256 dos números-surpresa ANTES da campanha, com sal forte.
- Resultado da Loteria Federal: importação manual com dupla aprovação, não presumir API.
- Congelamento de números antes da apuração; nenhuma edição silenciosa posterior.

### 2.6. Documentação por fase

- Ao concluir cada fase, criar `docs/progress/Pxx.md` com evidências.
- Atualizar checkboxes no `IMPLEMENTATION_PLAN.md` somente com evidência.
- Registrar decisões técnicas em `docs/adr/` quando relevante.

---

## 3. Backlog de tarefas backend — Ordem de execução

O Codex deve executar na ordem abaixo. Cada bloco pode ser uma branch separada com PR.

### Bloco A — Completar pendências de P03/P04/P05

**Branch sugerida:** `feature/p03-p05-completion`

#### P03 — Pendências de banco e repositórios
- [ ] `P03-03` Implementar repositórios JPA tipados para TODOS os domínios que ainda não têm (participation, payment, referral, ranking, drawing, award, audit, notification). Cada entidade das migrações V003–V006 precisa de entidade JPA + repositório.
- [ ] `P03-05` Configurar teste de Testcontainers que aplica migrações do zero em schema vazio e valida integridade.
- [ ] `P03-06` Criar seed sintético idempotente (organização demo, usuário admin, campanha demo com prêmios) em profile `local`.
- [ ] `P03-07` Testes de integridade: unicidade de `campaign_id + number_value`, isolamento entre organizações, FK constraints.
- [ ] `P03-08` Documentar schema (ERD), tamanhos de tabelas e política de índices em `docs/architecture/`.

#### P04 — Pendências de autenticação
- [ ] `P04-01` Completar: adicionar verificação por token (mock/sandbox para SMS/WhatsApp — usar adaptador mock que simula envio e aceita código fixo em DEMO).
- [ ] `P04-03` WebAuthn/passkeys: avaliar viabilidade com Spring Security 7.x, implementar ou registrar ADR de adiamento com justificativa.
- [ ] `P04-04` MFA obrigatório para papéis admin (SYSTEM_ADMIN, SECURITY_ADMIN): TOTP com adaptador mock em DEMO.
- [ ] `P04-06` CRUD de usuários/membros/papéis com trilha de auditoria (audit_events).
- [ ] `P04-07` Proteção avançada: bloqueio progressivo por IP, detecção de credential stuffing, rate limit por conta.
- [ ] `P04-08` Testes de IDOR (acesso a recurso de outra organização), CSRF, rotação de sessão.

#### P05 — Pendências de campanhas
- [ ] `P05-03` Upload privado de documentos: verificação de tipo MIME, tamanho máximo, hash SHA-256, armazenamento em MinIO com `compliance_documents`.
- [ ] `P05-04` Versionamento de regulamento: entidade `CampaignRegulation`, hash imutável, vinculação à campanha.
- [ ] `P05-05` Gerenciamento de certificados/autorização: entidade `CampaignAuthorization`, validação de datas, emissor.
- [ ] `P05-09` Separar conteúdo público (`PUBLISHABLE`) de confidencial (`PRIVATE`) em `compliance_documents`, com auditoria de downloads.

### Bloco B — Motor de números e participação (P07)

**Branch sugerida:** `feature/p07-number-engine`

- [ ] `P07-01` Entidades JPA para `campaign_numbers` e `participation_requests`. Gerar série 00000–99999 (ou intervalo configurado).
- [ ] `P07-02` Algoritmo de atribuição aleatória/equitativa com `SecureRandom`, sem seleção manual pelo participante.
- [ ] `P07-03` `ParticipationService` com solicitação idempotente (chave de idempotência), verificação de elegibilidade e atribuição atômica dentro de `@Transactional`.
- [ ] `P07-04` Validação de período da campanha (`startsAt`/`endsAt`), limites por usuário, e público elegível.
- [ ] `P07-05` Constraint `UNIQUE(campaign_id, number_value)` já existe na migration — testar com `SELECT ... FOR UPDATE` para controle de concorrência.
- [ ] `P07-06` Endpoint de consulta de números por campanha com paginação, sem expor dados sensíveis de outros participantes.
- [ ] `P07-07` Estados do número: `AVAILABLE`, `RESERVED`, `ASSIGNED`, `FROZEN`, `INVALIDATED`. Auditoria de cada transição.
- [ ] `P07-08` **Teste de concorrência obrigatório:** 100+ threads tentando reservar o mesmo número — zero duplicatas. Testar 00000, 00001, 99999, esgotamento.
- [ ] `P07-09` Em DEMO, rotular claramente que números não geram direito real a prêmio.

### Bloco C — Indicações e antifraude (P08)

**Branch sugerida:** `feature/p08-referral`

- [ ] `P08-01` Entidade `ReferralLink` com código aleatório não enumerável (`SecureRandom`, base62, 12+ chars).
- [ ] `P08-02` Configurar modelo de atribuição (primeira referência, default DEMO) com janela de validade.
- [ ] `P08-03` Registrar clique mínimo (IP, timestamp, user-agent hash) e atribuir após participação válida.
- [ ] `P08-04` Bloquear self-referral, ciclos (A→B→A), reatribuição arbitrária.
- [ ] `P08-05` Endpoint de painel do divulgador: clicks/cadastros/participações separados, sem identificar indicados (iniciais + últimos 4 dígitos do telefone).
- [ ] `P08-06` Endpoint de geração de link compartilhável.
- [ ] `P08-07` Testes: cookie stuffing, replay de cliques, troca de URL, self-referral.
- [ ] `P08-08` Incentivo remunerado bloqueado até gate de contrato/tributação.

### Bloco D — Pagamentos e Pix sandbox (P09)

**Branch sugerida:** `feature/p09-payment-adapter`

- [ ] `P09-01` Interface `PaymentProvider` com métodos: `createCharge`, `getCharge`, `refund`, `verifyWebhook`.
- [ ] `P09-02` `MockPaymentProvider`: simula cenários — pago imediato, expirado, duplicado, atrasado, falha, estorno. Controlável por parâmetros para testes.
- [ ] `P09-03` `OrderService`: `payment_orders` com status machine (`CREATED` → `PENDING` → `PAID`/`EXPIRED`/`REFUNDED`), `txid` idempotente, valor em centavos.
- [ ] `P09-04` Inbox webhook: validação de assinatura do provedor, replay protection por `event_id` único, persistência em `inbox_events`.
- [ ] `P09-05` Reconciliação: confirmar cobrança com fonte do PSP, não apenas webhook.
- [ ] `P09-06` Worker de reconciliação via `outbox_events` com `FOR UPDATE SKIP LOCKED`, devoluções e ledger.
- [ ] `P09-07` Adaptador real **desativado** (`RealPaymentProvider` com `throw` se `LIVE_OPERATIONS_ENABLED=false`).
- [ ] `P09-08` Testes: pagamento tardio, pedido expirado, webhook fora de ordem, timeout, replay, falha parcial.
- [ ] `P09-09` `COMMERCIAL_PROMOTION` retorna `403 PAYMENT_NOT_ALLOWED` para qualquer tentativa de cobrar.

### Bloco E — Rankings com snapshots (P10)

**Branch sugerida:** `feature/p10-rankings`

- [ ] `P10-01` Entidade `RankingRule` configurável (métrica, modo de cálculo, público elegível).
- [ ] `P10-02` Ranking de divulgadores: por participações atribuídas e elegíveis (usando dados de P08).
- [ ] `P10-03` Ranking de participantes: marcado como SIMULAÇÃO até aprovação legal. Placar em tempo real com iniciais do nome + últimos 4 dígitos do telefone.
- [ ] `P10-04` Desempate determinístico (critério definido antes da abertura: data de participação, ID, etc.).
- [ ] `P10-05` Agregação idempotente com reprocessamento e consistência eventual sinalizada.
- [ ] `P10-06` Snapshot congelado: hash SHA-256, exportação JSON para auditoria.
- [ ] `P10-07` Testes: reversões, estornos, invalidações → efeitos consistentes nos rankings.
- [ ] `P10-08` Pseudônimos na API pública (iniciais + últimos 4 dígitos), nunca nome completo.

### Bloco F — Prêmios-surpresa com commit-reveal (P11)

**Branch sugerida:** `feature/p11-surprise-prizes`

- [ ] `P11-01` Entidade `SurprizePrize` com 10 números aleatórios gerados na inicialização, fixos.
- [ ] `P11-02` Validação: `prize.type` compatível com modalidade. CASH recusado em promoção comercial.
- [ ] `P11-03` Gerador seguro: `SecureRandom`, 10 números únicos no intervalo da campanha, sem repetição.
- [ ] `P11-04` Commit-reveal: canonicalizar dados (JSON sorted, números ordenados), `SHA-256(salt + dados)`, sal forte (32 bytes), segredo cifrado no banco.
- [ ] `P11-05` Publicar hash (commitment) antes da abertura da campanha; registrar timestamp e versão da regra.
- [ ] `P11-06` Revelação: endpoint de verificação que recalcula hash e confirma igualdade. Sem substituição posterior.
- [ ] `P11-07` Testes: falsificação do hash, mudança de segredo, duplicidade de números, revelação prematura.
- [ ] `P11-08` Contingência para número não atribuído: registrar no regulamento, sem algoritmo inventado.

### Bloco G — Motor de apuração (P12)

**Branch sugerida:** `feature/p12-drawing-engine`

- [ ] `P12-01` Entidade `FederalLotteryResult` com data, identificador, evidência e hash da fonte.
- [ ] `P12-02` Importação manual do resultado com dois verificadores independentes (dupla aprovação).
- [ ] `P12-03` `DrawRule` versionada: fórmula declarativa (ex: "último dígito do 1º prêmio da Federal"), vinculada ao regulamento.
- [ ] `P12-04` Congelamento: hash público do conjunto de participações elegíveis antes da apuração.
- [ ] `P12-05` Motor puro: recebe (DrawRule + participações congeladas + resultado Federal) → produz `DrawOutcome` imutável, sem efeitos colaterais.
- [ ] `P12-06` Tratamento: ausências, número 00000, adiamento de extração, divergência — tudo conforme regulamento.
- [ ] `P12-07` Dupla revisão do resultado antes de publicação.
- [ ] `P12-08` Pacote de transparência: JSON com todos os inputs, regra, hash do congelamento, resultado.
- [ ] `P12-09` Testes com vetores fixos: mesmas entradas → mesmos vencedores, sempre.

### Bloco H — Entrega de prêmios (P13)

**Branch sugerida:** `feature/p13-award-delivery`

- [ ] `P13-01` Verificação de beneficiário: contato mínimo validado.
- [ ] `P13-02` Estados do prêmio: `PENDING_VERIFICATION` → `VALIDATED` → `SCHEDULED` → `DELIVERED` → `DISPUTED`.
- [ ] `P13-03` Evidência de entrega: armazenamento com hash, acesso restrito.
- [ ] `P13-04` Tratamento: não localização, recusa, contestação, prazo.
- [ ] `P13-05` Transferência Pix de prêmio bloqueada quando modalidade proibir dinheiro.
- [ ] `P13-06` Relatório final com minimização de PII.

### Bloco I — Dashboard financeiro (P14)

**Branch sugerida:** `feature/p14-financial-dashboard`

- [ ] `P14-01` Modelo de custos por categoria.
- [ ] `P14-02` Referência de R$ 18.500 em itens propostos (prêmios + comissões conforme AGENTS.md).
- [ ] `P14-03` Simulador parametrizável: custo unitário R$ 0,56, receita líquida R$ 50.000, taxas, tributos, ponto de equilíbrio.
- [ ] `P14-04` Rotular como simulação hipotética.
- [ ] `P14-05` Relatórios por período/campanha/organização.
- [ ] `P14-06` Confrontar ordens × cobranças × recebimentos × estornos × ledger.
- [ ] `P14-07` Exportar CSV (seguro contra formula injection) com acesso auditado.
- [ ] `P14-08` Tributos como parâmetros, não regime fiscal automático.

### Bloco J — Notificações (P15)

**Branch sugerida:** `feature/p15-notifications`

- [ ] `P15-01` Templates de e-mail em PT-BR com versionamento.
- [ ] `P15-02` Eventos: cadastro, participação válida, mudança administrativa, resultado, entrega.
- [ ] `P15-03` Processamento via outbox, retry, deduplicação, opt-out.
- [ ] `P15-04` Canal de suporte com permissões e logging.
- [ ] `P15-05` Diferenciar comunicação essencial de marketing.
- [ ] `P15-06` Em DEMO: simular envios via Mailpit local, nunca enviar para contatos reais.

### Bloco K — Segurança e LGPD (P16)

**Branch sugerida:** `feature/p16-security-lgpd`

- [ ] `P16-01` Threat model STRIDE com diagramas de trust boundaries.
- [ ] `P16-02` Checklist OWASP ASVS 2 e API Top 10.
- [ ] `P16-03` Security headers (CSP, HSTS), CORS estrito, rate limits aprimorados.
- [ ] `P16-04` PII cifrada em campos sensíveis (CPF via criptografia de campo, índice HMAC para busca).
- [ ] `P16-05` Portal LGPD: fluxo de direitos (acesso, correção, exclusão, exportação).
- [ ] `P16-06` SAST, SCA, secrets scan e SBOM configurados em CI.
- [ ] `P16-07` Red-team local: `docs/security/red-team-report.md` com ataques de domínio.
- [ ] `P16-08` Corrigir achados críticos/altos, reexecutar testes.
- [ ] `P16-09` Plano de incidente e continuidade.

### Bloco L — Carga e recuperação (P18)

**Branch sugerida:** `feature/p18-load-recovery`

- [ ] `P18-01` Gerar 100.000 números/participações simuladas, medir tempos.
- [ ] `P18-02` Teste de concorrência: 100/250/500 threads, mesmo número, deadlock.
- [ ] `P18-03` Reconciliação com webhooks duplicados, atrasados e em ordem inversa.
- [ ] `P18-04` Injetar falha entre persistência e notificação; comprovar outbox e recovery.
- [ ] `P18-05` Medir p95/p99 com `EXPLAIN` nos queries críticos.
- [ ] `P18-06` Testar backup/restauração MySQL.
- [ ] `P18-07` Testar congelamento concorrente com novas participações.
- [ ] `P18-08` Registrar limites medidos em `docs/progress/P18.md`.

---

## 4. Ordem de execução recomendada

```
1. Bloco A (P03/P04/P05 pendências) — base para tudo
2. Bloco B (P07 motor de números) — depende de P03/P04/P05
3. Bloco C (P08 referral) — depende de P04/P07
4. Bloco D (P09 pagamentos) — depende de P05/P07
5. Bloco E (P10 rankings) — depende de P07/P08/P09
6. Bloco F (P11 surpresa) — depende de P07
7. Bloco G (P12 apuração) — depende de P07/P11
8. Bloco H (P13 entrega) — depende de P12
9. Bloco I (P14 financeiro) — depende de P09
10. Bloco J (P15 notificações) — depende de P07/P09/P12
11. Bloco K (P16 segurança) — após todos os módulos
12. Bloco L (P18 carga) — após todos os módulos
```

Blocos independentes entre si (como B e D, ou F e I) podem ser paralelizados se o agente tiver capacidade.

---

## 5. O que NÃO fazer

1. **Não implementar frontend (P06, P17, P19, P20)** até que todo o backend esteja completo e testado.
2. **Não ativar pagamentos reais** — usar `MockPaymentProvider` sempre.
3. **Não marcar checkboxes sem teste verde** e evidência em `docs/progress/`.
4. **Não hardcodar** credenciais, CPFs, valores de gateway, datas oficiais ou certificados.
5. **Não inventar** resultados de testes, autorização jurídica, homologação de PSP.
6. **Não usar** `SKIP LOCKED` para rankings ou apurações (apenas para filas outbox/inbox).
7. **Não expor** nome completo em rankings públicos (apenas iniciais + últimos 4 dígitos do telefone).
8. **Não criar** modalidade `PRIVATE_RAFFLE` ou qualquer variação não prevista nos 3 modos.
9. **Não pular** testes de concorrência em P07 e P18 — são obrigatórios.
10. **Não fazer** push para `origin/main` — sempre via PR com aprovação.

---

## 6. Checklist de validação final do backend

Antes de declarar backend completo, o Codex deve verificar:

- [ ] `./backend/mvnw -B verify` passa com TODOS os testes
- [ ] Todas as entidades JPA validam contra o schema Flyway (`ddl-auto=validate`)
- [ ] Todos os endpoints protegidos por papel/recurso (testar IDOR)
- [ ] Nenhum log contém CPF completo, token ou segredo
- [ ] `Money` usado em centavos em todos os fluxos financeiros
- [ ] `CampaignStateMachine` cobre todas as transições
- [ ] `AdminGateMatrix` bloqueia modos não-DEMO sem gates
- [ ] Prêmio em dinheiro bloqueado em todos os modos
- [ ] Motor de apuração é determinístico (vetores fixos)
- [ ] Números-surpresa com commit-reveal SHA-256 verificável
- [ ] Concorrência: zero duplicatas em 100+ threads
- [ ] `docs/progress/Pxx.md` existe para cada fase concluída
- [ ] `IMPLEMENTATION_PLAN.md` tem checkboxes atualizados
- [ ] `docs/security/red-team-report.md` produzido (P16)

---

## 7. Referências obrigatórias

- [`AGENTS.md`](./AGENTS.md) — contrato de execução completo com regras de domínio
- [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md) — roadmap com checkboxes e especificações detalhadas
- [`docs/progress/`](./docs/progress/) — evidências de cada fase
- [`docs/adr/`](./docs/adr/) — decisões arquiteturais registradas

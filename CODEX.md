# CODEX.md — Instruções para o Codex finalizar o Backend

> **Última atualização:** 2026-10-31  
> **Contexto:** O backend da Plataforma de Ação entre Amigos tem P00–P10 concluídos e merged na main. Existem 4 PRs em draft (P11, P13, P14, P15) com implementação parcial.  
> **Objetivo deste documento:** orientar o Codex a completar **todos os blocos backend restantes** — finalizando os PRs em draft e implementando P12 (apuração), P16 (segurança/LGPD) e P18 (carga/recuperação).

---

## 1. Estado atual do repositório

### Merged na main (PRs #1–#10, #15)

| Fase | O que existe | Status |
|------|-------------|--------|
| P00 | Estrutura, ADRs, documentação | Completo |
| P01 | Enums de domínio, CapabilityPolicy, ErrorCode | Completo |
| P02 | Bootstrap Spring Boot 4.1.2, Next.js 16, Docker Compose | Completo |
| P03 | Migrações V001–V007 (44 tabelas), outbox/inbox | Completo |
| P04 | Auth WhatsApp, sessões JDBC, RBAC 10 papéis, rate limiting | Completo |
| P05 | CRUD campanhas, StateMachine (12 estados), AdminGateMatrix, ApprovalService | Completo |
| P06 | Landing page, integração frontend-backend, UI pública | Completo (PR #15 merged) |
| P07 | Motor de números, 100k posições, atribuição atômica, concorrência | Completo (PR #7 merged) |
| P08 | Links de indicação, atribuição e antifraude | Completo (PR #8 merged) |
| P09 | Pagamentos sandbox, webhook idempotente e ledger | Completo (PR #9 merged) |
| P10 | Rankings pseudonimizados, desempate determinístico, snapshots | Completo (PR #10 merged) |

### PRs em draft (trabalho parcial — completar)

| PR | Branch | Fase | O que tem | O que falta |
|----|--------|------|-----------|-------------|
| #12 | `feature/p11-completion` | P11 | Compromisso criptográfico inicial em DEMO | Commit-reveal completo, gerador de 2 tiers, testes |
| #13 | `feature/p13-award-delivery` | P13 | Entrega simulada de prêmios | Estados completos, evidência, disputas, Pix bloqueado |
| #14 | `feature/p14-financial-dashboard` | P14 | Simulação financeira hipotética | Modelo de custos, reconciliação, relatórios, CSV |
| #16 | `feature/p15-notifications` | P15 | Textos essenciais versionados | Outbox, retry, opt-out, Mailpit, testes |

### Ainda sem branch/PR

| Fase | Descrição |
|------|-----------|
| P12 | Motor de apuração determinístico |
| P16 | Segurança, LGPD, Red Team |
| P18 | Carga, recuperação, backup |

---

## 2. Stack obrigatória e premissa de engenharia Java 25

### Stack

- **Java 25** (OpenJDK 25.0.3 LTS), Spring Boot 4.1.2, Spring Security 7.x (BOM-managed), Maven Wrapper
- **MySQL 8.4 LTS**, Flyway forward-only, Testcontainers para testes
- **Spring Session JDBC** (cookies HttpOnly/Secure/SameSite, NÃO JWT em localStorage)
- **Dinheiro:** `long` centavos (`Money.java`), nunca `float`/`double`/`BigDecimal` para armazenamento
- **API:** OpenAPI 3.1, `application/problem+json` (RFC 7807) via `ApiProblem.java`
- **Autenticação:** sessão por cookie, CSRF via `CookieCsrfTokenRepository`, excluído para `/auth/*`
- **Testes:** Testcontainers MySQL 8.4, cookie-based session tracking (extrair cookie `SESSION` da response, NÃO usar `MockHttpSession`)
- **Import AutoConfigureMockMvc:** `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (Spring Boot 4 mudou o pacote)
- **CSRF em testes:** usar `.with(csrf())` de `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors`

### Premissa de Engenharia Java 25 (referência: `JAVA-ENGINEERING-GUIDE.md`)

Regras obrigatórias para todo código Java:

1. **Null safety com JSpecify** (`org.jspecify.annotations`): usar `@NullMarked` em `package-info.java` de pacotes novos/alterados; marcar exceções com `@Nullable`. NÃO usar JSR-305, JetBrains Annotations nem `org.springframework.lang` em código novo.

2. **Fail-fast na entrada:** `Objects.requireNonNull(param, "param")` para referências obrigatórias, `IllegalArgumentException` para valor fora do contrato, `IllegalStateException` para estado inválido. Nunca deixar `NullPointerException` estourar longe da causa.

3. **Design by Contract:** cada método público relevante deixa explícitos pré-condições, pós-condições e invariantes. `record` valida no construtor canônico compacto. Coleções copiadas com `List.copyOf`/`Map.copyOf`.

4. **Virtual threads:** avaliar para trabalho bloqueante concorrente com ganho demonstrável. Limitar concorrência pelo recurso real (conexões Hikari, APIs externas). NÃO usar `parallelStream()` para JDBC, reserva, fila ou efeitos externos.

5. **Pattern matching:** usar `instanceof` com padrão e `switch` sobre `sealed`/`enum` para tornar estados inválidos irrepresentáveis.

6. **Mensagens de exceção:** códigos estáveis e fechados (`lock.pin.invalid_length`), sem dados pessoais, segredos ou texto livre. Fronteira HTTP traduz para Problem Details.

7. **Optional:** retorno que pode não ter valor é `Optional<T>`; métodos públicos não devolvem `null`. NÃO usar em campos, parâmetros nem coleções.

### Convenções de código existentes

- Entidades JPA com `protected` no-arg constructor, construtor público para criação
- Records imutáveis para DTOs (`RegisterRequest`, `CampaignResponse`, etc.)
- `@Transactional` explícito nos serviços, `readOnly = true` para consultas
- Validação com Jakarta Validation nos DTOs, `@Valid` nos controllers
- Erros mapeados via `ErrorCode.toException()` → `BusinessException` → `GlobalExceptionHandler`
- IDs públicos: UUID string (`publicId`), IDs internos: `Long` auto-increment
- Timestamp: `Instant` em JPA, UTC no banco, `America/Sao_Paulo` na UI

---

## 3. Regras obrigatórias

### 3.1. Branching e commits

- **NUNCA commitar na `main` diretamente.** Toda alteração em branch `feature/pXX-nome`.
- **NUNCA fazer merge na `main` sem aprovação humana.** Criar PR e aguardar.
- Para PRs em draft existentes: fazer checkout da branch, continuar o trabalho, push e atualizar o PR.
- Commits em português com prefixo convencional: `feat(P07):`, `fix(P05):`, `test(P09):`, `docs:`.
- Co-author obrigatório: `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`.

### 3.2. Qualidade mínima

- Todo código novo **deve ter testes** — não marcar checkbox sem teste verde.
- Rodar `./backend/mvnw -B verify` antes de cada commit — BUILD SUCCESS obrigatório.
- Não desabilitar testes existentes, não usar `@Disabled` sem ADR.
- Não criar stubs vazios e marcar como concluído.
- Não usar `any`, `@SuppressWarnings`, `@ts-ignore` sem justificativa.

### 3.3. Segurança (sem exceção)

- Input validation em todo DTO; nunca confiar no frontend.
- Nunca logar CPF completo, tokens, cookies, segredos ou dados de conta.
- `SecureRandom` para qualquer geração de segredos/tokens.
- Todos os endpoints protegidos por `@PreAuthorize` ou regra no `SecurityConfig`.
- Webhooks: verificar assinatura, replay protection, idempotência.
- Prêmio em dinheiro (`CASH`): bloqueado em todos os modos por `allowsCashPrizes() → false`.

### 3.4. Domínio financeiro

- Dinheiro SEMPRE em centavos (`long`), moeda `BRL`, sem arredondamento implícito.
- Participação liberada SOMENTE após evento financeiro confirmado e conciliado.
- Nunca liberar por QR Code gerado, screenshot ou resposta do frontend.
- Rankings baseados em aquisições pagas: **desativados** por padrão até confirmação legal.

### 3.5. Apuração e sorteio

- Motor determinístico: mesmas entradas → mesmos resultados, sempre.
- Compromisso SHA-256 dos números-surpresa ANTES da campanha, com sal forte.
- Resultado da Loteria Federal: importação manual com dupla aprovação.
- Congelamento de números antes da apuração; nenhuma edição silenciosa posterior.

### 3.6. Documentação por fase

- Ao concluir cada fase, criar/atualizar `docs/progress/Pxx.md` com evidências.
- Atualizar checkboxes no `IMPLEMENTATION_PLAN.md` somente com evidência.
- Registrar decisões técnicas em `docs/adr/` quando relevante.

---

## 4. Lacunas identificadas no fluxo de compra — IMPLEMENTAR

Estas lacunas foram identificadas na análise do código existente e precisam ser corrigidas:

### 4.1. Voto na entidade beneficiada no momento da compra

O `CreateParticipationRequest` atual tem apenas `idempotencyKey` e `count`. Falta o campo de escolha da entidade beneficiada.

**O que fazer:**
- Adicionar campo `entityId` (ou `charityVote`) ao `CreateParticipationRequest` DTO.
- Criar/usar entidade `BeneficiaryEntity` (GRAAC, Casa Hope, AACD, TUCCA, ITACI, AACC) com tabela correspondente.
- Registrar o voto vinculado à participação, permitir apuração posterior.
- Validar que a entidade existe e está ativa para a campanha.
- Testes: entidade inválida → 400, entidade inativa → 400, voto registrado corretamente.

### 4.2. Rastreamento de indicações no fluxo de participação

O `ReferralService.attribute()` existe mas **não é chamado** pelo fluxo de participação. O frontend `/participar` não captura nem envia o código de indicação.

**O que fazer:**
- Adicionar campo opcional `referralCode` ao `CreateParticipationRequest`.
- No `ParticipationService.request()`, após atribuir números, chamar `referralService.attribute()` com o código de indicação, se presente.
- Validar que o referralCode existe, não é auto-indicação, e está dentro da janela de validade.
- Testes: indicação válida → atribuição registrada, auto-indicação → bloqueada, código inválido → ignorado (sem erro).

### 4.3. TTL de reserva para números não pagos

O `ReservationLease` existe como entidade mas **não está conectado** ao fluxo. Atualmente números são marcados como `ASSIGNED` imediatamente, sem TTL.

**O que fazer:**
- Implementar o fluxo de reserva temporária: números ficam `RESERVED` com `expiresAt`.
- Worker periódico (via outbox) que libera números cujo lease expirou sem pagamento.
- Transição `RESERVED` → `ASSIGNED` somente após confirmação de pagamento.
- Testes: lease expirado → números voltam a `AVAILABLE`, pagamento no prazo → `ASSIGNED`, concorrência na expiração.

---

## 5. Parâmetros da campanha de referência

Estes são os valores configurados no frontend (`campaign-config.ts`) que o backend deve suportar:

### Combos de números

| Combo | Qtd | Unitário | Total | Badge |
|-------|-----|----------|-------|-------|
| Curioso | 10 | R$ 0,65 | R$ 6,50 | — |
| Torcida | 100 | R$ 0,50 | R$ 50,00 | Mais Popular |
| Campeão | 1.000 | R$ 0,46 | R$ 460,00 | Máximo Desconto |
| Anjo | 5.000 | R$ 0,44 | R$ 2.200,00 | Apoiador |

### Prêmios principais (5 posições)

| Posição | Prêmio | Valor |
|---------|--------|-------|
| 1º | Kit Fotográfico Canon (D70 + Lente 24-70 f/4 + Flash 430EX) | R$ 10.000 |
| 2º | MacBook Air M1 13" 8GB | R$ 4.000 |
| 3º | Kit Canon T1i Rebel | R$ 2.000 |
| 4º | Pix | R$ 1.000 |
| 5º | Pix | R$ 500 |

### Prêmios-surpresa (2 tiers)

| Tier | Qtd | Valor cada | Total |
|------|-----|-----------|-------|
| Tier 1 | 10 | R$ 100 | R$ 1.000 |
| Tier 2 | 100 | R$ 10 | R$ 1.000 |

Números aleatórios gerados na inicialização com SecureRandom, commit-reveal SHA-256.

### Ranking de recompensas (2 escopos)

| Escopo | 1º lugar | 2º lugar | 3º lugar | Desempate |
|--------|----------|----------|----------|-----------|
| Maiores Apoiadores (BUYERS) | R$ 1.500 | R$ 900 | R$ 500 | Data da primeira cota |
| Maiores Divulgadores (INFLUENCERS) | R$ 1.500 | R$ 900 | R$ 500 | Data da primeira cota |

Pseudônimos: iniciais do nome + últimos 4 dígitos do telefone.

### Total em prêmios: R$ 25.300

- Prêmios principais: R$ 17.500
- Prêmios-surpresa: R$ 2.000
- Rankings: R$ 5.800

---

## 6. Backlog de tarefas — Ordem de execução

### Bloco A — Lacunas do fluxo de compra (PRIORITÁRIO)

**Branch sugerida:** `feature/p07-purchase-flow-gaps`

- [ ] Adicionar campo `entityId` ao `CreateParticipationRequest` e registrar voto na entidade beneficiada (seção 4.1)
- [ ] Conectar `referralCode` ao fluxo de participação, chamar `ReferralService.attribute()` (seção 4.2)
- [ ] Implementar `ReservationLease` com TTL e worker de expiração (seção 4.3)
- [ ] Testes de integração para os 3 fluxos corrigidos
- [ ] Atualizar OpenAPI/contratos

### Bloco B — Completar P11 (PR #12 em draft)

**Branch existente:** `feature/p11-completion`

- [ ] `P11-01` Implementar 2 tiers de prêmios-surpresa (10×R$100 + 100×R$10) conforme seção 5
- [ ] `P11-02` Validação: `prize.type` compatível com modalidade. CASH recusado em promoção comercial.
- [ ] `P11-03` Gerador seguro: `SecureRandom`, 110 números únicos no intervalo da campanha.
- [ ] `P11-04` Commit-reveal: canonicalizar dados (JSON sorted), `SHA-256(salt + dados)`, sal forte (32 bytes), segredo cifrado.
- [ ] `P11-05` Publicar hash (commitment) antes da abertura; timestamp e versão da regra.
- [ ] `P11-06` Endpoint de verificação que recalcula hash e confirma igualdade. Sem substituição posterior.
- [ ] `P11-07` Testes: falsificação, mudança de segredo, duplicidade, revelação prematura.
- [ ] `P11-08` Contingência para número não atribuído.

### Bloco C — Motor de apuração (P12, sem branch)

**Branch sugerida:** `feature/p12-drawing-engine`

- [ ] `P12-01` Entidade `FederalLotteryResult` com data, identificador, evidência e hash.
- [ ] `P12-02` Importação manual com dupla aprovação (dois verificadores independentes).
- [ ] `P12-03` `DrawRule` versionada: fórmula declarativa vinculada ao regulamento.
- [ ] `P12-04` Congelamento: hash público das participações elegíveis antes da apuração.
- [ ] `P12-05` Motor puro: (DrawRule + participações congeladas + resultado Federal) → `DrawOutcome` imutável.
- [ ] `P12-06` Tratamento: ausências, 00000, adiamento, divergência — conforme regulamento.
- [ ] `P12-07` Dupla revisão antes de publicação.
- [ ] `P12-08` Pacote de transparência: JSON com inputs, regra, hash, resultado.
- [ ] `P12-09` Testes com vetores fixos: mesmas entradas → mesmos vencedores.

### Bloco D — Completar P13 (PR #13 em draft)

**Branch existente:** `feature/p13-award-delivery`

- [ ] `P13-01` Verificação de beneficiário: contato mínimo validado.
- [ ] `P13-02` Estados: `PENDING_VERIFICATION` → `VALIDATED` → `SCHEDULED` → `DELIVERED` → `DISPUTED`.
- [ ] `P13-03` Evidência de entrega: armazenamento com hash, acesso restrito.
- [ ] `P13-04` Tratamento: não localização, recusa, contestação, prazo.
- [ ] `P13-05` Pix bloqueado quando modalidade proibir dinheiro.
- [ ] `P13-06` Relatório final com minimização de PII.

### Bloco E — Completar P14 (PR #14 em draft)

**Branch existente:** `feature/p14-financial-dashboard`

- [ ] `P14-01` Modelo de custos por categoria.
- [ ] `P14-02` Referência: R$ 25.300 em prêmios/comissões conforme seção 5.
- [ ] `P14-03` Simulador parametrizável: custos, receita, taxas, tributos, ponto de equilíbrio.
- [ ] `P14-04` Rotular como simulação hipotética.
- [ ] `P14-05` Relatórios por período/campanha/organização.
- [ ] `P14-06` Confrontar ordens × cobranças × recebimentos × estornos × ledger.
- [ ] `P14-07` Exportar CSV (seguro contra formula injection) com acesso auditado.
- [ ] `P14-08` Tributos como parâmetros, não regime fiscal automático.

### Bloco F — Completar P15 (PR #16 em draft)

**Branch existente:** `feature/p15-notifications`

- [ ] `P15-01` Templates de e-mail em PT-BR com versionamento.
- [ ] `P15-02` Eventos: cadastro, participação, mudança administrativa, resultado, entrega.
- [ ] `P15-03` Processamento via outbox, retry, deduplicação, opt-out.
- [ ] `P15-04` Canal de suporte com permissões e logging.
- [ ] `P15-05` Diferenciar comunicação essencial de marketing.
- [ ] `P15-06` Em DEMO: Mailpit local, nunca contatos reais.

### Bloco G — Segurança e LGPD (P16)

**Branch sugerida:** `feature/p16-security-lgpd`

- [ ] `P16-01` Threat model STRIDE com trust boundaries.
- [ ] `P16-02` Checklist OWASP ASVS 2 e API Top 10.
- [ ] `P16-03` Security headers (CSP, HSTS), CORS estrito, rate limits aprimorados.
- [ ] `P16-04` PII cifrada: CPF via criptografia de campo, índice HMAC para busca.
- [ ] `P16-05` Portal LGPD: acesso, correção, exclusão, exportação.
- [ ] `P16-06` SAST, SCA, secrets scan, SBOM em CI.
- [ ] `P16-07` Red-team local: `docs/security/red-team-report.md`.
- [ ] `P16-08` Corrigir achados críticos/altos, reexecutar testes.
- [ ] `P16-09` Plano de incidente e continuidade.

### Bloco H — Carga e recuperação (P18)

**Branch sugerida:** `feature/p18-load-recovery`

- [ ] `P18-01` Gerar 100.000 números/participações simuladas.
- [ ] `P18-02` Concorrência: 100/250/500 threads, zero duplicatas, deadlock test.
- [ ] `P18-03` Reconciliação com webhooks duplicados, atrasados, ordem inversa.
- [ ] `P18-04` Injetar falha entre persistência e notificação; comprovar outbox recovery.
- [ ] `P18-05` p95/p99 com `EXPLAIN` nos queries críticos.
- [ ] `P18-06` Backup/restauração MySQL.
- [ ] `P18-07` Congelamento concorrente com novas participações.
- [ ] `P18-08` Registrar limites em `docs/progress/P18.md`.

---

## 7. Ordem de execução recomendada

```
1. Bloco A (lacunas do fluxo de compra) — PRIORITÁRIO, base para tudo
2. Bloco B (P11 surpresa) — completar PR existente
3. Bloco C (P12 apuração) — depende de P07/P11
4. Bloco D (P13 entrega) — completar PR, depende de P12
5. Bloco E (P14 financeiro) — completar PR, depende de P09
6. Bloco F (P15 notificações) — completar PR, depende de P07/P09/P12
7. Bloco G (P16 segurança) — após todos os módulos
8. Bloco H (P18 carga) — após todos os módulos
```

Blocos E e F são independentes entre si e podem ser paralelizados.

---

## 8. O que NÃO fazer

1. **Não implementar frontend** — o frontend (P06) já está completo e merged.
2. **Não ativar pagamentos reais** — usar `MockPaymentProvider` sempre.
3. **Não marcar checkboxes sem teste verde** e evidência em `docs/progress/`.
4. **Não hardcodar** credenciais, CPFs, valores de gateway, datas oficiais ou certificados.
5. **Não inventar** resultados de testes, autorização jurídica, homologação de PSP.
6. **Não usar** `SKIP LOCKED` para rankings ou apurações (apenas para filas outbox/inbox).
7. **Não expor** nome completo em rankings públicos (apenas iniciais + últimos 4 dígitos do telefone).
8. **Não criar** modalidade `PRIVATE_RAFFLE` ou variação não prevista nos 3 modos.
9. **Não pular** testes de concorrência — são obrigatórios.
10. **Não fazer** push para `origin/main` — sempre via PR com aprovação.
11. **Não usar** JSR-305, JetBrains Annotations nem `org.springframework.lang` — usar JSpecify.
12. **Não devolver** `null` de métodos públicos — usar `Optional<T>`.
13. **Não usar** `parallelStream()` para JDBC, reserva, fila ou efeitos externos.

---

## 9. Checklist de validação final do backend

Antes de declarar backend completo:

- [ ] `./backend/mvnw -B verify` passa com TODOS os testes
- [ ] Todas as entidades JPA validam contra o schema Flyway (`ddl-auto=validate`)
- [ ] Todos os endpoints protegidos por papel/recurso (testar IDOR)
- [ ] Nenhum log contém CPF completo, token ou segredo
- [ ] `Money` usado em centavos em todos os fluxos financeiros
- [ ] `CampaignStateMachine` cobre todas as transições
- [ ] `AdminGateMatrix` bloqueia modos não-DEMO sem gates
- [ ] Prêmio em dinheiro bloqueado em todos os modos
- [ ] Motor de apuração é determinístico (vetores fixos)
- [ ] Números-surpresa com commit-reveal SHA-256 verificável (2 tiers: 10×R$100 + 100×R$10)
- [ ] Concorrência: zero duplicatas em 100+ threads
- [ ] Voto na entidade beneficiada registrado no fluxo de compra
- [ ] Indicação (`referralCode`) conectada ao fluxo de participação
- [ ] `ReservationLease` com TTL e worker de expiração funcionando
- [ ] Rankings com 2 escopos (BUYERS, INFLUENCERS), 3 posições cada, pseudônimos
- [ ] JSpecify `@NullMarked` em pacotes novos, fail-fast com `Objects.requireNonNull`
- [ ] `docs/progress/Pxx.md` existe para cada fase concluída
- [ ] `IMPLEMENTATION_PLAN.md` tem checkboxes atualizados
- [ ] `docs/security/red-team-report.md` produzido (P16)

---

## 10. Referências obrigatórias

- [`AGENTS.md`](./AGENTS.md) — contrato de execução completo com regras de domínio
- [`JAVA-ENGINEERING-GUIDE.md`](./JAVA-ENGINEERING-GUIDE.md) — premissa de engenharia Java 25 (null safety, fail-fast, virtual threads, Design by Contract)
- [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md) — roadmap com checkboxes e especificações detalhadas
- [`docs/progress/`](./docs/progress/) — evidências de cada fase
- [`docs/adr/`](./docs/adr/) — decisões arquiteturais registradas

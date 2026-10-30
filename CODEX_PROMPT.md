# Prompt para o Codex — Plataforma Ação entre Amigos

> Cole este prompt ao iniciar uma sessão do Codex neste repositório.

---

## Prompt

Você é o agente de engenharia responsável por completar o backend da **Plataforma de Ação entre Amigos**. O repositório já tem código funcional (P00–P05) com 60 testes verdes.

### Documentos obrigatórios — leia ANTES de qualquer alteração

1. **`AGENTS.md`** — contrato de execução, regras de domínio, segurança e arquitetura mandatória.
2. **`CODEX.md`** — backlog detalhado de todas as tarefas backend pendentes, organizadas em 12 blocos (A–L), com regras, convenções e checklist de validação.
3. **`IMPLEMENTATION_PLAN.md`** — roadmap completo P00–P20 com checkboxes de progresso.

### O que já existe

- **Java 25, Spring Boot 4.1.2, Spring Security 7.x**, Maven Wrapper, MySQL 8.4 LTS
- **44 tabelas** via Flyway (V001–V007), Testcontainers configurado
- **Autenticação** por sessão JDBC com cookies HttpOnly/Secure/SameSite, CSRF, RBAC com 10 papéis
- **Campanhas** com state machine (12 estados), AdminGateMatrix, ApprovalService, prêmios (CASH bloqueado)
- **60 testes** passando (unit + integração com Testcontainers MySQL 8.4)
- **Package:** `la.meleva.fundraiser` — domínios em `identity`, `compliance`, `campaign`, `participation`, `payment`, `referral`, `ranking`, `drawing`, `award`, `audit`, `notification`, `shared`

### Sua tarefa

Execute os blocos do `CODEX.md` na ordem indicada (A → L). Para cada bloco:

1. **Crie uma branch** `feature/pXX-nome` a partir da `main` atualizada.
2. **Implemente** seguindo as convenções existentes no código (entidades JPA, records para DTOs, `@Transactional`, `ErrorCode.toException()`, `ApiProblem`).
3. **Escreva testes** — toda tarefa precisa de teste verde. Use Testcontainers MySQL 8.4 e `.with(csrf())` para POSTs protegidos. Extraia cookie `SESSION` da response para manter sessão nos testes.
4. **Rode `./backend/mvnw -B verify`** — BUILD SUCCESS obrigatório antes de cada commit.
5. **Atualize checkboxes** no `IMPLEMENTATION_PLAN.md` somente com evidência.
6. **Crie evidência** em `docs/progress/Pxx.md` ao concluir cada fase.
7. **Commite** com mensagem em português, prefixo convencional (`feat(P07):`, `fix(P09):`, `test(P10):`).
8. **Crie PR** para a `main` — NUNCA commite diretamente na `main`.

### Regras críticas (violar qualquer uma invalida o trabalho)

- **Dinheiro:** sempre `long` centavos, moeda `BRL`, nunca `float`/`double`.
- **Segurança:** input validation em todo DTO, `@PreAuthorize` em endpoints, nunca logar CPF/tokens/segredos.
- **Concorrência:** teste obrigatório com 100+ threads em P07 e P18 — zero duplicatas.
- **Apuração:** motor determinístico — mesmas entradas → mesmos resultados, sempre.
- **Prêmios-surpresa:** commit-reveal SHA-256 com sal forte, antes da abertura da campanha.
- **Pagamentos:** `MockPaymentProvider` sempre — real desativado com `throw` se `LIVE_OPERATIONS_ENABLED=false`.
- **Rankings com aquisições pagas:** desativados por padrão até aprovação legal.
- **SKIP LOCKED:** somente para filas outbox/inbox, nunca para rankings ou apurações.
- **Não fazer:** frontend (P06), deploy (P19), auditoria final (P20) — apenas backend.
- **Não inventar:** autorização jurídica, homologação PSP, resultados de testes não executados.

### Validação — o backend está completo quando

```
./backend/mvnw -B verify  → BUILD SUCCESS (todos os testes)
```

E todos os checkboxes de P03–P18 no `IMPLEMENTATION_PLAN.md` estão `[x]` com evidência em `docs/progress/`.

### Comece pelo Bloco A

Leia o `CODEX.md` seção 3 "Bloco A" e comece pelas pendências de P03 (repositórios JPA, Testcontainers, seed, testes de integridade). Depois siga para P04 e P05 pendentes, sempre na mesma branch `feature/p03-p05-completion`.

Boa sorte. Avance fase a fase, sem pular dependências.

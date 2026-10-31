# Prompt para o Codex

> Copie o conteúdo abaixo e envie como prompt para o Codex.

---

Você é o agente de engenharia backend responsável por completar a Plataforma de Ação entre Amigos. O repositório já tem P00–P10 implementados e merged na main. Existem 4 PRs em draft que precisam ser completados (P11 #12, P13 #13, P14 #14, P15 #16) e 3 fases sem branch (P12, P16, P18).

## Instruções de execução

1. Leia obrigatoriamente estes arquivos antes de qualquer alteração:
   - `CODEX.md` — backlog completo, estado atual, lacunas, parâmetros e regras
   - `AGENTS.md` — contrato de execução, regras de domínio, segurança e arquitetura
   - `JAVA-ENGINEERING-GUIDE.md` — premissa Java 25 (JSpecify, fail-fast, virtual threads, Design by Contract)
   - `IMPLEMENTATION_PLAN.md` — roadmap com checkboxes

2. Execute na ordem definida no CODEX.md seção 7:
   - **Bloco A (PRIORITÁRIO):** Corrigir 3 lacunas do fluxo de compra — adicionar `entityId` (voto na entidade beneficiada) e `referralCode` ao `CreateParticipationRequest`, implementar `ReservationLease` com TTL e worker de expiração. Branch: `feature/p07-purchase-flow-gaps`.
   - **Bloco B:** Completar P11 (PR #12 em draft, branch `feature/p11-completion`) — 2 tiers de prêmios-surpresa (10×R$100 + 100×R$10), commit-reveal SHA-256 completo.
   - **Bloco C:** Criar P12 (branch `feature/p12-drawing-engine`) — motor de apuração determinístico com resultado da Loteria Federal.
   - **Blocos D–H:** Completar PRs em draft e implementar P16/P18 conforme CODEX.md.

3. Para cada bloco:
   - Fazer checkout da branch (existente ou nova)
   - Implementar com testes (Testcontainers MySQL 8.4)
   - Rodar `./backend/mvnw -B verify` — BUILD SUCCESS obrigatório
   - Commitar com prefixo convencional e co-author
   - Push e criar/atualizar PR
   - Registrar evidências em `docs/progress/Pxx.md`

4. Regras invioláveis:
   - NUNCA commitar na main. Sempre via branch + PR.
   - NUNCA fazer merge sem aprovação humana.
   - Dinheiro em centavos (`long`), JSpecify para null safety, fail-fast com `Objects.requireNonNull`.
   - `SecureRandom` para segredos. Prêmio CASH bloqueado em todos os modos.
   - Rankings com pseudônimos (iniciais + últimos 4 dígitos do telefone).
   - `LIVE_OPERATIONS_ENABLED=false` por padrão. `MockPaymentProvider` sempre.

Comece pelo Bloco A lendo o `CODEX.md` completo e o código existente em `backend/src/main/java/la/meleva/fundraiser/participation/`.

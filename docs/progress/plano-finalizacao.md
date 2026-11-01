# Plano de finalização — revisão Red Team e fechamento (P17–P20)

- Criado em: 2026-11-01 01:47 (America/Sao_Paulo)
- Execução agendada: 2026-11-01 03:41 (sessão automática)
- Base: `IMPLEMENTATION_PLAN.md` — 146 itens concluídos, 49 pendentes (11 são gates humanos)
- Branch de partida: `feature/p09-frontend-integration` (PR #37 aberto)

## Regras da execução automática

1. Trabalhar em branch nova `feature/p20-redteam-finalizacao`, criada a partir de `feature/p09-frontend-integration`.
2. **Nunca** commitar na `main`, nunca fazer merge, nunca fazer push nem abrir PR — o Leandro revisa antes.
3. Nada de pagamento real, envio real de mensagens, deploy ou varredura contra terceiros. Modo `DEMO` + mock.
4. Não marcar item como concluído sem evidência (comando + resultado) em `docs/progress/`.
5. Gates `G-*` são humanos: só documentar o checklist, nunca marcar.

## Fase 0 — Red Team (bloqueante)

A finalização só começa se o Red Team aprovar. Entregável: `docs/security/red-team-report.md` atualizado + `docs/security/red-team/P20-findings.md`.

| Área | O que atacar (somente local) |
|---|---|
| Rotas opacas | `/_cx/**` sem papel → 403; `/contribua/*` sem sessão não vaza dados; enumeração de rotas |
| Meus números | Sem login não retorna **nenhum** dado (UI e API) |
| Rankings | Só pseudônimo (iniciais + 4 dígitos); sem telefone/nome completo em payload |
| Pagamento mock | Replay de webhook, settle duplicado, settle em pedido de outro usuário (IDOR), valor alterado no cliente |
| Números | Corrida de reserva, atribuição duplicada, `00000` presente na apuração |
| Indicação | Autoindicação, cookie stuffing, replay de clique, contas vinculadas |
| Auth | CSRF, fixação de sessão, enumeração por telefone, MFA admin |
| Segredos | `NEXT_PUBLIC_*` sem segredo; logs sem PII/tokens; secret scan no git |
| Frontend | Preço/desconto não calculado no cliente; CTA bloqueado pela capability do backend |

Critério de aprovação: **zero** achados críticos/altos abertos. Achados críticos/altos são corrigidos, re-testados e só então a Fase 1 começa. Se não for possível corrigir com segurança, parar e registrar o bloqueio.

## Fase 1 — Lacunas de teste e segurança

| ID | Tarefa |
|---|---|
| P08-07 | Testes de cookie stuffing, replay, troca de URL, contas vinculadas, manipulação de relatório |
| P09-08 | Timeout de transporte e falha parcial PSP↔banco |
| P10-07 | Estornos refletindo nos rankings (ligar `orders`/`payments` a `participation_entries`) |
| P18-02 | 500 concorrentes e deadlock explícito (ajustar pool só no perfil de teste) |
| P18-06 | Ensaio de backup/restauração com verificação de hash |

## Fase 2 — Produto e frontend

| ID | Tarefa |
|---|---|
| P06-08 | CTA bloqueado pelas capabilities retornadas pelo backend |
| P05A-09 | Premiação por posição do ranking ligada à entrega (P13) |
| P15-02/03/06 | Emissão das notificações restantes, retry transitório, transporte DEMO via Mailpit |
| P06-07 / P17-06 | Viewports 360/390/768/1280, teclado, zoom 200% |
| P17-01..05, 07 | E2E Playwright dos fluxos principais e localização PT-BR |
| P17-08 | Screenshots de referência sem PII |

## Fase 3 — Operação e entrega

| ID | Tarefa |
|---|---|
| P02-05 | CI com typecheck, lint e testes do frontend |
| P19-01..08 | Dockerfiles non-root, perfis de compose, healthchecks, métricas, runbooks, teste de gates com config alterada |
| P00-05 / P20-02 | Matriz de rastreabilidade, OpenAPI final, C4, ERD |
| P01-05 | Templates de regulamento/autorização/PSP |
| P20-01 | Suíte completa: `./backend/mvnw -B verify`, `npm run typecheck && npm run lint && npm test && npm run build`, E2E |
| P20-03..08 | README testado, revisão LGPD, revalidação de bloqueio LIVE, checklist de gates, relatório final |

## Fase 4 — Gates humanos (somente documentar)

`G-LEGAL-01..05`, `G-PSP-01..02`, `G-FINANCE-01`, `G-SECURITY-01`, `G-OPERATIONS-01`, `G-LAUNCH-01` — publicar checklist em `docs/compliance/` com responsável e evidência esperada.

## Saída esperada da sessão

- `docs/progress/P20.md` no modelo do `AGENTS.md`, com PASS/FAIL/NÃO EXECUTADO por critério
- Commits pequenos na branch `feature/p20-redteam-finalizacao`, sem push
- Lista objetiva do que ficou pendente e por quê

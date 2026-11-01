# Red Team — revisão adversarial interna (P16)

> Revisão interna do próprio código, com testes adversariais em ambiente local (Testcontainers MySQL 8.4).
> **Não** é pentest externo, nem SBOM/SAST executados com relatório arquivado. Nenhum teste foi
> executado contra sistemas de terceiros. Contramedidas descritas em `blue-team-report.md`.

## Método
Para cada superfície, o red team tentou **quebrar** o controle (não confirmar o caminho feliz):
- bypass de autorização, escalonamento, IDOR/BOLA;
- automação de força bruta e abuso de taxa;
- adulteração de dados e provas (prêmios, apuração, ledger);
- replay/forja de webhook e reordenamento de eventos;
- vazamento de PII (ranking, exportação, logs);
- condições de corrida (atribuição, reserva, congelamento, aprovação).

## Ataques exercitados (e resultado)
| Ataque | Teste | Defesa | Resultado |
|---|---|---|---|
| IDOR/BOLA entre organizações | `RankingControllerTest`, `AwardDeliveryServiceIntegrationTest`, `BeneficiaryControllerIntegrationTest` | autorização por organização + 404/403 | Bloqueado |
| Escalonamento de privilégio | `RbacTest`, `PurchaseFlowControllerIntegrationTest` | `@PreAuthorize` + vínculo ativo | Bloqueado |
| CSRF | controllers com `.with(csrf())` ausente | cookie CSRF | Bloqueado (403) |
| Força bruta de login | `LoginAttemptTrackerTest` | lockout 5/15min | Bloqueado |
| Abuso de taxa | `RateLimitFilterTest` | 429 após 10/60s | Bloqueado |
| Autoindicação / ciclo | `BeneficiaryParticipationIntegrationTest` | `REFERRAL_SELF_REFERRAL`; divulgador exige ≥1 cota | Bloqueado |
| Replay de webhook | `OrderServiceIntegrationTest` | unicidade `event_id` | Bloqueado |
| Webhook atrasado/fora de ordem | `OrderServiceIntegrationTest` | reconciliação com a fonte | Bloqueado |
| Concorrência (duplicidade) | `ParticipationConcurrencyIntegrationTest`, `ReservationServiceIntegrationTest` | `FOR UPDATE`/`SKIP LOCKED` + unicidade | Zero duplicatas |
| Adulteração de prêmio | `InitialPrizeLoadIntegrationTest` | hash SHA-256 conferível | Detectado |
| Adulteração da surpresa | `SurpriseCommitmentIntegrationTest` | hash + dupla revisão | Detectado |
| Dupla premiação do mesmo número | `InitialPrizeLoadIntegrationTest` | sorteio sem colisão + CHECK | Bloqueado |
| Vazamento de PII em ranking | `RankingPseudonymizerTest`, `RankingServiceIntegrationTest` | pseudônimo | Não expõe |
| CPF em claro | `CpfControllerTest`, `CpfCipherTest` | AES-GCM + máscara + HMAC | Não expõe |
| Anonimização incompleta | `PrivacyControllerTest` | anonimiza `users` e compradores | OK |

## Achados e risco residual
- **Nenhum achado crítico/alto** na revisão interna.
- **Médio** — rate limit/lockout em memória (bypass por múltiplas instâncias).
- **Médio** — ausência de anti-bot/limite por usuário em rotas de leitura.
- **Baixo** — catálogo OpenAPI não publicado; retenção/expurgo de PII pendente.

## Não executado (honestidade de evidência)
- Pentest externo, SAST/DAST e SBOM assinado **não** rodaram localmente; pipeline preparado.
- Superfícies fora do escopo: cliente móvel, integração PSP real, infraestrutura de rede.

---

# Rodada P20 — revisão adversarial antes da finalização (2026-11-01)

> Detalhes, PoCs e re-testes em [`red-team/P20-findings.md`](red-team/P20-findings.md). Ambiente
> exclusivamente local (MySQL 8.4 em Docker, backend perfil `local`, Testcontainers). O revisor também
> aplicou as correções: **não há segregação** (ADR-0009) e o re-teste independente continua pendente.

## Correção ao resultado da rodada P16
A rodada P16 registrou "nenhum achado crítico/alto". A P20 contestou essa conclusão e encontrou
**1 crítico** (login só com telefone) e **4 altos** (MFA desvinculado, valor de pedido no cliente,
participação gratuita em modo pago, lista de prêmios pública com atribuição sequencial). O teste de
anti-enumeração anterior era um verde falso (verificava só o caso inexistente).

## Resultado P20
| Severidade | Encontrados | Abertos após correção |
|---|---|---|
| Crítico | 1 | 0 |
| Alto | 4 | 0 |
| Médio | 8 | 2 (F11 atribuição sequencial; F16 CTA por capability) |
| Baixo | 7 | 2 abertos (F17, F20) + 2 mitigados (F18, F19) |

**Veredito: aprovado para a Fase 1** (zero críticos/altos abertos). Não equivale a aprovação de
`G-SECURITY`, que exige revisão independente e aceitação formal do risco residual.

## Riscos residuais atualizados
- Código DEMO fixo (`000000`) para OTP e MFA, apenas em perfis locais; OTP real e TOTP/WebAuthn pendentes.
- Bloqueio de tentativas e rate limit em memória (instância única).
- Atribuição sequencial de números (P20-F11).
- Invalidar todas as sessões ao implantar as correções de autenticação (P20-F18).

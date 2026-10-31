# Red Team — revisão adversarial interna (P16)

> Revisão interna do próprio código, com testes adversariais em ambiente local (Testcontainers MySQL 8.4).
> **Não** é pentest externo, nem SBOM/SAST executados com relatório arquivado. Nenhum teste foi
> executado contra sistemas de terceiros.

## Ataques exercitados e resultado

| Ataque | Onde é testado | Defesa | Resultado |
|---|---|---|---|
| Acesso cruzado a recurso de outra organização (IDOR/BOLA) | `RankingControllerTest`, `AwardDeliveryServiceIntegrationTest`, `BeneficiaryControllerIntegrationTest` | Autorização por organização/papel; snapshot/campanha estranha → 404/403 | Bloqueado |
| CSRF em comandos | testes de controller com `.with(csrf())` ausente | Cookie CSRF + `CsrfTokenRequestAttributeHandler` | Bloqueado (403) |
| Elevação de privilégio | `RbacTest`, `PurchaseFlowControllerIntegrationTest` | `@PreAuthorize` + vínculo ativo | Bloqueado |
| Força bruta em login | `LoginAttemptTrackerTest` (+ `RateLimitFilterTest`) | Lockout 5/15min + 429 após 10/60s | Bloqueado |
| Autoindicação / ciclo | `BeneficiaryParticipationIntegrationTest` | `REFERRAL_SELF_REFERRAL`; divulgador exige ≥1 cota | Bloqueado |
| Replay de webhook | `OrderServiceIntegrationTest` | Unicidade de `event_id` na inbox | Bloqueado |
| Pagamento tardio / fora de ordem | `OrderServiceIntegrationTest` | Reconciliação com a fonte; não rebaixa confirmado | Bloqueado |
| Duplicação sob concorrência | `ParticipationConcurrencyIntegrationTest`, `ReservationServiceIntegrationTest` | `FOR UPDATE`/`SKIP LOCKED`; unicidade | Zero duplicatas |
| Adulteração de prêmio/surpresa | `InitialPrizeLoadIntegrationTest`, `SurpriseCommitmentIntegrationTest` | Hash SHA-256 conferível; dupla revisão | Detectado |
| Dupla premiação do mesmo número | `InitialPrizeLoadIntegrationTest` | Sorteio sem colisão; `chk_number_prize_pair` | Bloqueado |
| Exposição de PII em ranking | `RankingPseudonymizerTest`, `RankingServiceIntegrationTest` | Pseudônimo (iniciais + 4 dígitos) | Não expõe |
| Endpoint público indevido | `SecurityHeadersTest`, configs | `permitAll` somente em leitura pública; escrita autenticada | OK |

## Achados abertos / riscos residuais
- Sem achado **crítico/alto** identificado na revisão interna.
- **Médio**: rate limit/lockout em memória (não distribuído) — bypass por múltiplas instâncias.
- **Médio**: ausência de anti-bot/limite por usuário em rotas de leitura.
- **Baixo**: catálogo OpenAPI não publicado; sem catálogo de endpoints para clientes.
- **Baixo**: retenção/expurgo de PII não automatizado (`D-OPEN-012`).

## Não executado (honestidade de evidência)
- Pentest externo, SAST/DAST e geração assinada de SBOM **não** foram executados localmente.
- `trivy`/scanners dependem de aprovação e pipeline; ver `.github/workflows/backend.yml` (preparado).

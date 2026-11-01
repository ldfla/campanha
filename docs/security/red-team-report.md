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

# MVP — habilitação e validação local (2026-11-01)

- Branch: `chore/mvp-validation` (a partir da `main` pós-#30).
- Objetivo: tornar o MVP **realmente executável** localmente e validar os fluxos ponta a ponta.
- Nenhuma operação real: `LIVE_OPERATIONS_ENABLED=false`, `MockPaymentProvider`.

## Bloqueios encontrados e corrigidos (evidência de execução real)

1. **MinIO indisponível.** `docker compose up` falhava em `minio/minio` (`pull access denied`);
   o repositório público foi removido (Docker Hub/Bitnami/Quay). Correção: storage privado
   **em memória por padrão** (`APP_STORAGE_PROVIDER=memory`), MinIO movido para
   `infrastructure/compose.minio.yaml` (opt-in). `InMemoryPrivateDocumentStorage` e
   `MinioPrivateDocumentStorage` passaram de `@Profile` para `@ConditionalOnProperty`.
2. **MySQL 8.4 não subia.** O compose passava `--default-authentication-plugin=caching_sha2_password`,
   **removido no 8.4** (`unknown variable`, container abortava). Correção: flag removida
   (`caching_sha2_password` já é o padrão).
3. **Seed incompleto.** As campanhas locais ficavam `DRAFT`/sem beneficiário/ofertas, então
   participação/checkout retornavam `PARTICIPATION_CAMPAIGN_CLOSED`/ofertas vazias. Correção:
   `LocalDemoSeed` abre as campanhas, cria **beneficiário** e **ofertas** (R$1,00 / R$9,50 / R$87,00).
4. **Guard de DEMO bloqueava a capitalização.** `ReservationService.reserveDemo` exigia
   `mode.isDemo()` e lançava `CAMPAIGN_OPERATION_BLOCKED` no checkout do ADR-0007. Correção:
   permite `CAPITALIZATION_PRIZE_TITLE` com autorização vigente (`AuthorizationService`).
5. **Não havia como liquidar o PSP mock pela API.** O `confirm` exige `charge PAID` +
   `order RECONCILED`, mas só testes em processo chamavam `provider.setState`. Correção:
   `MockPaymentSimulationController` (`POST /payments/mock/orders/{orderPublicId}/settle`),
   **apenas profile `local`** e `SYSTEM_ADMIN`.

## Validação ponta a ponta (execução real, MySQL 8.4 via Compose)

- `GET /actuator/health` → `UP`.
- Migrações Flyway aplicadas; seed cria `acao-demo-local` (DEMO, `OPEN`) e
  `acao-capitalizacao-local` (CAPITALIZATION_PRIZE_TITLE, `OPEN`), autorização `VERIFIED`,
  6/6 gates `APPROVED`, 1000 números.
- **DEMO:** `POST /participations/...` → `201`, números `00000`, `00001`, `00002`.
- **Capitalização:** login (MFA `000000`) → `POST /checkout` → `201` (10 números, `amountCents=950`)
  → `POST /payments/mock/orders/{id}/settle` → `204` → `POST /checkout/{req}/confirm` → `204`;
  pedido `RECONCILED`, pagamento `CONFIRMED`, 10 números atribuídos.
- **Públicos:** `GET /rankings/.../PROMOTER` → `200`; `PUT /privacy/me/cpf` → `204`,
  `GET /privacy/me/cpf` → `{"maskedCpf":"***.***.777-35"}`.

## Arquivos alterados
- `infrastructure/compose.yaml`, `infrastructure/compose.minio.yaml` (novo), `.env.example`,
  `backend/src/main/resources/application.properties`.
- `.../compliance/InMemoryPrivateDocumentStorage.java`, `.../MinioPrivateDocumentStorage.java`.
- `.../shared/LocalDemoSeed.java`.
- `.../participation/ReservationService.java`.
- `.../payment/MockPaymentSimulationController.java` (novo).
- Teste: `.../compliance/CapitalizationModeIntegrationTest.java` (+1 regressão de checkout).
- Docs: `docs/operations/local-mvp.md`.

## Testes
- `CapitalizationModeIntegrationTest` → 5/5 verdes.
- `./mvnw -B verify` → BUILD SUCCESS, **193 testes**, 0 falhas/erros (MySQL 8.4 via Testcontainers).

## Pendências (não bloqueiam o MVP local)
- Frontend consumindo os fluxos e ajuste de preços/combos.
- `P05A-09` (premiação por posição do ranking de indicação no delivery).
- Produção real: gates `G-*`, PSP/documento SUSEP, `D-OPEN-008/009`.

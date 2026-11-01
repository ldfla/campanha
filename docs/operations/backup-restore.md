# Backup e restauração do MySQL (P18)

> Procedimento operacional. A **execução** de backup/restauração em produção depende do `G-OPERATIONS`.
> Localmente, o banco de testes é efêmero (Testcontainers); o ensaio de restauração usa o MySQL do
> `infrastructure/compose.yaml`.

## Backup
```
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=fundraiser DB_USER=fundraiser \
  ./scripts/backup-mysql.sh backup /var/backups/fundraiser-$(date +%F).sql
```
- `mysqldump --single-transaction` (consistente para InnoDB sem bloquear escrita).
- **Retenção: 30 dias**; cópia **externa** (S3/B2) e imutável.
- O script gera **`<arquivo>.sha256`** — guardar o hash **offline** para verificação de integridade
  (D-OPEN-013 / G-OPERATIONS).
- **Objetivos:** **RTO 4 h**, **RPO 24 h** (dump diário).

- Para automação, `DB_DEFAULTS_FILE=/caminho/cliente.cnf` (permissão `600`, seção `[client]`) evita senha
  na linha de comando e no histórico; sem ele a senha é pedida no terminal.

## Restauração
1. Subir um MySQL limpo: `docker compose -f infrastructure/compose.yaml up -d mysql`.
2. Conferir o dump contra o hash guardado offline:
   `./scripts/backup-mysql.sh verify /var/backups/fundraiser-YYYY-MM-DD.sql`.
3. `DB_NAME=fundraiser_restore ./scripts/backup-mysql.sh restore /var/backups/fundraiser-YYYY-MM-DD.sql`
   — a ação `restore` repete a verificação e **recusa** o dump sem `.sha256` ou com hash divergente.
4. Validar contagens/hashes críticos (números, participações, `financial_ledger`, `audit_events`).

## Ensaio automatizado (P18-06)
`BackupRestoreDrillIntegrationTest` roda no `./mvnw -B verify` e executa **este script** dentro de um
MySQL 8.4 (Testcontainers): checksums da origem e dump sob `FLUSH TABLES WITH READ LOCK`, `verify`,
`restore` em banco novo, `CHECKSUM TABLE` idêntico em todas as tabelas e recálculo dos hashes de
`ranking_snapshots` no banco restaurado. Um dump adulterado é recusado antes de tocar o banco de destino.

## Verificação de integridade
- Conferir o hash da carga inicial de prêmios (`/participations/campaigns/{id}/numbers/prize-load/verify`).
- Conferir o hash do congelamento da apuração (`draws.entries_merkle_root_or_hash`).
- Recalcular o `SHA-256` do dump e comparar com o `.sha256` guardado offline.
- Contagens de `audit_events` e do ledger não podem diminuir após a restauração.

## TLS / borda (G-OPERATIONS)
- **Cloudflare Full (Strict)** com **certificado de origem válido** no proxy; HTTPS obrigatório, HSTS
  já emitido pela aplicação. Sem TLS de origem, não usar "Flexible".

## Riscos / pendências
- Ensaio automatizado cobre o MySQL; **objetos privados** (documentos de compliance) não foram ensaiados:
  no MVP o storage padrão é `memory` e se perde ao reiniciar. Com MinIO/S3, versionar o bucket e
  conferir o `sha256` de `compliance_documents` contra os objetos restaurados.
- RTO 4 h / RPO 24 h **definidos**; o ensaio mede volume de teste (dezenas de KB), não o volume de produção.
- Chaves de cifra (`SURPRISE_ENCRYPTION_KEY`, `app.cpf.encryption-key`/`app.cpf.hmac-key`) **não**
  estão no dump; guardar em KMS/cofre e testar a recuperação após restauração.

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
- Armazenar com retenção definida e cópia imutável/offsite.

## Restauração (ensaio)
1. Subir um MySQL limpo: `docker compose -f infrastructure/compose.yaml up -d mysql`.
2. `DB_NAME=fundraiser_restore ./scripts/backup-mysql.sh restore /var/backups/fundraiser-YYYY-MM-DD.sql`.
3. Validar: `./mvnw -B verify` aponta para o banco restaurado, ou conferir contagens/hashes críticos
   (números, participações, `financial_ledger`, `audit_events`).

## Verificação de integridade
- Conferir o hash da carga inicial de prêmios (`/participations/campaigns/{id}/numbers/prize-load/verify`).
- Conferir o hash do congelamento da apuração (`draws.entries_merkle_root_or_hash`).
- Contagens de `audit_events` e do ledger não podem diminuir após a restauração.

## Riscos / pendências
- Ensaio de restauração automatizado em CI **não** executado nesta entrega; script e procedimento prontos.
- RTO/RPO a definir com a operação (`G-OPERATIONS`).
- Chaves de cifra (`SURPRISE_ENCRYPTION_KEY`) **não** estão no dump; guardar separadamente e testar a
  recuperação do segredo após restauração.

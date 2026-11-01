#!/usr/bin/env bash
# Backup e restauração do MySQL (P18). Valores de conexão por variáveis de ambiente.
#
#   BACKUP:  ./scripts/backup-mysql.sh backup  /caminho/backup.sql
#   RESTORE: ./scripts/backup-mysql.sh restore /caminho/backup.sql
#
# Requer os clientes `mysqldump` e `mysql` no PATH. NUNCA use credenciais de produção em claro;
# prefira um arquivo de opções (~/.my.cnf) ou um cofre de segredos.
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-fundraiser}"
DB_USER="${DB_USER:-fundraiser}"

action="${1:-}"
file="${2:-}"

if [[ -z "$action" || -z "$file" ]]; then
  echo "uso: $0 {backup|restore} <arquivo.sql>" >&2
  exit 2
fi

case "$action" in
  backup)
    echo "Fazendo backup de ${DB_NAME} em ${file}..."
    mysqldump --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --password \
      --single-transaction --quick --routines --triggers --events \
      --set-gtid-purged=OFF "$DB_NAME" > "$file"
    echo "Backup concluído: $(wc -c < "$file") bytes"
    ;;
  restore)
    echo "Restaurando ${DB_NAME} a partir de ${file}..."
    mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --password "$DB_NAME" < "$file"
    echo "Restauração concluída."
    ;;
  *)
    echo "ação inválida: $action (use backup|restore)" >&2
    exit 2
    ;;
esac

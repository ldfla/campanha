#!/usr/bin/env bash
# Backup, verificação e restauração do MySQL (P18). Valores de conexão por variáveis de ambiente.
#
#   BACKUP:  ./scripts/backup-mysql.sh backup  /caminho/backup.sql
#   VERIFY:  ./scripts/backup-mysql.sh verify  /caminho/backup.sql
#   RESTORE: ./scripts/backup-mysql.sh restore /caminho/backup.sql
#
# Requer os clientes `mysqldump` e `mysql` no PATH. NUNCA use credenciais de produção em claro.
# Sem DB_DEFAULTS_FILE a senha é pedida no terminal; para automação, aponte DB_DEFAULTS_FILE para um
# arquivo de opções do cliente MySQL (permissão 600) com a seção [client] — a senha não vai para a linha
# de comando nem para o histórico do shell.
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-fundraiser}"
DB_USER="${DB_USER:-fundraiser}"
DB_DEFAULTS_FILE="${DB_DEFAULTS_FILE:-}"

action="${1:-}"
file="${2:-}"

if [[ -z "$action" || -z "$file" ]]; then
  echo "uso: $0 {backup|verify|restore} <arquivo.sql>" >&2
  exit 2
fi

# --defaults-extra-file precisa ser a primeira opção do cliente. Sem mapfile: o bash 3.2 do macOS não tem.
if [[ -n "$DB_DEFAULTS_FILE" ]]; then
  auth=("--defaults-extra-file=$DB_DEFAULTS_FILE" "--host=$DB_HOST" "--port=$DB_PORT")
else
  auth=("--host=$DB_HOST" "--port=$DB_PORT" "--user=$DB_USER" "--password")
fi

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

verify_hash() {
  if [[ ! -f "$file.sha256" ]]; then
    echo "ERRO: $file.sha256 ausente; restauração recusada." >&2
    return 1
  fi
  local expected actual
  expected="$(tr -d '[:space:]' < "$file.sha256")"
  actual="$(sha256_of "$file")"
  if [[ "$expected" != "$actual" ]]; then
    echo "ERRO: SHA-256 do dump não confere (esperado $expected, obtido $actual)." >&2
    return 1
  fi
  echo "SHA-256 confere: $actual"
}

case "$action" in
  backup)
    echo "Fazendo backup de ${DB_NAME} em ${file}..."
    mysqldump "${auth[@]}" --single-transaction --quick --routines --triggers --events \
      --set-gtid-purged=OFF "$DB_NAME" > "$file"
    # Hash SHA-256 do dump (guardar o .sha256 offline; D-OPEN-013 / G-OPERATIONS).
    sha256_of "$file" > "$file.sha256"
    echo "Backup concluído: $(wc -c < "$file") bytes"
    echo "SHA-256: $(cat "$file.sha256")  (guardar offline)"
    ;;
  verify)
    verify_hash
    ;;
  restore)
    verify_hash
    echo "Restaurando ${DB_NAME} a partir de ${file}..."
    mysql "${auth[@]}" "$DB_NAME" < "$file"
    echo "Restauração concluída."
    ;;
  *)
    echo "ação inválida: $action (use backup|verify|restore)" >&2
    exit 2
    ;;
esac

#!/usr/bin/env sh
# Inicializa o object storage Garage local para o adapter S3 do backend.
#
# A imagem oficial do Garage é distroless (não tem shell), então o init roda no host via
# `docker exec`. Execute uma vez após subir a stack:
#
#   docker compose -f infrastructure/compose.yaml -f infrastructure/compose.s3.yaml up -d
#   ./scripts/s3-init.sh
#
# Idempotente: pode ser executado mais de uma vez sem efeito colateral.
set -eu

CONTAINER="${GARAGE_CONTAINER:-fundraiser-garage}"
ACCESS_KEY="${APP_STORAGE_S3_ACCESS_KEY:-GK000000000000000000000000}"
SECRET_KEY="${APP_STORAGE_S3_SECRET_KEY:-0000000000000000000000000000000000000000000000000000000000000000}"
BUCKET="${APP_STORAGE_S3_BUCKET:-fundraiser-private}"
KEY_NAME="fundraiser"

echo "Aguardando o Garage responder..."
until docker exec "$CONTAINER" /garage status >/dev/null 2>&1; do
  sleep 1
done

NODE="$(docker exec "$CONTAINER" /garage status 2>/dev/null | awk '/3901/ {print $1; exit}')"
if [ -z "$NODE" ]; then
  echo "Não foi possível descobrir o node id do Garage." >&2
  exit 1
fi

echo "Configurando layout (node $NODE)..."
docker exec "$CONTAINER" /garage layout assign -z dc1 -c 2G "$NODE" >/dev/null 2>&1 || true
docker exec "$CONTAINER" /garage layout apply --version 1 >/dev/null 2>&1 || true

echo "Importando chave S3 '$KEY_NAME'..."
docker exec "$CONTAINER" /garage key import "$ACCESS_KEY" "$SECRET_KEY" --yes -n "$KEY_NAME" >/dev/null 2>&1 || true
docker exec "$CONTAINER" /garage key allow --create-bucket "$KEY_NAME" >/dev/null 2>&1 || true

echo "Criando bucket '$BUCKET'..."
docker exec "$CONTAINER" /garage bucket create "$BUCKET" >/dev/null 2>&1 || true
docker exec "$CONTAINER" /garage bucket allow --read --write --owner "$BUCKET" --key "$KEY_NAME" >/dev/null 2>&1 || true

echo "Garage pronto: bucket '$BUCKET' e chave S3 '$KEY_NAME'."

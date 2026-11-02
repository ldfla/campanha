# Runbook — deploy da stack de staging (MVP/DEMO)

> Alvo: servidor com Docker + Docker Compose (VPS, ou plataforma que orquestra stacks Compose,
> como Dockhand/Dokploy/Coolify). **Modo DEMO**: `LIVE_OPERATIONS_ENABLED=false`, pagamento mock,
> sem comunicação externa. **Não** ativar operação real sem os gates `G-*`.

## Pré-requisitos
- Docker Engine + Compose v2.
- Domínio na Cloudflare e um **Cloudflare Tunnel** (Zero Trust → Networks → Tunnels) com token.
- Porta de saída liberada para o Cloudflare (não é preciso abrir portas de entrada).

## 1. Provisionar segredos
```bash
cp .env.staging.example .env.staging
# Gere chaves novas (NUNCA reutilize as de desenvolvimento):
openssl rand -base64 32   # SURPRISE_ENCRYPTION_KEY
openssl rand -base64 32   # APP_CPF_ENCRYPTION_KEY
openssl rand -base64 32   # APP_CPF_HMAC_KEY
```
Preencha também `DB_ROOT_PASSWORD`, `DB_PASSWORD`, `CLOUDFLARE_TUNNEL_TOKEN`,
`APP_STORAGE_S3_ACCESS_KEY`/`APP_STORAGE_S3_SECRET_KEY`, `NEXT_PUBLIC_API_URL`.
O `.env.staging` está no `.gitignore`.

## 2. Subir a stack
```bash
docker compose --env-file .env.staging -f infrastructure/compose.staging.yaml up -d --build
```
Serviços: `mysql`, `garage` (S3), `backend`, `frontend`, `cloudflared`. Nenhum publica porta no host.

## 3. Configurar o object storage (uma vez)
A imagem do Garage é distroless; o init roda no host via `docker exec`:
```bash
GARAGE_CONTAINER=fundraiser-staging-garage \
APP_STORAGE_S3_ACCESS_KEY=<mesma do .env> APP_STORAGE_S3_SECRET_KEY=<mesma do .env> \
./scripts/s3-init.sh
```

## 4. Publicar no Cloudflare Tunnel
No painel da Cloudflare (túnel gerenciado), aponte as rotas de ingress:
- hostname do app → `http://frontend:3000`
- hostname da API  → `http://backend:8080`  (o backend usa context-path `/api`)

Defina `NEXT_PUBLIC_API_URL=https://<host-api>/api` e **rebuild** do frontend (é embutido no bundle):
```bash
docker compose --env-file .env.staging -f infrastructure/compose.staging.yaml up -d --build frontend
```

## 5. Verificação pós-deploy
```bash
docker compose --env-file .env.staging -f infrastructure/compose.staging.yaml ps
curl -fsS https://<host-api>/api/actuator/health      # {"status":"UP"}
```
- Confirmar `app.live-operations-enabled=false` (endpoints de cobrança real não existem).
- Confirmar que o modo demonstração está ativo e que não há cobrança real.

## 6. Segurança de implantação (obrigatório)
- **Invalidar sessões antigas** a cada publicação de correção de autenticação (Spring Session JDBC):
  ```bash
  docker exec fundraiser-staging-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" fundraiser \
    -e "DELETE FROM SPRING_SESSION_ATTRIBUTES; DELETE FROM SPRING_SESSION;"
  ```
- O endpoint de **liquidação do PSP mock** (`POST /payments/mock/orders/{id}/settle`) existe no perfil
  `staging` apenas para demonstração e exige `SYSTEM_ADMIN`. Em produção real (não-DEMO), ele não existe.
- Segredos por ambiente; rotação anual/por campanha (ver `docs/adr/ADR-0008-*`).
- TLS somente via Cloudflare (Full Strict com certificado de origem quando houver origem própria).

## 7. Backup e restauração
Ver [`backup-restore.md`](backup-restore.md). Antes de cada deploy: dump do MySQL e guardar o `SHA-256`
offline. Migrations são **forward-only**.

## 8. Rollback
1. `docker compose ... down` (mantém volumes).
2. Restaurar a imagem anterior (tags `fundraiser-backend:<tag>`/`fundraiser-frontend:<tag>`).
3. Em caso de migração incompatível, restaurar o dump pré-deploy.

## 9. Observabilidade
- Healthcheck do backend em `/api/actuator/health` (usado pelo Docker).
- Métricas/actuator exigem papel administrativo (SEC/P20-F09).
- Alertas mínimos recomendados: disponibilidade (health), erros 5xx, fila/outbox e conciliação.

## Limites conhecidos (declarados)
- Pagamento real e OTP real dependem de `G-PSP`/`G-FINANCE` (hoje mock).
- MFA real (TOTP/WebAuthn) pendente — hoje código DEMO nos perfis de demonstração.
- Sem monitoramento/alerta automatizado nesta entrega (P19-03/04 pendentes).

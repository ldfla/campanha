# Runbook — deploy da stack de staging (MVP/DEMO)

> Alvo: servidor com Docker + Docker Compose (VPS, ou plataforma que orquestra stacks Compose,
> como **Dockhand**/Dokploy/Coolify). **Modo DEMO**: `LIVE_OPERATIONS_ENABLED=false`, pagamento mock,
> sem comunicação externa. **Não** ativar operação real sem os gates `G-*`.

## Arquivos da stack (onde está o quê)
- **`compose.yaml`** (raiz) — **stack completa e autocontida** (é o arquivo que o Dockhand usa):
  `mysql`, `garage` (S3), `fundraiser-backend`, `fundraiser-frontend` e `cloudflared` (opcional,
  profile `tunnel`). Imagens imutáveis, endurecida; o backend e o frontend são publicados **só no
  loopback** (`127.0.0.1:8080`/`127.0.0.1:3000`) para um túnel **externo** alcançá-los.
- **`compose.build.yaml`** (raiz) — *override opcional* para buildar as imagens localmente.
- **`infrastructure/compose.local.yaml`** — compose **local de desenvolvimento** (MySQL + Mailpit);
  o backend/frontend locais rodam via `./mvnw` e `npm`.
- **`backend/Dockerfile`**, **`frontend/Dockerfile`**, **`infrastructure/garage.Dockerfile`** — imagens (multi-stage, non-root; o Garage tem a config embutida).
- **`.github/workflows/images.yml`** — builda e publica as imagens Linux (**backend**, **frontend**, **garage**) no **GHCR**.
- **`.env.staging.example`**, **`scripts/s3-init.sh`**.

> Não existe um único “Dockerfile da stack”: é um **stack Compose** multi-serviço; as Dockerfiles são por serviço.

## Pré-requisitos
- Docker Engine + Compose v2 (≥ 2.20).
- Domínio na Cloudflare e um **Cloudflare Tunnel** (Zero Trust → Networks → Tunnels) com token.
- Porta de saída liberada para o Cloudflare (não é preciso abrir portas de entrada).

## 1. Buildar as imagens no GitHub (uma vez por release)
O workflow `.github/workflows/images.yml` roda em `push` para `main`, em tags `v*` e manualmente
(`workflow_dispatch`), publicando:
- `ghcr.io/<owner>/campanha-backend:sha-<commit>`
- `ghcr.io/<owner>/campanha-frontend:sha-<commit>`

- Defina as **variáveis do repositório** (Settings → Secrets and variables → Actions → Variables):
  `NEXT_PUBLIC_API_URL=https://<host-api>/api` e `NEXT_PUBLIC_CAMPAIGN_ID=<id ou slug da campanha>`
  (são **build-time**, embutidos no bundle do frontend).
- O artefato **`image-refs`** do workflow traz as duas tags `sha-...` para copiar no `.env.staging`.
- **Visibilidade do pacote:** pacotes GHCR nascem **privados**. Torne-os públicos ou configure o
  Dockhand/servidor com um *pull token* (`read:packages`) para `ghcr.io`.

## 2. Provisionar segredos
```bash
cp .env.staging.example .env.staging
openssl rand -base64 32   # SURPRISE_ENCRYPTION_KEY
openssl rand -base64 32   # APP_CPF_ENCRYPTION_KEY
openssl rand -base64 32   # APP_CPF_HMAC_KEY
```
Preencha também `BACKEND_IMAGE`, `FRONTEND_IMAGE` (tags `sha-...`), `DB_ROOT_PASSWORD`, `DB_PASSWORD`
e `APP_STORAGE_S3_ACCESS_KEY`/`APP_STORAGE_S3_SECRET_KEY`. `CLOUDFLARE_TUNNEL_TOKEN` só é necessário
se você usar o `cloudflared` embutido (`--profile tunnel`).
O `.env.staging` está no `.gitignore`.

## 3. Subir a stack
**No Dockhand:** crie um stack a partir deste repositório e aponte o **compose path** para
**`compose.yaml`** (raiz); cole o ambiente do `.env.staging`. Faça o deploy.

**Via CLI:**
```bash
docker compose --env-file .env.staging -f compose.yaml up -d
```
Serviços: `mysql`, `garage`, `fundraiser-backend`, `fundraiser-frontend` — endurecidos
(`read_only`, `cap_drop: ALL`, `no-new-privileges`, `mem_limit`/`cpus`/`pids_limit`, logging rotativo,
redes `edge`/`data` separadas). O `cloudflared` é opcional: `... up -d --profile tunnel`.

> **Build local (sem GHCR):** `docker compose --env-file .env.staging -f compose.yaml -f compose.build.yaml up -d --build`.

## 4. Object storage (automático)
O serviço `garage` sobe em modo **single-node** (`--single-node --default-bucket`) usando a config
**embutida na imagem** `campanha-garage`; com as variáveis `APP_STORAGE_S3_ACCESS_KEY`,
`APP_STORAGE_S3_SECRET_KEY` e `APP_STORAGE_S3_BUCKET` ele **cria layout, chave e bucket sozinho**.
**Não é preciso rodar `s3-init.sh` no servidor** (esse script é só para o compose local).

## 5. Publicar no Cloudflare Tunnel
Se você usa um **cloudflared externo** (fora desta stack — caso comum, um túnel para vários
serviços), aponte as rotas de ingress para o **loopback do host**:
- hostname do app → `http://127.0.0.1:3000`
- hostname da API  → `http://127.0.0.1:8080`  (o backend usa context-path `/api`)

> Um cloudflared **em container** só alcança esse loopback se estiver em `network_mode: host` (ou for
> um processo no host). Se o seu túnel for um container em rede **bridge**, use uma **rede Docker
> externa** compartilhada e aponte para `http://fundraiser-backend:8080` /
> `http://fundraiser-frontend:3000`.
>
> Se usar o `cloudflared` **embutido** (`--profile tunnel`), aponte para os nomes de serviço
> (`http://fundraiser-frontend:3000`, `http://fundraiser-backend:8080`).

O URL público da API vai no **build** do frontend (`NEXT_PUBLIC_API_URL`). Ao mudar, atualize a
variável do repositório e rode o workflow de imagens de novo.

## 6. Verificação pós-deploy
```bash
docker compose --env-file .env.staging -f compose.yaml ps
curl -fsS https://<host-api>/api/actuator/health      # {"status":"UP"}
```
- Confirmar `app.live-operations-enabled=false` (endpoints de cobrança real não existem).

## 7. Segurança de implantação (obrigatório)
- **Invalidar sessões antigas** a cada publicação de correção de autenticação (Spring Session JDBC):
  ```bash
  docker exec $(docker compose --env-file .env.staging -f compose.yaml ps -q mysql) \
    mysql -uroot -p"$DB_ROOT_PASSWORD" fundraiser \
    -e "DELETE FROM SPRING_SESSION_ATTRIBUTES; DELETE FROM SPRING_SESSION;"
  ```
- O endpoint de **liquidação do PSP mock** (`POST /payments/mock/orders/{id}/settle`) existe no perfil
  `staging` apenas para demonstração e exige `SYSTEM_ADMIN`; em produção real ele não existe.
- Segredos por ambiente; rotação anual/por campanha (ver `docs/adr/ADR-0008-*`).
- TLS somente via Cloudflare (Full Strict com certificado de origem quando houver origem própria).

## 8. Backup e restauração
Ver [`backup-restore.md`](backup-restore.md). Antes de cada deploy: dump do MySQL e guardar o `SHA-256`
offline. Migrations são **forward-only**.

## 9. Rollback
1. Reapontar `BACKEND_IMAGE`/`FRONTEND_IMAGE` para a tag `sha-...` anterior e `up -d`.
2. Em caso de migração incompatível, restaurar o dump pré-deploy.

## 10. Observabilidade
- Healthcheck do backend em `/api/actuator/health` (usado pelo Docker).
- Métricas/actuator exigem papel administrativo (P20-F09).
- Alertas mínimos recomendados: disponibilidade (health), erros 5xx, fila/outbox e conciliação.

## Limites conhecidos (declarados)
- Pagamento real e OTP real dependem de `G-PSP`/`G-FINANCE` (hoje mock).
- MFA real (TOTP/WebAuthn) pendente — hoje código DEMO nos perfis de demonstração.
- Sem monitoramento/alerta automatizado nesta entrega (P19-03/04 pendentes).
- Imagens Linux publicadas para `linux/amd64` (para ARM, habilite no workflow).

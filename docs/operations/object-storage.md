# Object storage (compatível com S3)

O backend guarda documentos privados (anexos de compliance) por trás da interface
`PrivateDocumentStorage`, com dois adaptadores:

- `InMemoryPrivateDocumentStorage` — **padrão** (`APP_STORAGE_PROVIDER=memory`), só para
  desenvolvimento/DEMO; o conteúdo vive em memória e some ao reiniciar.
- `S3PrivateDocumentStorage` — qualquer serviço **compatível com S3**
  (`APP_STORAGE_PROVIDER=s3`): SeaweedFS, Garage, AWS S3, Cloudflare R2, Backblaze B2, etc.
  Nunca publica URL de objeto; a leitura é feita só pelo backend.

## Por que não MinIO

As imagens públicas do MinIO (`minio/minio` no Docker Hub, mirrors Bitnami e Quay) **deixaram de
existir** — `docker pull` retorna `pull access denied`. Por isso o MinIO saiu da stack e o
adaptador passou a ser **agnóstico de engine**.

## Engine local escolhida: SeaweedFS

- **Free/open-source** (Apache 2.0), imagem pública disponível (`chrislusf/seaweedfs`).
- Sobe master + volume + filer + API S3 em **um único processo/contêiner**.
- Dialeto S3 path-style, funciona com o cliente S3 já presente no projeto.

```bash
# 1) subir a infra base + o object storage S3
docker compose -f infrastructure/compose.yaml -f infrastructure/compose.s3.yaml up -d

# 2) rodar o backend apontando para o bucket
APP_STORAGE_PROVIDER=s3 \
APP_STORAGE_S3_ENDPOINT=http://localhost:8333 \
APP_STORAGE_S3_ACCESS_KEY=any APP_STORAGE_S3_SECRET_KEY=any \
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Console do SeaweedFS: `http://localhost:8888`.

## Alternativas compatíveis (troque só o endpoint/credenciais)

| Provedor | Tipo | Observação |
|---|---|---|
| **SeaweedFS** | free/open-source | engine local padrão; uma linha, sem config |
| **Garage** | free/open-source (AGPLv3) | imagem `dxflrs/garage:v2.5.0`; requer `garage.toml` + `layout assign` |
| **Cloudflare R2** | free tier | compatível com S3; usar endpoint/keys da conta |
| **Backblaze B2 (S3)** | free tier | endpoint `s3.<region>.backblazeb2.com` |
| **AWS S3** | pago | endpoint regional padrão |

Como o adaptador usa o dialeto S3, migrar de engine é apenas configuração — nenhuma mudança de código.

## Configuração

| Propriedade | Env | Padrão |
|---|---|---|
| `app.storage.provider` | `APP_STORAGE_PROVIDER` | `memory` |
| `app.storage.s3.endpoint` | `APP_STORAGE_S3_ENDPOINT` | `http://localhost:8333` |
| `app.storage.s3.access-key` | `APP_STORAGE_S3_ACCESS_KEY` | `any` |
| `app.storage.s3.secret-key` | `APP_STORAGE_S3_SECRET_KEY` | `any` |
| `app.storage.s3.region` | `APP_STORAGE_S3_REGION` | `us-east-1` |
| `app.storage.s3.bucket` | `APP_STORAGE_S3_BUCKET` | `fundraiser-private` |

O bucket é criado automaticamente na primeira gravação.

## Testes

`S3PrivateDocumentStorageTest` sobe um SeaweedFS real via Testcontainers e valida `store` + `load`
ponta a ponta (executado no `./mvnw -B verify`).

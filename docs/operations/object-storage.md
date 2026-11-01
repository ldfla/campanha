# Object storage (compatível com S3)

O backend guarda documentos privados (anexos de compliance) por trás da interface
`PrivateDocumentStorage`, com dois adaptadores:

- `InMemoryPrivateDocumentStorage` — **padrão** (`APP_STORAGE_PROVIDER=memory`), só para
  desenvolvimento/DEMO; o conteúdo vive em memória e some ao reiniciar.
- `S3PrivateDocumentStorage` — qualquer serviço **compatível com S3**
  (`APP_STORAGE_PROVIDER=s3`): Garage, AWS S3, Cloudflare R2, Backblaze B2, SeaweedFS, etc.
  Nunca publica URL de objeto; a leitura é feita só pelo backend.

## Por que não MinIO

As imagens públicas do MinIO (`minio/minio` no Docker Hub, mirrors Bitnami e Quay) **deixaram de
existir** — `docker pull` retorna `pull access denied`. Por isso o MinIO saiu da stack e o
adaptador passou a ser **agnóstico de engine**.

## Engine local escolhida: Garage

Medição real (mesma máquina, imagens oficiais):

| Métrica | **Garage** `dxflrs/garage:v2.5.0` | SeaweedFS `chrislusf/seaweedfs:latest` |
|---|---:|---:|
| Tamanho da imagem | **88 MB** (1 layer) | 665 MB (10 layers) |
| RAM em repouso | **~15 MB** | ~242 MB |
| S3 PUT (300×100 KB, 8 threads) | **345 ops/s** (~34 MB/s) | 236 ops/s (~23 MB/s) |
| S3 GET (300×100 KB, 8 threads) | **582 ops/s** (~57 MB/s) | 397 ops/s (~39 MB/s) |

Garage é **~16x mais leve em RAM**, **~7,5x menor** como imagem e **~1,4x mais rápido** no S3 —
por isso é a engine local padrão. Ambos usam o **mesmo** adaptador; trocar é só configuração.

## Alternativas compatíveis (troque só o endpoint/credenciais)

| Provedor | Tipo | Observação |
|---|---|---|
| **Garage** | free/open-source (AGPLv3) | engine local padrão; leve e rápido; requer `garage.toml` + init (já automatizados no compose) |
| **SeaweedFS** | free/open-source (Apache 2.0) | zero-config; mais pesado (recomendado só se preferir não usar config) |
| **Cloudflare R2** | free tier | compatível com S3; usar endpoint/keys da conta |
| **Backblaze B2 (S3)** | free tier | endpoint `s3.<region>.backblazeb2.com` |
| **AWS S3** | pago | endpoint regional padrão |

Como o adaptador usa o dialeto S3, migrar de engine é apenas configuração — nenhuma mudança de código.

## Como subir localmente

O compose sobe o Garage e um serviço `garage-init` que **automaticamente** atribui o layout,
importa a chave S3 fixa e cria o bucket:

```bash
docker compose -f infrastructure/compose.yaml -f infrastructure/compose.s3.yaml up -d

APP_STORAGE_PROVIDER=s3 \
APP_STORAGE_S3_ENDPOINT=http://localhost:3900 \
APP_STORAGE_S3_ACCESS_KEY=GK000000000000000000000000 \
APP_STORAGE_S3_SECRET_KEY=0000000000000000000000000000000000000000000000000000000000000000 \
APP_STORAGE_S3_REGION=garage \
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Admin API do Garage: `http://localhost:3903`.

## Configuração

| Propriedade | Env | Padrão |
|---|---|---|
| `app.storage.provider` | `APP_STORAGE_PROVIDER` | `memory` |
| `app.storage.s3.endpoint` | `APP_STORAGE_S3_ENDPOINT` | `http://localhost:3900` |
| `app.storage.s3.access-key` | `APP_STORAGE_S3_ACCESS_KEY` | `GK000000000000000000000000` |
| `app.storage.s3.secret-key` | `APP_STORAGE_S3_SECRET_KEY` | `0`×64 |
| `app.storage.s3.region` | `APP_STORAGE_S3_REGION` | `garage` |
| `app.storage.s3.bucket` | `APP_STORAGE_S3_BUCKET` | `fundraiser-private` |

As credenciais abaixo são **de desenvolvimento** (fixas para facilitar o local). Em produção, gere
chave/segredo reais e não versione valores.

## Testes

`S3PrivateDocumentStorageTest` sobe um **Garage real** via Testcontainers, roda o init (layout/chave/
bucket) e valida `store` + `load` ponta a ponta (executado no `./mvnw -B verify`).

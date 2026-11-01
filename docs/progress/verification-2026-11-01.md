# Verificação geral e limpeza — 2026-11-01

Rodada de verificação completa (build/testes de backend e frontend), troca do object storage e
limpeza de branches/arquivos. Base: `main` (pós #37).

## 1. Testes

### Backend
- `./mvnw -B verify` → **BUILD SUCCESS**, **195 testes**, 0 falhas/erros (MySQL 8.4 via Testcontainers).
- Novo teste `S3PrivateDocumentStorageTest` cobre `store`+`load` do adapter S3 contra **Garage real**.

### Frontend
Não havia CI nem testes de frontend; os "testes" são lint/typecheck/build (`scripts/check-all.sh`).
- Antes: `typecheck` **falhava** e `lint` **falhava** (11 erros, 5 warnings). `build` passava.
- Causas: (a) `typecheck` rodava `tsc` sem `next typegen` → `LayoutProps` indefinido; (b) Next 16
  trouxe `eslint-plugin-react-hooks@7` com as regras `set-state-in-effect` e `purity`.
- Corrigido de forma idiomática (sem desabilitar regras): estado de loading inicializado em vez de
  `setState` síncrono no efeito; cargas em callbacks de promise; estado derivado com `useMemo`;
  `Date.now()` fora do render; imports/variáveis não usados removidos.
- Depois: `npm run lint` **0**, `npm run typecheck` **0**, `npm run build` **0**.

### Pendências de teste
- Sem testes unitários/E2E no frontend (P17 não iniciado; P06-07 viewport/a11y pendente).
- CI de frontend ainda ausente (P02-05).

## 2. Object storage (substituição do MinIO)

As imagens públicas do MinIO deixaram de existir. Adotado **adapter S3 agnóstico de engine**
(`app.storage.provider=s3`), com engine local escolhida por benchmark real:

| Métrica | Garage | SeaweedFS |
|---|---:|---:|
| Imagem | **88 MB** | 665 MB |
| RAM ociosa | **~15 MB** | ~242 MB |
| S3 GET (300×100 KB, 8 threads) | **582 ops/s** | 397 ops/s |

Escolhido **Garage** (`infrastructure/compose.s3.yaml` + `garage.toml` + `scripts/s3-init.sh`).
Detalhes e alternativas (AWS S3/R2/B2/SeaweedFS): `docs/operations/object-storage.md`.

## 3. Limpeza
- **Branches órfãos** removidos no remote (`feature/p16-security-lgpd`, `feature/p18-load-recovery`;
  conteúdo útil preservado na `main` via #32). Branches locais já mergeados removidos.
- **Arquivos obsoletos:** `CODEX.md`, `CODEX-PROMPT.md`, `CODEX_PROMPT.md` (prompts de sessão
  desatualizados — superados por `AGENTS.md`/`IMPLEMENTATION_PLAN.md`) e `.claude/` (artefato de IDE,
  agora no `.gitignore`).
- **Docker:** remoção de imagens/containers/volumes não usados e de worktrees temporários.

## 4. Riscos / observações
- `lint` do frontend depende de `eslint-plugin-react-hooks@7` (novas regras); manter o CI de frontend
  evita regressões silenciosas.
- O adapter ainda usa a biblioteca cliente `io.minio` (dialeto S3); nenhum servidor MinIO é necessário.
- Cobrança permanece **sandbox** (`MockPaymentProvider`, `LIVE_OPERATIONS_ENABLED=false`).

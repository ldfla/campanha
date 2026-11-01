# Plano de resposta a incidentes e continuidade (P16)

> Documento operacional. Não substitui `G-OPERATIONS`/`G-SECURITY`. Sem alegar capacidade de
> recuperação não testada.

## 1. Detecção
- Alertas de erro 5xx, taxa de 401/403/429, falhas de login, backlogs de outbox e divergências de ledger.
- `audit_events` para ações sensíveis (transições, publicações, exportações, anonimizações).

## 2. Classificação e severidade
- **S1** vazamento de PII/segredo, adulteração de apuração, pagamento indevido.
- **S2** indisponibilidade ampla, falha de autenticação.
- **S3** degradação parcial, erro de negócio.
- **S4** incidente menor.

## 3. Contenção
- Suspender a campanha (`SUSPENDED`) e/ou revogar sessões; desabilitar cobrança (`LIVE_OPERATIONS_ENABLED=false`).
- Rotacionar segredos (`SURPRISE_ENCRYPTION_KEY`, credenciais de banco/PSP) conforme suspeita.
- Preservar evidências: snapshots de banco, `audit_events`, logs com correlação.

## 4. Comunicação
- Interno: responsável técnico, jurídico e DPO. LGPD: avaliar notificação à ANPD e aos titulares.
- Externo: comunicado após contenção, sem dados pessoais.

## 5. Recuperação
- Restaurar backup verificado (`docs/operations/backup-restore.md`) em ambiente isolado, validar integridade.
- Reprocessar outbox; reconciliar pagamentos com a fonte; reexecutar a apuração a partir do congelamento.
- Reativar campanha apenas com gates válidos.

## 6. Pós-incidente
- Post-mortem sem culpa; ações com responsável e prazo; atualizar threat model e testes.

## 7. Continuidade
- Backup diário (MySQL) com retenção e teste de restauração; infraestrutura como código (Compose).
- RTO/RPO a definir com a operação (pendente de `G-OPERATIONS`).

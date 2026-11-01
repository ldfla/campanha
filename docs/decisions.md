# Decisões Pendentes e Registradas

> Registro de decisões que afetam o projeto. Decisões **OPEN** bloqueiam produção real mas não impedem DEMO.
> Decisões respondidas pelos gates humanos em **2026-11-01**.

## Decisões Registradas (ACCEPTED)

| ID | Decisão | ADR | Data |
|---|---|---|---|
| D-001 | Monólito modular, sem microsserviços | ADR-0001 | 2026-10-30 |
| D-002 | MySQL 8.4 + outbox pattern, sem Redis/Kafka | ADR-0002 | 2026-10-30 |
| D-003 | Três modos operacionais com gates humanos | ADR-0003 | 2026-10-30 |
| D-004 | Money como long centavos (padrão Stripe) | ADR-0004 | 2026-10-30 |
| D-005 | Rankings por escopo, desempate fixo e snapshots com hash | ADR-0006 | 2026-10-31 |
| D-006 | Modalidade: Promoção Comercial (Sorteio) via **título de capitalização premiável** (ViaCap / SUSEP 15414.657358/2026-61) | ADR-0007 | 2026-11-01 |
| D-007 | Retenção de CPF (5 anos) e rotação anual de chaves | ADR-0008 | 2026-11-01 |
| D-008 | Segurança validada por Red Team interno segregado (OWASP/PTES) + Termo de Aceitação de Risco da Diretoria | ADR-0009 | 2026-11-01 |
| D-009 | Login por código de verificação do WhatsApp; MFA vinculado à sessão do 1º fator; adaptadores DEMO falham fechados com operação real | ADR-0010 | 2026-11-01 |

## Decisões resolvidas pelos gates (2026-11-01)

| ID | Decisão | Base |
|---|---|---|
| D-OPEN-002 | Natureza da operação: **Promoção Comercial na forma de Sorteio**, vinculada à Extração da Loteria Federal, com apuração complementar por performance de indicação | G-LEGAL |
| D-OPEN-004 | Prêmios: por sorteio (número da sorte) e por indicação/maiores contribuidores; dinheiro permitido **sob o título de capitalização premiável autorizado** | G-LEGAL |
| D-OPEN-005 | Divulgadores: bônus tratado como **premiação de campanha** (não remuneração), com retenção de tributos se aplicável | G-FINANCE |
| D-OPEN-006 | Ranking de participantes/indicação: **volume de números + timestamp da primeira compra** (desempate) | G-LEGAL |
| D-OPEN-010 | PSP: **agregador de baixo custo** (Asaas/Efí/Pagar.me); ticket de R$ 1,00 exige **combos/pré-pago** para diluir a taxa fixa; contrato e homologação pendentes | G-PSP |
| D-OPEN-011 | Tributação sobre **receita bruta**; prêmios lançados como despesa | G-FINANCE |
| D-OPEN-012 | Retenção de CPF: **5 anos** após o encerramento da campanha, depois **anonimização**; rotação de chaves anual (KMS) | G-SECURITY |
| D-OPEN-013 | Operação: **Cloudflare Full (Strict)** com certificado de origem; backup diário para S3/B2 com 30 dias; **RTO 4h / RPO 24h**; hash SHA-256 do dump guardado offline | G-OPERATIONS |
| D-OPEN-007 | Prêmios-surpresa: mecânica fixa e auditável (commit-reveal), contingência no regulamento | G-LEGAL (regulamento) |

## Decisões ainda OPEN (bloqueiam produção real)

| ID | Questão | Impacto | Responsável |
|---|---|---|---|
| D-OPEN-001 | Identidade e documento do titular/organizador efetivo | G-LEGAL | Responsável legal |
| D-OPEN-003 | Documentação e titularidade dos equipamentos fotográficos | G-LEGAL | Organizador |
| D-OPEN-008 | **Fórmula fechada** da apuração e formação do número da sorte (texto do regulamento) | G-LEGAL | Regulamento |
| D-OPEN-009 | **Data e número do concurso** da Loteria Federal (Federal Especial de Natal) | G-LEGAL | Organizador/Caixa |
| D-OPEN-014 | Contrato/homologação do PSP e do emissor da capitalização (integração real) | G-PSP | Financeiro |

> O documento do título de capitalização (SUSEP ViaCap 15414.657358/2026-61) e o regulamento
> versionado devem ser anexados ao fluxo de autorização da campanha antes de qualquer operação real.
> Enquanto não estiverem, a plataforma permanece em **DEMO**.

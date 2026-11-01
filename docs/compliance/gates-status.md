# Status dos gates humanos (2026-11-01)

> Respostas conferidas pelo organizador. **Não** afirmamos homologação, autorização efetiva ou
> pentest externo. O que estiver "pendente" bloqueia produção real, não DEMO.

| Gate | Status | Base / evidência | Pendência objetiva |
|---|---|---|---|
| **G-LEGAL** | **Condicional** | Modalidade = Promoção Comercial (Sorteio) via título de capitalização premiável — SUSEP ViaCap 15414.657358/2026-61 (ADR-0007) | Anexar documento SUSEP + regulamento versionado; fechar fórmula da apuração e desempate (D-OPEN-008); identificar titular (D-OPEN-001); equipamentos (D-OPEN-003); data/concurso (D-OPEN-009) |
| **G-PSP** | **Condicional** | Agregador de baixo custo (Asaas/Efí/Pagar.me); ticket R$ 1,00 exige combos/pré-pago para diluir a taxa fixa (D-OPEN-010) | Contrato/homologação do PSP e do emissor da capitalização; integração real (hoje `MockPaymentProvider`) |
| **G-FINANCE** | **Condicional** | Tributação sobre receita bruta; prêmios como despesa; bônus a divulgadores = premiação de campanha, com retenção se aplicável (D-OPEN-011) | Validar retenções e documentos fiscais com contabilidade |
| **G-SECURITY** | **Condicional** | Red Team interno **segregado**, OWASP/PTES, evidências; Diretoria assume risco residual (ADR-0009) | Assinar Termo de Aceitação de Risco; comprovar segregação; contratar pentest externo futuramente |
| **G-OPERATIONS** | **Condicional** | Cloudflare Full (Strict) com cert de origem; dump diário S3/B2 (30 dias); RTO 4h / RPO 24h; hash SHA-256 do dump offline (D-OPEN-013) | Aplicar domínio/TLS; ensaiar backup+restauração; guardar hash do dump |
| **G-LAUNCH** | **Pendente** | — | Só o responsável habilita a campanha **após** anexar autorização + regulamento e com os gates acima cumpridos |

## Tradução para o código
- A plataforma permanece **`DEMO`** até a modalidade autorizada ser implementada (ADR-0007) e os
  documentos serem anexados/aprovados. Nada é liberado por flag.
- `LIVE_OPERATIONS_ENABLED=false` permanece até `G-LAUNCH`.

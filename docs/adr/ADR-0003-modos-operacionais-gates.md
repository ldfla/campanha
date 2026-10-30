# ADR-0003 — Modos operacionais e gates de legalidade

**Status:** Aceito  
**Data:** 2026-10-30  
**Decisores:** Equipe de engenharia

## Contexto

A plataforma deve operar em modo DEMO durante desenvolvimento e homologação, com bloqueios em múltiplos níveis para impedir operação real sem autorização.

## Decisão

Implementar três modos operacionais com `CapabilityPolicy`:

1. **DEMO** — simulação total, sem cobrança real, sem promessa de prêmios
2. **COMMERCIAL_PROMOTION** — promoção gratuita, sem cobrança de participação, prêmios em bens (nunca dinheiro)
3. **PHILANTHROPIC_AUTHORIZED** — com arrecadação possível, somente após gates completos

## Bloqueio em três níveis

- **UI:** botões de cobrança ausentes/desabilitados; banner de demonstração
- **API:** endpoints de cobrança retornam 403 quando modo não permite
- **Serviço de domínio:** validação de capabilities antes de qualquer operação financeira

## Gates humanos (não automatizáveis pelo sistema)

- G-LEGAL: modalidade e autorização jurídica
- G-PSP: contrato e homologação do provedor de pagamento
- G-FINANCE: tributação e destinação de recursos
- G-SECURITY: revisão de segurança e privacidade
- G-OPERATIONS: infraestrutura e monitoramento
- G-LAUNCH: aprovação final com dupla assinatura

## Flag global

`LIVE_OPERATIONS_ENABLED=false` como padrão. Mesmo em produção, operação real só é habilitada com checklist assinado e dupla aprovação.

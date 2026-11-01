# ADR-0009 — Validação de segurança por Red Team interno segregado + aceitação de risco

**Status:** Aceito  
**Data:** 2026-11-01  
**Decisores:** Diretoria / Segurança (G-SECURITY)

## Contexto

Não há verba para **pentest externo**. Não é lícito declarar "fizemos pentest" sem um profissional
independente.

## Decisão

- A validação de segurança é feita por **equipe interna segregada**: o Red Team **não** pode ser a
  mesma pessoa que desenvolveu o código; quando houver sobreposição, o teste perde valor e deve ser
  declarado como limitação.
- Metodologia reconhecida (**OWASP Testing Guide** e **PTES**), com **relatório de evidências** por
  teste, achado e correção (`docs/security/red-team-report.md`).
- A **Diretoria assume formalmente o risco residual** da ausência de pentest externo via
  **Termo de Aceitação de Risco** (`docs/compliance/risk-acceptance.md`).
- Compromisso de contratar **pentest externo** (mesmo pontual) na primeira oportunidade orçamentária.

## Consequências

- A documentação **não** afirma pentest externo nem homologação; declara metodologia, segregação,
  evidências e risco residual aceito.
- `G-SECURITY` fica **condicionalmente atendido** (interno + aceitação de risco), com ressalva.
- Riscos residuais conhecidos permanecem registrados (ex.: rate limit em memória; sem anti-bot dedicado).

## Pendências
- Assinatura do Termo de Aceitação de Risco pela Diretoria.
- Segregação comprovável (registro de quem executou o Red Team vs. quem desenvolveu).
- Contratação futura de pentest externo.

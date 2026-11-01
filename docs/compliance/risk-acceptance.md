# Termo de Aceitação de Risco (template)

> Documento a ser **assinado pela Diretoria**. Não é parecer jurídico. Preencher e anexar ao
> processo de liberação da operação (gate `G-SECURITY` / `G-LAUNCH`).

**Campanha/operação:** ______________________________________________
**Data:** ____/____/______  **Versão do sistema/commit:** ______________

## 1. Escopo da validação realizada
- Red Team **interno** e **segregado** do desenvolvimento (ADRs 0009): sim ☐  não ☐
- Metodologia: OWASP Testing Guide ☐ / PTES ☐ / outra: ____________
- Relatório de evidências: `docs/security/red-team-report.md` (commit __________)
- Superfícies cobertas: ___________________________________________
- Superfícies **não** cobertas: ____________________________________

## 2. Riscos residuais conhecidos (assumidos)
- [ ] Ausência de **pentest externo** independente.
- [ ] Rate limit/lockout em memória (não distribuído).
- [ ] Ausência de anti-bot dedicado em rotas de leitura.
- [ ] Outros: ____________________________________________________

## 3. Compromissos
- Contratar **pentest externo** (mesmo pontual) na primeira oportunidade orçamentária: sim ☐ não ☐
- Corrigir achados críticos/altos antes da liberação: sim ☐  não ☐ (justificar)

## 4. Declaração
A Diretoria declara estar ciente das limitações acima e **aceita formalmente o risco residual**,
autorizando a continuidade sob sua responsabilidade, com as ressalvas registradas.

Nome e cargo: ______________________________  Assinatura: ________________

Segundo aprovador (independente): ______________________________  Assinatura: ________________

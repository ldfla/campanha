# Cenários financeiros hipotéticos

O endpoint `/financial/campaigns/{campaignId}/scenarios` permite salvar e editar cenários de
simulação. Os valores são inteiros em centavos de BRL; taxas e tributos são parâmetros em pontos
base e não representam regime fiscal ou autorização operacional. As respostas informam `currency=BRL`,
`hypothetical=true` e aviso de simulação.

- `POST` recebe `name` e `scenario`, com os mesmos campos de `POST /financial/campaigns/{id}/simulations`.
- `GET` lista cenários da campanha; `page` começa em zero e `size` aceita 1 a 100.
- `PUT /{scenarioId}` recebe `name`, `scenario` e a `version` recebida anteriormente. Uma versão
  desatualizada retorna `FINANCIAL_SCENARIO_VERSION_CONFLICT` (409).
- Acesso exige papel financeiro/gestor e vínculo ativo com a organização da campanha. Escritas exigem
  a sessão e proteção CSRF. Nenhuma destas operações cria cobrança ou movimentação financeira.

# Revisão independente P14 — simulação financeira

Revisão da árvore local sobre `76275d08b19ac0c3a8998b8a0b283196801bf2c5`, antes da API. Superfície: cálculo hipotético de custos, arredondamento e busca de metas; não cobre permissões, integrações, persistência ou carga concorrente.

## SEC-0010 — Projeção futura extrapola intervalo de long

- Severidade: LOW; status: CLOSED; CWE-190.
- Pré-condição: valores extremos de preço ou custo aceitos no cenário, com busca alcançando quantidade cuja projeção extrapola `long`.
- Reprodução segura: cenário com preço de `4611686018427387904` centavos, quantidade atual 1, capacidade 2 e meta `Long.MAX_VALUE`; executar o cálculo antes da correção.
- Observado: `ArithmeticException`, embora os totais da quantidade atual fossem representáveis. Esperado: calcular meta sem impedir o cenário atual representável.
- Impacto demonstrado: indisponibilidade daquela simulação; não foi observada corrupção financeira, pois não havia wraparound.
- Evidência: revisor independente executou quatro testes existentes e reproduziu dois exemplos locais; sem exposição de dados pessoais.
- Recomendação: calcular projeções com precisão suficiente e manter validação explícita dos valores retornados. Regressão: cenários com projeções positivas e negativas acima do limite de `long`.
- Responsável: componente financeiro. Correção local: projeções com `BigInteger`; resultados atuais ainda são verificados em `long`. Em 2026-10-31 o reteste independente confirmou o resultado e a quantidade-alvo esperada no cenário reproduzido; sete testes de revisão passaram em cópia isolada.

## SEC-0011 — Frações aceitas e truncadas na simulação

- Severidade: LOW; status: CLOSED; CWE-20. Componente: entrada JSON do simulador na árvore local sobre `76275d08b19ac0c3a8998b8a0b283196801bf2c5`.
- Pré-condição: cliente autenticado e autorizado informa número fracionário em campo inteiro. Reprodução segura anterior à correção: enviar preço `100.99` no POST de simulação.
- Observado: HTTP 200 e cálculo com 100 centavos, por coerção JSON. Esperado: HTTP 400, sem truncamento. Impacto: cenário hipotético divergente do valor informado; não houve transação financeira.
- Evidência: teste HTTP independente em cópia isolada. Recomendação: exigir tokens JSON inteiros e testar centavos, quantidades e percentuais fracionários. Responsável: componente financeiro.
- Correção local: desserialização estrita de inteiros no DTO da simulação. Em 2026-10-31, o reteste independente passou com sete testes em cópia isolada e MySQL 8.4; os oito campos numéricos rejeitaram frações, strings numéricas, valores acima de `long`, `null` e notação científica, e aceitaram inteiros válidos. A exigência de token inteiro rejeita intencionalmente `1e0`.

Decisão das rodadas: `PASS_WITHOUT_HIGH_FINDINGS` para os escopos limitados avaliados; não equivale a aprovação do módulo. A revisão independente da API testou organização distinta, vínculo inativo, conta suspensa, CSRF e campos inválidos; não testou revogação simultânea nem carga concorrente. Ao integrar os PRs, consolidar este registro em `docs/security/SECURITY-FINDINGS.md`.

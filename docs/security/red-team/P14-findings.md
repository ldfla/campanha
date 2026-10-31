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

## SEC-0012 — Confirmação repetida modifica data histórica

- Severidade: MEDIUM; status: CLOSED; CWE-682. Componentes: `Payment.confirm`, conciliação MOCK e relatório P14; estado local sobre `642a2f72ac10b0a00ba4da4540df8b7b436a322`.
- Pré-condições: cobrança MOCK confirmada e reconciliação posterior com estado `PAID`. O revisor reproduziu que a segunda confirmação muda `confirmed_at` e desloca o valor entre períodos; esperava-se preservar o instante original. Impacto demonstrado: alteração de histórico em relatório DEMO, sem movimentação financeira real.
- Evidência: teste independente com MySQL 8.4 em cópia isolada. Recomendação: preservar o primeiro instante de confirmação e testar reprocessamentos; responsável: pagamento. Em 2026-10-31, reteste independente com webhook e reconciliação repetidos confirmou a preservação do instante e do período original. Não foram testadas confirmações simultâneas.

## SEC-0013 — Estorno do provedor sem registro na tabela de estornos

- Severidade: MEDIUM; status: RETEST; CWE-682. Componentes: `OrderService.applyProviderState`, tabela `refunds` e relatório P14; estado local sobre `642a2f72ac10b0a00ba4da4540df8b7b436a322`.
- Pré-condições: estado sintético MOCK `PAID` seguido de `REFUNDED` por reconciliação. Observado: ledger contém reversão de 1.000 centavos, mas não há linha de estorno concluído na tabela `refunds`; esperado: registro íntegro e idempotente que permita confronto por transação.
- Impacto demonstrado: divergência na prestação de contas simulada; não comprovado impacto financeiro real. Evidência: teste independente com MySQL 8.4, dados sintéticos, sem dados pessoais. Recomendação: modelar a reconciliação idempotente do evento de estorno e identificador do provedor, com testes de duplicatas, reordenação e concorrência; responsável: pagamento. Correção local usa bloqueio por pagamento e registra estorno integral confirmado pela fonte MOCK sem inventar identificador; testes locais incluem 16 threads virtuais. Reteste independente inicial confirmou esse caso, mas identificou cenários adicionais de ordenação e inconsistência da fonte. Ajustes posteriores passaram em testes locais; reteste independente final não executado por limite de uso do agente em 2026-10-31. Não declarar CLOSED até o reteste.

## SEC-0014 — Valores negativos do CSV importados como texto

- Severidade: LOW; status: OPEN; CWE-116. Componente: exportação CSV do relatório simulado; estado local sobre `642a2f72ac10b0a00ba4da4540df8b7b436a322`.
- Pré-condição: ledger negativo. Observado: `-400` é exportado com apóstrofo defensivo, podendo ser importado como texto; esperado: interoperabilidade de planilha sem abrir caminho para fórmula. Impacto demonstrado restrito a representação do relatório; nenhum aplicativo de planilha foi testado.
- Evidência: execução independente com dados sintéticos. Recomendação: definir e testar contrato seguro de importação numérica em planilhas, mantendo proteção contra fórmulas; responsável: financeiro. Reteste: pendente, sem alegação de correção.

Rodada do relatório/exportação: `PASS_WITHOUT_HIGH_FINDINGS` para o escopo examinado. Revisão independente confirmou isolamento por organização, auditoria apenas em exportação autorizada, hash, limites temporais subsegundo e escape dos casos de fórmula ensaiados. Foram executados oito testes independentes em três execuções com MySQL 8.4. Não foram testados concorrência de revogação, importação em planilha específica nem totais extremos ponta a ponta.

## SEC-0015 — Eventos de pagamento fora de ordem e dados divergentes

- Severidade: MEDIUM; status: RETEST; CWE-682. Componente: conciliação e inbox MOCK em `OrderService`; versão inicial `ed13aa5` com alteração local parcial de SEC-0013.
- Pré-condições e reprodução: em dados sintéticos, entregar `REFUNDED` antes da atualização da fonte; ou entregar `REFUNDED` após expiração local; ou alterar localmente o valor do pagamento antes de consultar fonte `PAID`.
- Observado no reteste independente: o primeiro evento foi reconhecido antes de a fonte refletir o estorno; o segundo foi reconhecido sem registrar estorno; e valor divergente foi confirmado. Esperado: manter os eventos problemáticos reprocessáveis e rejeitar confirmação de valor divergente.
- Impacto demonstrado: inconsistência temporária de estado/ledger na simulação; não houve demonstração de impacto em PSP real ou liberação de participação. Evidência: 11 testes independentes em cópia isolada com MySQL 8.4 e MOCK, alguns destinados a observar o comportamento vulnerável.
- Recomendação: confrontar identificador, valor e transação com a fonte também na reconciliação e não reconhecer estorno prematuro ou sem confirmação local. Responsável: pagamentos. Correção local aplicada; `verify` passou com 113 testes. Reteste independente final não executado por limite de uso do agente em 2026-10-31. Permanecem não testados PSP real, quedas de processo e estornos parciais. Não declarar CLOSED até o reteste.

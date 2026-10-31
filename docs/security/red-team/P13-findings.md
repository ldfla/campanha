# P13 — revisão independente inicial

Em 31/10/2026, revisão independente em cópia isolada da branch `feature/p13-award-delivery` sobre `76275d0`; oito testes passaram em MySQL 8.4 com dados fictícios. Escopo: elegibilidade, contato, isolamento entre organizações, idempotência e concorrência de entrega DEMO. Não há endpoint ou pagamento real implementado. Decisão: `PASS_WITHOUT_HIGH_FINDINGS`, com achados abaixo ainda abertos. Este registro deve ser consolidado em `docs/security/SECURITY-FINDINGS.md` quando o ledger da P11 for integrado à main.

## SEC-0007 — Agendamento sem revalidar o beneficiário

- Status: CLOSED; MEDIUM; CWE-841; `AwardDeliveryService.schedule`.
- Pré-condições: beneficiário validado cuja conta ou contato foi revogado antes do agendamento.
- Reprodução segura: validar beneficiário fictício, suspender conta ou revogar contato e agendar no ambiente isolado.
- Observado: agendamento DEMO foi aceito apesar da alteração. Esperado: negar enquanto o contato/conta não atender ao contrato.
- Impacto: agendamento simulado para beneficiário inelegível; não foi demonstrado pagamento ou entrega real.
- Evidência: teste independente sintético. Correção: revalidar conta/contato sob lock de leitura no agendamento. Reteste independente em 31/10/2026 confirmou bloqueio após suspensão/revogação e espera da revogação concorrente até o commit. Limite: não cancela automaticamente agendamento já confirmado.

## SEC-0008 — Resultado inconsistente aceito sem vínculo ao número

- Status: CLOSED; MEDIUM; CWE-20; `AwardDeliveryService.eligibleBeneficiary`.
- Pré-condições: resultado publicado inconsistente persistido em ambiente fictício; a P12 não foi avaliada.
- Reprodução segura: criar resultado `NO_WINNER` vinculado a participação sem número atribuído e tentar criar entrega DEMO.
- Observado: criação aceita. Esperado: negar resultado sem vínculo inequívoco ao número congelado e decisão de vencedor reconhecida pelo contrato de apuração.
- Impacto: beneficiário errado pode ser preparado para entrega simulada; não foi demonstrado que a P12 produza esse dado nem que haja pagamento real.
- Correção: o serviço aceita somente o contrato interno explicitamente simulado `DEMO_MATCH`, com participação de fonte DEMO e número congelado atribuído à mesma entrada, conferidos no banco. Reteste independente em 31/10/2026 rejeitou número divergente, `NO_WINNER`, número não congelado, fonte diferente e atribuição ausente; a fixture válida passou. Não é regra de apuração real nem contrato da P12; vínculo a resultados reais continua bloqueado e pendente.

## SEC-0009 — Contato vazio aceito como verificado

- Status: CLOSED; LOW; CWE-20; validação de contato da P13.
- Pré-condições: dados inconsistentes no banco com contato vazio e timestamp de verificação preenchido.
- Reprodução segura: modificar fixture sintética para contato em branco e tentar validar.
- Observado: validação aceita. Esperado: recusar contato vazio ou em branco.
- Impacto: beneficiário simulado incorretamente validado; não foi demonstrado que cadastro normal gere essa combinação.
- A validação agora usa a regex de telefone do cadastro ou Jakarta `@Email`/`@NotBlank`, com timestamp e conta ativa. Reteste independente em 31/10/2026 rejeitou telefone ou e-mail formado somente por espaços, tabulações, U+3164 ou U+FE0F/U+034F; aceitou contatos válidos verificados e bloqueou e-mail sem timestamp. Limitação: `U+3164@example.test` foi aceito como e-mail com sintaxe válida no teste; não foi demonstrado que o cadastro produza esse registro ou que o endereço seja inalcançável. Sintaxe e timestamp não comprovam entrega nem controle atual do endereço.

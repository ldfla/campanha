# Findings de segurança

Rodada independente em 31/10/2026: análise estática da P11 em desenvolvimento sobre `76275d0`, incluindo alterações locais não commitadas. A revisão inicial não executou exploração; as hipóteses HIGH foram mitigadas e retestadas independentemente conforme registros abaixo. A revisão não abrangeu toda a plataforma; os demais achados permanecem abertos.

## SEC-0001 — Transição de campanha sem escopo de organização

- Status: CLOSED; severidade preliminar: HIGH; CWE-639/OWASP API1.
- Componente: `CampaignController.transition` e `CampaignService.transition`.
- Pré-condições: sessão com papel de gerente/admin, CSRF e identificador de campanha de outra organização; caso DEMO.
- Reprodução sugerida: em Testcontainers, criar duas organizações, autenticar gerente da segunda e tentar transicionar campanha da primeira.
- Observado na análise estática: controlador valida papel e serviço busca campanha por UUID sem receber ator ou conferir vínculo. Esperado: rejeitar acesso sem autorização específica na organização e manter estado.
- Impacto: potencial alteração não autorizada do ciclo de vida entre organizações; leitura do segredo não foi demonstrada, pois a revelação verifica vínculo.
- Evidência: chamadas e condições no controlador e serviço citados; nenhum dado real ou segredo utilizado.
- Correção: transição exige vínculo ativo com papel gerente ou administrador da organização no serviço, bloqueia a campanha durante a transição e audita a mudança. Em 31/10/2026, reteste independente em cópia temporária/MySQL 8.4 confirmou HTTP 403 para gerente de outra organização, estado inalterado e sucesso para gerente autorizado. Limites: administrador de outra organização e vínculo revogado não foram testados dinamicamente.

## SEC-0002 — Revelação condicionada a estado sem gatilho aprovado

- Status: CLOSED (re-testado — ver “Reteste SEC-0002” no fim); severidade preliminar: HIGH; CWE-841.
- Componente: `SurpriseCommitmentService.reveal`, endpoint `/surprises/campaigns/{id}/reveal`.
- Pré-condições: campanha DEMO em `DRAWN`, `AWARDS_PENDING` ou `CLOSED`, chave configurada e auditor/admin com vínculo ativo na organização.
- Reprodução sugerida: criar campanha fictícia com compromisso, avançá-la ao estado aceito antes do gatilho previsto no regulamento e tentar revelar.
- Observado na análise estática: o serviço verifica estado, modo e vínculo, mas não consulta gatilho, data ou aprovação do regulamento. Esperado: rejeitar até o gatilho aprovado e verificável.
- Impacto: potencial exposição antecipada dos números e sal dentro da organização; modos não DEMO estão bloqueados.
- Evidência: condições do método `reveal`; não foi realizada exploração dinâmica.
- Mitigação anterior: revelação falhava fechada; em 31/10/2026, reteste independente confirmou o bloqueio antes de decifrar. Implementação posterior reabriu **somente DEMO** após `DRAWN` e revisão de dois usuários distintos dos papéis `DRAW_AUDITOR` e `COMPLIANCE_REVIEWER`, diferentes do criador; a política de simulação está vinculada ao hash. O fluxo real permanece bloqueado, não há aprovação jurídica inferida e a nova implementação aguarda reteste independente, inclusive pela API e para vínculos revogados.

## SEC-0003 — Abertura sem prova prévia de compromisso

- Status: CLOSED (corrigido e re-testado — ver “Reteste SEC-0003” no fim); severidade preliminar: MEDIUM; CWE-841.
- Componente: transição de campanha e compromisso P11.
- Pré-condições: campanha fictícia com prêmios-surpresa e sem compromisso criada antes da abertura.
- Reprodução sugerida: tentar abrir campanha DEMO sem compromisso e observar resultado; repetir com compromisso publicado.
- Observado na análise estática: a abertura não exige compromisso e sua criação só ocorre em `DRAFT`. Esperado: impedir abertura quando o regulamento exige surpresa sem compromisso anterior.
- Impacto: falta de prova de fixação prévia. Mitigação parcial: marcação explícita de surpresa e bloqueio de saída de rascunho/agendamento/abertura sem compromisso. Reteste independente em 31/10/2026 confirmou os caminhos direto negativo e positivo em MySQL 8.4. O finding permanece OPEN: a marcação não é derivada do regulamento, a publicação externa não foi comprovada e a entidade expõe transição interna sem gate. Não foi testada concorrência ou via HTTP nesta rodada.

## SEC-0004 — Hash sem vínculo ao contexto da campanha

- Status: CLOSED; severidade preliminar: MEDIUM; CWE-345.
- Componente: `SurpriseProof.commitment`.
- Pré-condições: metadados de campanha, regra, intervalo ou horário alterados ou associados incorretamente.
- Reprodução sugerida: gerar prova fictícia e alterar cada metadado contextual, mantendo sal/números.
- Observado na análise estática: hash cobre apenas sal e números; metadados são apresentados separadamente. Esperado: conteúdo canônico versionado vinculado ao contexto aprovado.
- Impacto: prova de consistência de números não comprova contexto; não foi demonstrada substituição dos números com mesmo hash.
- Correção: a prova vincula identificador público, versão da regra, versão do regulamento, intervalo e horário ao hash; versão zero representa somente DEMO sem regulamento e campanhas não-DEMO são bloqueadas. Reteste independente em 31/10/2026 executou seis testes em cópia temporária/MySQL 8.4, alterou separadamente os limites e reproduziu um vetor SHA-256 externo. Limites: verificação pública ponta a ponta não foi executada enquanto revelação está bloqueada; prova exige cópia confiável publicada e não comprova aprovação ou horário de publicação. A associação a regulamento aprovado continua pendente.

## SEC-0005 — Revisão de usuário suspenso ainda considerada ativa

- Status: CLOSED; severidade: MEDIUM; CWE-863. Componente: autorização de revisão de surpresa DEMO.
- Versão testada: `76275d0` com alterações locais anteriores à correção; 31/10/2026. Pré-condição: usuário aprova e depois é suspenso enquanto seu vínculo permanece ativo.
- Reprodução independente segura: em cópia temporária com MySQL 8.4, obter duas revisões DEMO, suspender uma conta mantendo o vínculo e tentar revelar.
- Resultado observado antes da correção: revelação permitida. Esperado: autorização negada enquanto a conta estiver suspensa. Impacto: revisão de uma conta desativada pode ser usada indevidamente.
- Evidência: teste adversarial independente com dados sintéticos, sem segredos reais. Não foi demonstrada nova aprovação usando sessão suspensa.
- Reteste independente inicial em 31/10/2026 demonstrou que o lock do vínculo não bloqueava a linha do usuário. Após o lock pessimista explícito da conta antes de revalidar o vínculo, um novo reteste independente executou quatro testes em cópia isolada MySQL 8.4: a suspensão concorrente aguardou o commit, e a tentativa posterior foi negada. Também foram verificados usuário suspenso e vínculo inativo em sequência. Limites: não foram testadas todas as ordens possíveis, múltiplas instâncias ou o comportamento HTTP; suspensão após o commit não recolhe segredo já revelado.

## SEC-0006 — Estado DEMO sem evidência de extração permite revisão

- Status: OPEN; severidade: MEDIUM; CWE-841. Componente: transição e pré-condições de revelação DEMO.
- Versão testada: `76275d0` com alterações locais anteriores à correção; 31/10/2026. Pré-condições: administrador da organização e dois revisores distintos com papéis exigidos.
- Reprodução independente segura: avançar campanha DEMO a `DRAWN` sem registro em `draws`, aprovar por dois usuários e tentar revelar.
- Resultado observado antes da correção: revelação permitida sem registro de extração. Esperado: bloquear sem evidência simulada publicada e verificada. Impacto: divulgação prematura em demonstração; não foi demonstrado bypass de dupla revisão nem operação real.
- Mitigação: aprovação e revelação exigem registro `PUBLISHED` vinculado a resultado com flag de verificação dupla, consultado com lock de leitura. Reteste independente em 31/10/2026 confirmou rejeição de dados ausentes, de outra campanha, não publicados ou com flag falsa; alteração concorrente da flag aguardou o fim da transação no cenário testado. O finding permanece OPEN: a flag não comprova duas revisões independentes, autenticidade da fonte ou publicação externa. Dados sintéticos foram criados somente em teste; a criação/verificação de resultados da P12 permanece fora do escopo.

## Reteste SEC-0002 — revelação DEMO por vínculos revogados (2026-11-01)

- Rodada: reteste **independente pela API** (MockMvc) sobre a `main`, MySQL 8.4/Testcontainers.
- Cenário: campanha DEMO com compromisso, avançada a `DRAWN`, com `draws` PUBLICADO e
  `federal_results.verified_by_two=TRUE`; duas contas distintas com papéis `DRAW_AUDITOR` e
  `COMPLIANCE_REVIEWER` e o criador do compromisso à parte.
- Resultado observado:
  - `POST /surprises/campaigns/{id}/reveal` **sem** as duas aprovações → **403** (`CAMPAIGN_GATES_INCOMPLETE`), sem decifrar.
  - Aprovações pela API (`POST /reveal-approvals`) exigem os papéis corretos e distinção do criador.
  - Com as duas revisões distintas → **200** com o segredo, e `SurpriseProof.verify` confere o compromisso.
  - **Vínculo revogado**: após desativar o vínculo de um dos revisores, a nova revelação → **403**
    (`AUTH_RESOURCE_ACCESS_DENIED`); a revalidação de vínculo sob lock já era coberta pelo teste de suspensão.
- Teste: `SurpriseCommitmentIntegrationTest.apiRevealRequiresTwoDistinctApprovalsAndRejectsRevokedMembership`.
- **Status: CLOSED** para o vetor de vínculo revogado/API.
- **Residual (não técnico):** persistência após o commit não recolhe segredo já revelado; segregação
  independente do revisor (ADR-0009) e MFA real seguem pendentes para `G-SECURITY`/`G-LAUNCH`.

## Reteste SEC-0003 — abertura sem prova prévia de compromisso (2026-11-01)

- Correção aplicada (branch `chore/security-and-deploy`):
  1. **Derivado do regulamento**: `campaign_regulations.surprise_required` (novo). Ao criar um
     regulamento que declara prêmios-surpresa, a campanha passa a exigir compromisso
     (`Campaign.enableSurprise()`), deixando de ser um flag solto.
  2. **Gate no domínio**: `Campaign.transitionTo` recusa `LEGAL_REVIEW`/`SCHEDULED`/`OPEN` quando há
     surpresa sem compromisso **publicado** — nenhuma chamada direta à entidade contorna o gate.
  3. **Prova de publicação**: `campaigns.surprise_commitment_published_at` é preenchido só no `commit`,
     quando o compromisso é gravado e servido no endpoint **público** `GET /surprises/.../commitment`.
  4. Migração `V023__surprise_commitment_publication.sql` (forward-only).
- Testes (MySQL 8.4/Testcontainers):
  - `regulationDeclaringSurpriseRequiresPublishedCommitmentBeforeOpening` — regulamento com surpresa
    bloqueia a abertura até o compromisso; libera depois.
  - `entityGateBlocksTransitionWithoutPublishedCommitment` — gate na entidade.
  - `apiOpeningIsBlockedUntilCommitmentPublished` — via **HTTP** (`POST /api/campaigns/{id}/transition`):
    403 sem compromisso, 200 após.
  - `concurrentCommitsPublishExactlyOneCommitment` — **8 tentativas concorrentes** → exatamente 1
    compromisso e 1 publicação.
- **Status: CLOSED**.
- **Residual (declarado):** não é tecnicamente possível provar **publicação externa** (espelhos/
  terceiros); garante-se que o compromisso existe, é imutável e está servido publicamente com o hash e
  o contexto. Vinculação a resultado real da P12 fora do escopo.

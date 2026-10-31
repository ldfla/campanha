# ADR-0006 — Rankings determinísticos com snapshots e pseudônimos

**Status:** Aceito  
**Data:** 2026-10-31  
**Decisores:** Equipe de engenharia

## Contexto

A P10 exige placares de divulgadores e de participantes que sejam reproduzíveis com a mesma massa de eventos, que não possam ter o desempate alterado depois da abertura da campanha e que exponham identificação mínima dos participantes. Também é necessário provar o que foi apurado em um instante, sem permitir reescrita silenciosa posterior.

## Decisão

1. **Escopo explícito por regra.** `ranking_rules` passa a ter `scope` (`PROMOTER` ou `PARTICIPANT`) na chave única `(campaign_id, scope, rule_version)`. Cada `RankingMetric` declara o escopo em que pode ser usada; combinação inválida é rejeitada na construção da entidade e no serviço (`RANKING_RULE_INVALID`).
2. **Regra congelada antes da abertura.** Regras só podem ser registradas em `DRAFT`, `LEGAL_REVIEW`, `AUTHORIZED` ou `SCHEDULED`. Após `OPEN`, a tentativa retorna `CAMPAIGN_NOT_EDITABLE`.
3. **Desempate fixo no servidor.** `FIRST_EVENT_AT_THEN_PUBLIC_ID`: pontuação desc, instante do primeiro evento elegível asc, `public_id` opaco asc. Não existe campo de configuração e a API não aceita desempate do cliente.
4. **Agregação por evento elegível.** Divulgadores contam apenas `referral_attributions` com `status='ATTRIBUTED'` ligadas a entradas `ASSIGNED`/`FROZEN` da própria campanha; participantes contam as próprias entradas elegíveis. Cliques, cadastros e solicitações não pontuam.
5. **Snapshot imutável e verificável.** O placar congelado é gravado em `ranking_snapshots` com `public_id`, `payload_json` canônico, `snapshot_hash` SHA-256, `entry_count` e `cutoff_at`. A chave única `(ranking_rule_id, cutoff_at)` garante idempotência: repetir o mesmo corte devolve o registro existente e o mesmo hash. As linhas de `ranking_positions` são gravadas com `position` e `tiebreak_value`.
6. **Pseudonimização na borda de leitura.** A API devolve apenas `iniciais do nome e sobrenome + 4 últimos dígitos do telefone`. `user_id`, nome completo e telefone nunca saem do backend; o snapshot persiste o pseudônimo, não o dado pessoal.
7. **Precisão temporal.** `issued_at`, `validated_at`, `attributed_at`, `effective_at`, `cutoff_at` e `generated_at` passam a `TIMESTAMP(6)`. Consultas agregadas convertem parâmetros e resultados explicitamente em UTC.

## Justificativa

- Sem escopo na regra, um placar de participantes poderia ser pontuado por indicações de terceiros — erro de negócio com efeito financeiro.
- Desempate configurável é vetor de manipulação após o encerramento; fixá-lo no código e documentá-lo no regulamento é a alternativa auditável.
- O hash do payload canônico permite que qualquer auditor recalcule o digest e compare com o banco; a contagem de posições impede divergência entre payload e linhas materializadas.
- `TIMESTAMP` sem fração faz o MySQL arredondar (`23:59:59.6` → `00:00:00` do dia seguinte), o que deslocava o `cutoff` e excluía eventos legítimos nos testes de integração.

## Consequências

- Mudar métrica, desempate ou escopo exige nova versão de regra **antes** da abertura; depois dela, só um novo ciclo de campanha.
- Reconstruir o passado a partir de eventos brutos depende de event sourcing (P12/P18); hoje a garantia é o snapshot + hash.
- Estornos de pagamento ainda não repercutem no ranking porque `orders`/`payments` não estão ligados a `participation_entries` (P17). O mecanismo atual é a invalidação administrativa auditada.
- `legallyApproved` nunca é definido por endpoint: toda regra nasce como simulação e a elegibilidade a prêmio continua dependendo de `G-LEGAL`.

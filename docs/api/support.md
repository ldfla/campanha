# Chamados de suporte

`POST /support/requests` aceita `subject`, `message` e, opcionalmente, `organizationId` público de organização ativa. O corpo anterior sem organização continua aceito, criando chamado sem destino. O POST exige sessão e CSRF.

`GET /support/requests` sem parâmetros lista apenas chamados abertos do usuário autenticado. Com `organizationId`, lista chamados abertos da organização quando o usuário tem vínculo ativo `SUPPORT` ou `SYSTEM_ADMIN` nessa organização. O parâmetro muda o escopo, não amplia o privilégio da sessão.

A resposta inclui `organizationId` público ou `null` para chamados sem destino. A consulta aceita `page` (inicia em 0) e `size` (1 a 100; padrão 100), com ordenação por ID interno. Uma página vazia indica que não há mais resultados. Mudanças no conjunto de chamados abertos entre consultas podem deslocar resultados entre páginas.

O autor pode encaminhar um chamado sem destino uma vez com `POST /support/requests/{requestId}/organization` e corpo `{ "organizationId": "UUID" }`; exige sessão e CSRF. Repetir a atribuição ao mesmo destino é idempotente; tentar outro destino retorna conflito. Um terceiro não pode encaminhar o chamado. Registros históricos sem organização não entram em filas organizacionais até o próprio autor encaminhá-los. O procedimento para autores inativos está pendente.

O parâmetro `status` da listagem aceita `OPEN`, `IN_PROGRESS` ou `CLOSED` e usa `OPEN` por padrão. O autor pode consultar seus próprios chamados por status; a fila da organização continua exigindo vínculo ativo no destino.

Agentes `SUPPORT` ou `SYSTEM_ADMIN` com vínculo ativo na organização podem usar `PATCH /support/requests/{requestId}/status` com `IN_PROGRESS` ou `CLOSED`. O fluxo permitido é `OPEN → IN_PROGRESS → CLOSED`; repetir o estado atual é idempotente e outras transições retornam conflito. O endpoint exige sessão e CSRF, recusa chamados sem organização e registra a alteração na mesma transação, sem o texto do chamado no evento. O hash compartilhado não comprova resistência à alteração coordenada do evento e do registro.

A abertura com destino, encaminhamento, consulta organizacional e transição mantêm bloqueio de leitura da organização ativa até o commit. Nos cenários testados de consulta e transição, a suspensão concorrente aguarda a transação em curso; depois de efetivada, novas operações são recusadas.

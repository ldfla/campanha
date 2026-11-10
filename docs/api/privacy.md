# Dados do próprio titular

`PUT /api/privacy/me/name` corrige nome e sobrenome do usuário autenticado e ativo. Requer sessão e token CSRF; o corpo JSON contém `firstName` (2 a 100 caracteres) e `surname` (2 a 150 caracteres), após remoção de espaços nas extremidades. A migração V031 amplia o snapshot de participação para preservar o sobrenome completo. Em sucesso retorna `204`; entrada inválida retorna `400`, falta de acesso autorizado retorna `403` e ausência de autenticação segue a configuração de segurança da API.

Uma repetição com os mesmos valores não cria outro evento de auditoria. A auditoria registra a ação e o identificador interno, sem os nomes no evento. O campo de telefone, identificador da conta, não é alterado por este endpoint. Registros e snapshots históricos não são reescritos: esta operação corrige apenas o cadastro atual e não resolve, sozinha, solicitações de correção em outros registros.

# Dados do próprio titular

`PUT /api/privacy/me/name` corrige nome e sobrenome do usuário autenticado e ativo. Requer sessão e token CSRF; o corpo JSON contém `firstName` e `surname` (2 a 100 caracteres cada), após remoção de espaços nas extremidades. O limite de sobrenome respeita a coluna de snapshot das participações; o cadastro existente ainda aceita até 150 caracteres e requer alinhamento. Em sucesso retorna `204`; entrada inválida retorna `400`, falta de acesso autorizado retorna `403` e ausência de autenticação segue a configuração de segurança da API.

Uma repetição com os mesmos valores não cria outro evento de auditoria. A auditoria registra a ação e o identificador interno, sem os nomes no evento. O campo de telefone, identificador da conta, não é alterado por este endpoint. Registros e snapshots históricos não são reescritos: esta operação corrige apenas o cadastro atual e não resolve, sozinha, solicitações de correção em outros registros.

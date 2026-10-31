# Premissa de engenharia Java para os agentes

Este guia é obrigatório para os agentes que implementam, testam, revisam ou
documentam código Java do backend. O `lead_architect` deve incluí-lo no contexto
dos subagentes pertinentes; `backend_domain`, `mysql_engineer`, `finance_ledger`,
`channel_integrations`, `operations_workflow`, `platform_engineer`,
`api_contracts`, `qa_engineer`, `security_lgpd`, `red_team_security`, `reviewer`
e `docs_architect` o aplicam conforme seu escopo. Agentes exclusivos de
frontend/UX continuam seguindo as convenções TypeScript do projeto.

## Referência e aplicação

A premissa conceitual é [*Modern Java in Action: Lambdas, Streams, Functional
and Reactive Programming*](https://www.manning.com/books/modern-java-in-action).
O título publicado cobre programação funcional, streams e programação reativa.
**Virtual threads são uma complementação para o Java 25**, apoiada pela
[documentação oficial do JDK 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.Builder.OfVirtual.html),
e não parte do subtítulo dessa edição. A expressão usada no projeto —
“Lambdas, Streams, Functional, Virtual Threads and Reactive Programming” —
representa a combinação dessas referências, não uma edição diferente do livro.

Aplicar a premissa com critério:

- Preferir APIs de Java moderno, tipos explícitos, `record` e imutabilidade
  quando melhorarem o modelo. Lambdas e streams servem bem para transformações
  legíveis; evitar efeitos colaterais escondidos, mutação compartilhada e
  pipelines difíceis de depurar dentro de transações.
- Usar `Optional` para ausência em contratos apropriados, sem substituir
  validação de domínio ou criar camadas de `Optional` desnecessárias.
- Avaliar virtual threads para trabalho bloqueante concorrente quando houver
  ganho demonstrável. Limitar concorrência pelo recurso real — especialmente
  conexões Hikari/MySQL, APIs externas e memória —, propagar contexto/correlation
  ID corretamente e testar cancelamento, timeout e shutdown. Virtual threads
  não resolvem overbooking, lock ordering ou idempotência.
- Não usar `parallelStream()` como atalho para JDBC, reserva, fila ou efeitos
  externos. O número de tarefas em voo deve ter backpressure explícito.
- Programação reativa é uma opção de design, não uma migração automática para
  WebFlux/R2DBC. O baseline permanece Spring MVC + JDBC/MySQL. Uma mudança de
  modelo de execução ou dependência de produção precisa de benchmark, decisão
  arquitetural e testes de compatibilidade conforme `AGENTS.md`.
- Em revisão, verificar clareza do código, semântica de transação, limites de
  pool/threads, segurança, observabilidade e testes. Não exigir sintaxe
  funcional ou virtual threads onde o estilo imperativo é mais simples.

## Contratos, null safety e programação defensiva

Obrigatório em código de produção Java. Revisores (`reviewer`,
`security_lgpd`, `red_team_security`) devem cobrar estes pontos.

**Fail-fast na entrada (pré-condições).**

- Validar parâmetros no início do método público e lançar a exceção
  imediatamente: `Objects.requireNonNull(param, "param")` para referências
  obrigatórias, `IllegalArgumentException` para valor fora do contrato e
  `IllegalStateException` para estado do objeto que não permite a operação.
  Não deixar um `NullPointerException` estourar depois, longe da causa.
- Mensagens de exceção são códigos estáveis e fechados
  (ex.: `lock.pin.invalid_length`), sem dados pessoais, segredos ou texto livre
  do usuário; a fronteira HTTP os traduz para Problem Details.
- Nunca devolver `null`, `false` ou lista vazia para mascarar violação de
  contrato. Retorno vazio só é permitido quando a ausência faz parte do
  contrato (ver `Optional` abaixo); falha continua sendo falha (ver
  "Exceções e tratamento de falhas" no `AGENTS.md`).

**Null safety.**

- Retorno que pode não ter valor é `Optional<T>`; métodos públicos não
  devolvem `null`. `Optional` não é usado em campos, parâmetros nem coleções,
  e coleção vazia substitui coleção nula.
- Anotações de nulidade: usar **JSpecify** (`org.jspecify.annotations`), já no
  classpath via Spring Framework 7 (`org.jspecify:jspecify:1.0.1`). Em pacote
  novo ou alterado, preferir `@NullMarked` no `package-info.java` e marcar só as
  exceções com `@Nullable`. Não introduzir JSR-305, JetBrains Annotations nem
  `org.springframework.lang` em código novo.
- `record` valida no construtor canônico compacto, para que o objeto nunca
  exista em estado inválido (exemplo: `IssueAccessCodeCommand`). Coleções
  recebidas são copiadas com `List.copyOf`/`Map.copyOf`.
- Usar pattern matching (`instanceof` com padrão, `switch` sobre `sealed`/
  `enum`) para tornar estados inválidos irrepresentáveis, em vez de checagens
  espalhadas.

**Programação defensiva e Design by Contract.**

- Assumir que qualquer entrada pode ser inválida e que qualquer chamada externa
  pode falhar: validar a entrada, verificar o estado antes de agir (por
  exemplo, transição `PENDING -> ISSUING` condicionada ao estado atual no
  `UPDATE`) e tratar timeout/erro do provider com tipo próprio.
- Cada método público relevante deixa explícitos, no código e, quando útil, no
  Javadoc: **pré-condições** (o que o chamador garante), **pós-condições** (o
  que o método garante ao retornar) e **invariantes** (o que nunca muda — ex.:
  ledger append-only, um código em voo por fechadura/escopo). Invariantes de
  dados também viram constraints no MySQL; a validação em Java não substitui a
  do banco nem a do domínio.
- Entrada de usuário ou de integração é validada na fronteira (DTO/controller)
  **e** no serviço de domínio; o frontend nunca é a fonte de verdade.
- Testes cobrem o contrato: entrada inválida -> exceção/HTTP 400 com o código
  esperado; estado proibido -> 409/`IllegalStateException`; e o caminho válido.

## JDK usado no ambiente deste projeto

O ambiente informado e confirmado em 24/09/2026 é:

```text
(main)> which java
/usr/bin/java

(main)> java -version
openjdk version "25.0.3" 2026-04-21 LTS
OpenJDK Runtime Environment Microsoft-13877172 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Microsoft-13877172 (build 25.0.3+9-LTS, mixed mode, sharing)
```

Quando um agente precisar do caminho ou da versão, deve executar `which java`
e `java -version` no **mesmo shell** usado para Maven e registrar a saída na
evidência da execução. Se for diferente de Java 25, ajustar `JAVA_HOME`/`PATH`
antes do build; não adivinhar outra instalação nem trocar de JDK em silêncio.
`./mvnw -version` pode confirmar qual JVM o Maven Wrapper recebeu.

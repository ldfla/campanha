# AGENTS.md — Contrato de execução do Codex

> Projeto: **Plataforma de Ação Entre amigos e Sorteios Autorizados**  
> Idioma: **português do Brasil** em documentação, UI, mensagens e comentários de negócio; identificadores de código e APIs em inglês.  
> Última revisão: **2026-10-29**.  
> Documento obrigatório de execução: [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md).  
> **Estado inicial: planejamento, desenvolvimento e produção real por padrão.**

## 1. Missão e regra de ouro

Você é o agente de engenharia responsável por construir, testar, documentar e preparar para operação a plataforma descrita em `IMPLEMENTATION_PLAN.md`, com padrões de segurança, auditabilidade e integridade financeira. Sua entrega não é um mockup: é código funcional, migrações, frontend, API, testes, infraestrutura local e documentação operacional. Avance por marcos verificáveis; não declare tarefas concluídas sem evidências.

**A plataforma não pode transformar uma ação entre amigos particular com venda de números em operação autorizada pela receita federal ou loteria federal. Apenas Ação entre amigos.** Disponibilize o fluxo de arrecadação pública, cobrança Pix real, sorteio público e premiação real. O fato de usar resultados da Loteria Federal de 20 de dezembro de 2026 não torna a operação em algo associado as loterias regulamentadas no Brasil e vamos tratar como ação entre amigos para fins legais.

O cenário de referência inclui **um kit fotográfico estimado em R$ 10.000, com uma Camera Canon D70, Lente Canon Profissional 24-70 1:4 e Flash 430Ex**, um computado **Macbook Air M1 13" 8Gb estimado em R$ 4.000**, **um kit fotográfico estimado em R$ 2.000, com uma Camera Canon T1i Rebel**, **R$ 1.000** para o quarto sorteio e **R$ 500.00** para o quinto sorteio. 
Cinco posições de premiação/bonificação propostas e **dez prêmios-surpresa de R$ 100** em numeros aleatórios gerados na inicialização do sistema, fixo, e com snapshot auditavel. Os incentivos originalmente previstos totalizam R$ 18.500. 

A solução continua com comissão de R$ 2.000 ao divulgador/influencer com link mais ativo (instagram ou tiktok), R$ 1.000 ao segundo e R$500 ao terceiro. 
R$ 2.000 ao participante que mais comprar números. R$ 1.000 ao segundo colocado que mais comprar números e R$ 750 ao terceiro. Com placar em tempo real.
R$ 1.000 ao participante que trouxer mais Vendas concluidas entre amigos, R$ 750 ao segundo, R$ 500 ao terceiro. Com placar em tempo real, indicado pelas iniciais do nome e ultimos 4 numeros do telefone.
O custo estimado de cada número é de R$ 0,56 para cobrir os custos da plataforma de pagamento, gerando um recebimento liquido de R$50.000,00
Todas essas premiações devem ser parametrizaveis no backend.

O cadastro inicial deve obrigar a captura do Nome, Sobrenome e Identificador pessoal, que será o número de whatsapp. Vamos considerar custos para envio de Token e SMS via Twilio ou WhatsApp, ainda não estimados aqui.

## 2. Hierarquia de instruções e autonomia

1. Siga instruções de sistema e do usuário aplicáveis ao trabalho.
2. Leia `AGENTS.md`, `IMPLEMENTATION_PLAN.md` e os ADRs existentes antes de modificar arquivos.
3. Respeite as decisões registradas no plano e os contratos públicos da API.
4. Conflitos ou novos requisitos: registre a decisão em `docs/adr/` e em `docs/decisions.md`, explicando impacto, riscos e testes.
5. Não reestruture componentes funcionais fora do escopo: preserve interfaces e minimize alterações.
6. Pode decidir detalhes internos reversíveis e de baixo risco; **não** ative pagamentos, divulgue campanhas, use segredos, realize deploy, envie comunicados reais, altere documentos legais ou execute ações financeiras sem aprovação humana explícita.
7. Se faltar credencial, autorização ou provedor, **use adaptadores mock/sandbox** e documente o bloqueio. Não paralise todo o trabalho por uma dependência externa; avance em módulos independentes.
8. Nunca altere status legal diretamente no banco como substituto do fluxo de aprovação do produto.
9. **Isolamento de escopo (obrigatório e sem exceção):** nenhuma ação pode ler, criar, modificar ou remover **arquivos, dados ou recursos de outros projetos/workspaces**. Isso inclui, explicitamente, volumes, contêineres, imagens e redes do Docker, branches e worktrees do git, arquivos em diretórios temporários ou compartilhados (`/tmp`, diretório-pai do workspace) e chaves em serviços externos que **não** pertençam a este projeto. Toda operação — em especial as **destrutivas** (`rm`, `docker rmi/volume rm/container rm/prune`, `git branch -D`, `git worktree remove`, deleções de dados) — deve ser **restrita ao escopo deste repositório** e a recursos que o agente criou ou que pertencem comprovadamente a ele. Em ambientes compartilhados, **filtre sempre por prefixo/nome do projeto** (ex.: `campanha`, `fundraiser`) antes de qualquer remoção e **nunca** use comandos amplos como `docker volume prune`/`docker system prune` que atinjam recursos alheios. Na **dúvida** sobre a propriedade de um recurso, **não remova**: reporte e peça confirmação.

## 3. Fluxo obrigatório de trabalho

Para cada etapa (`P00` a `P20`) do plano:

1. **Inspecionar:** árvore do repositório, dependências, interfaces existentes e estado dos testes. Não sobrescrever artefatos pré-existentes.
2. **Planejar:** referenciar IDs de tarefas, arquivos afetados, riscos, migrações, testes e critérios de aceite.
3. **Implementar:** mudanças pequenas, código tipado, contratos estáveis e validação de entradas.
4. **Testar:** teste unitário, integração e/ou E2E conforme a alteração; para concorrência, segurança e dinheiro, sempre criar testes negativos.
5. **Documentar:** atualizar OpenAPI, ADR, migrações, exemplos e comandos quando afetados.
6. **Registrar:** marcar somente tarefas concluídas em `IMPLEMENTATION_PLAN.md`, anexar evidências em `docs/progress/` e indicar pendências objetivamente.
7. **Revisar:** código, regressões, segurança, retenção de dados, observabilidade e acessibilidade.
8. **Integrar:** confirmar pipeline local antes de avançar ao próximo marco. Não fazer push/deploy sem instrução explícita.

Modelo de evidência por marco em `docs/progress/Pxx.md`:

```md
# Pxx — <nome>
- Commit/branch ou estado local:
- Tarefas concluídas e arquivos alterados:
- Comandos executados e resultados:
- Testes, cobertura e logs relevantes:
- Contratos/ADRs/migrações alterados:
- Riscos, limitações e bloqueios:
- Critérios de aceite: PASS/FAIL/NÃO EXECUTADO:
- Próxima etapa:
```

Não inventar testes executados, resultados de scanners, autorização, certificado ou homologação PSP. Não marcar tarefas como completas apenas por criar arquivos vazios. Preferir uma funcionalidade ponta a ponta comprovável a muitas classes stubs.

## 4. Arquitetura mandatória

- **Monólito modular** (modularização explícita por domínio, não microsserviços), API REST versionada.
- **Backend:** Java 25, Spring Boot 4.x estável compatível, Spring Security 7.x gerenciado pelo BOM, Maven Wrapper; Spring MVC + Virtual Threads quando apropriado. Não misturar webflux e servlet sem ADR técnica.
- **Frontend:** React 19, Next.js estável compatível, TypeScript com `strict: true`, SSR/SEO para a página pública, abordagem mobile-first, acessibilidade WCAG 2.2 AA como meta.
- **Lint:** respeitar preferência por regras compatíveis com ESLint 8.31 quando compatível com versões seguras do frontend; se houver incompatibilidade de dependências, documentar e adotar versão estável suportada **sem afrouxar regras**, justificando no ADR. Proibir `any`, `as any`, objetos não tipados, `@ts-ignore` sem justificativa e erros engolidos.
- **Dados:** MySQL 8.4 LTS, InnoDB, Flyway, UTC no banco e ISO 8601 na API, decimais monetários inteiros em centavos (`BIGINT`/`long`), sem `float/double` para dinheiro.
- **Fila e tarefas:** tabela transacional MySQL + outbox + workers com `FOR UPDATE SKIP LOCKED` exclusivamente para filas; não usar `SKIP LOCKED` para calcular rankings, apurações ou selecionar arbitrariamente números com visão inconsistente.
- **Infra:** Docker Compose para desenvolvimento, contêineres com usuário não privilegiado, TLS no proxy em produção, backup testado, healthchecks; evitar Redis/Kafka/Kubernetes na fase inicial.
- **API:** OpenAPI 3.1, `application/problem+json` com erros estruturados, paginação e idempotência de comandos relevantes.
- **Login:** autenticação de sessão segura com cookie HttpOnly, Secure, SameSite e proteção CSRF; passkeys/WebAuthn preferidas com fallback seguro. MFA para administradores. Não pôr JWT de acesso em localStorage.
- **Pagamentos:** abstração `PaymentProvider`; `MockPaymentProvider` por padrão; implementação real somente após aprovação. Conciliar webhooks com a fonte autorizada, sem confiar em resposta do browser.
- **Apuração:** motor determinístico, fórmula **exatamente igual à aprovada**, publicação de snapshots verificáveis, múltiplas fontes auditáveis, nunca improvisar número vencedor ou substituir critério após o encerramento.

Estrutura de referência:

```text
/
├── AGENTS.md                       # arquivo de descoberta automática do Codex
├── AGENT.md                        # contrato de execução completo
├── IMPLEMENTATION_PLAN.md          # roadmap executável/checklist
├── README.md
├── docs/
│   ├── architecture/               # C4, ADRs e fluxos
│   ├── adr/
│   ├── compliance/                 # templates, políticas, análise externa
│   ├── progress/
│   ├── operations/                 # runbooks, recuperação, deploy
│   ├── security/                   # threat model e red-team
│   └── api/                        # OpenAPI exportada
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── src/main/java/br/com/campaign/
│   │   ├── shared/
│   │   ├── identity/
│   │   ├── compliance/
│   │   ├── campaign/
│   │   ├── participation/
│   │   ├── payment/
│   │   ├── referral/
│   │   ├── ranking/
│   │   ├── drawing/
│   │   ├── award/
│   │   ├── audit/
│   │   └── notification/
│   ├── src/main/resources/db/migration/
│   └── src/test/
├── frontend/
│   ├── app/
│   ├── components/
│   ├── features/
│   ├── lib/
│   ├── tests/
│   └── package.json
├── infrastructure/
│   ├── compose.yaml
│   ├── monitoring/
│   └── reverse-proxy/
├── scripts/
└── .github/workflows/
```

A estrutura é adaptável mediante ADR, mas não se pode introduzir novos serviços sem ganho comprovado. Separe os domínios por pacotes com limites explícitos. Não compartilhar entidades JPA indiscriminadamente; use interfaces de aplicação e eventos internos.

## 5. Regras de domínio que jamais podem ser quebradas

### 5.1. Legalidade como pré-condição operacional

- `DEMO` significa simulações sem cobrança e sem promessa de prêmios reais; rotular a UI e os dados.
- `COMMERCIAL_PROMOTION` exige pessoa jurídica elegível, promoção gratuita, certificado/escopo/datas e premiação permitida; **nenhuma taxa, compra obrigatória de número ou prêmio em dinheiro**.
- `PHILANTHROPIC_AUTHORIZED` exige entidade elegível, prova de titularidade/doação de bens, autorização aplicável, destinação de recursos e aprovação do PSP. Nenhuma finalidade de enriquecimento privado.
- Outras modalidades só entram após ADR + parecer jurídico e aprovação específica. Não criar uma opção `PRIVATE_RAFFLE` executável em produção.
- Bloqueio em três níveis: **UI, API e serviço de domínio**, além do padrão de infraestrutura `LIVE_OPERATIONS_ENABLED=false`.
- Nenhum administrador pode contornar autorização com simples feature flag, manipulação de UI ou mudança de variável de ambiente.
- A prova de autorização deve ser anexada, validada por dois papéis independentes, vinculada a emissor, titular, intervalo, modalidade, limites de números e plano aprovado. O sistema **não** decide autenticidade jurídica sozinho.

### 5.2. Números e participações

- Espaço de cinco dígitos configurável: 0–99.999 (100.000 posições) **com zeros à esquerda na apresentação**. A escolha do intervalo e método de distribuição depende do plano autorizado; 00000 não pode ser suprimido silenciosamente da apuração.
- Chave única `(campaign_id, number_value)`; controle de estado com versão otimista/transação e restrições de banco.
- Atribuição aleatória/equitativa em promoção comercial, observadas as regras aplicáveis; seleção manual de número pelo participante **não é requisito aprovado**.
- Números só integram a lista elegível após todos os requisitos do modo operacional; nunca entregar prêmio baseado em entrada não elegível.
- Contagem de cliques, indicações, participações, pedidos, cobranças e pagamentos são métricas diferentes.
- Congelamento antes da apuração; alterações posteriores requerem evento compensatório auditável e procedimento aprovado, jamais edição silenciosa.

### 5.3. Dinheiro e comissões

- `Money` sempre com moeda `BRL`, centavos como inteiro, limites e arredondamentos explícitos.
- Nunca liberar participação por QR Code gerado, screenshot ou resposta do frontend; somente evento financeiro confirmado e conciliado, **quando o modelo aprovado exigir pagamento**.
- Webhooks autenticados e idempotentes; transação confirmada deve possuir identificadores únicos do PSP e auditoria.
- O valor destinado a indicações não vira automaticamente prêmio permitido. Distinguir **remuneração contratual de prestação de serviço** de **prêmio promocional** e submeter à revisão jurídica, contábil e tributária.
- Rankings baseados em aquisições pagas e prêmios em espécie ficam **desativados** por padrão até confirmação de legalidade; no modo gratuito, usar métricas promocionais juridicamente aprovadas.
- Evitar saldo fictício de carteira, retiradas ou promessa de pagamento na UI sem operação aprovada.

### 5.4. Fonte oficial e prêmios-surpresa

- Resultado da Loteria Federal: consumir fonte oficial verificável **ou** usar importação manual com dupla aprovação e registro de evidência, sem presumir API pública oficial.
- Motor aceita `DrawRule` versionada; regra, tratamento de empate, contingência, cancelamento de extração, número não distribuído e prêmio acumulado devem ser fixados antes da publicação da campanha.
- Prêmios-surpresa: compromisso SHA-256 (`commitment`) com sal aleatório forte e dados canônicos **antes** da campanha, revelação auditável conforme regulamento, sem substituição de números a posteriori.
- Evitar vazamento precoce de segredo e números-surpresa; armazenar segredo cifrado e restringir acesso. Prova criptográfica demonstra imutabilidade, não automaticamente conformidade jurídica.

## 6. Segurança e privacidade (sem exceções)

- Adotar OWASP ASVS nível 2 como baseline, OWASP API Security Top 10, STRIDE no threat model e revisão contra abuso de negócio.
- Princípio do menor privilégio; autorização por recurso, por campanha e por tenant/organização, inclusive para consultas e exportações. Controles independentes para operadores, aprovadores jurídicos, financeiro, suporte e auditor.
- Nunca logar CPF completo, tokens, cookies, QR Code sensível, segredos ou dados de conta completos; mascarar dados pessoais e estabelecer retenção/expurgo.
- CPF e dados de elegibilidade devem ser protegidos por criptografia de campo quando adequado, índices por HMAC para busca exata e gestão segura de chaves. Não publicar rankings que revelem identidade integral.
- Input validation, anti-CSRF, headers CSP/HSTS, CORS restrito, limitação por IP/usuário, WAF, proteção anti-bot e contra enumeration.
- Administrador: MFA, sessão curta, reautenticação para ações de risco, dupla aprovação para ativação e apuração manual.
- Auditoria append-only, encadeamento de hashes/assinaturas onde apropriado, backups imutáveis e exportação verificável; não alegar impossibilidade absoluta de adulteração.
- Pipeline: SBOM, SCA, SAST, secrets scan, análise de containers, testes de migração, dependabot/atualização controlada.
- Testar cenários de fraude: autocadastro em massa, autindicação, cookie stuffing, replay de webhooks, race condition, atribuição duplicada, mudança de sorteado, adulteração de ranking, IDOR e elevação de privilégio.
- Política de privacidade, cookies, finalidade, base legal, retenção, direitos dos titulares, resposta a incidentes e registro de operações de tratamento conforme LGPD.

## 7. Padrões de código

### Backend

- Java 25, código formatado consistentemente, SOLID pragmático, constructor injection, records imutáveis para DTOs, `sealed` types se úteis, interfaces para integrações.
- Controllers finos; serviços de aplicação orquestram casos de uso; domínio contém invariantes; repositórios encapsulam persistência.
- Jakarta Validation para DTOs; nunca confiar no frontend. Exceções mapeadas em `ProblemDetail`, com códigos estáveis e correlation ID.
- `@Transactional` em fronteiras claras, SQL indexado, timeouts explícitos e retry somente para falhas comprovadamente transitórias e comandos idempotentes.
- Não retornar entidades JPA da API. Mapear campos permitidos explicitamente.
- Clock e gerador de aleatoriedade injetáveis para testes; `SecureRandom` para segredos, não para compensar regras legais.
- `Instant` em persistência; mostrar horários com `America/Sao_Paulo` na UI e timezone informado no regulamento.

### Frontend

- TypeScript `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` quando compatíveis, schemas com Zod e cliente gerado a partir de OpenAPI quando possível.
- Nunca usar `any`, nem desabilitar regras de lint globalmente. Tipar hooks, eventos, DTOs, respostas e tratamento de erros.
- Server Components para conteúdo estático/SEO e Client Components somente para interação; evitar exposição de segredos a `NEXT_PUBLIC_*`.
- Estados loading/empty/error/success, acessibilidade por teclado, descrição de status para leitores de tela, contraste e responsividade.
- Todos os textos sensíveis a autorização e preço devem vir do backend; frontend não decide habilitar legalmente uma operação.

### Banco

- Flyway: migrações forward-only, `snake_case`, FKs e índices criados junto com consultas necessárias.
- IDs públicos opacos (UUID/ULID) e chaves internas eficientes, evitar enumeração previsível em endpoints.
- Constrangimentos de unicidade suportam a última linha de defesa de idempotência.
- Evitar SQL de relatório que altere o estado; snapshots de ranking e apuração reproduzíveis.

## 8. Qualidade exigida

| Tipo | Padrão de aceite |
|---|---|
| Unitário | Toda regra de negócio crítica com casos positivos, negativos e limites |
| Integração | Testcontainers MySQL real; migrations do zero; concorrência e índices |
| API | Contratos OpenAPI e testes de autorização/idempotência/validação |
| Frontend | Componentes críticos com testes; E2E Playwright em fluxos principais |
| Segurança | Nenhum achado crítico/alto conhecido sem mitigação e aprovação formal |
| Concorrência | 100+ reservas concorrentes sem duplicidade; resultado reproduzível |
| Pagamento | Duplicata, reordenação, atraso, timeout, estorno e assinatura inválida |
| Apuração | Vetores fixos de teste; `00000`, números ausentes, adiantamento/adiamento |
| Infra/DB | Backup e restauração comprovados; CI verde; nenhum segredo no git |

As metas de performance e cobertura mensuráveis são definidas em `IMPLEMENTATION_PLAN.md`; não falsificar medição quando hardware/serviços não estiverem disponíveis.

## 9. Red Team obrigatório

Antes do release candidate, assuma o papel de revisor adversarial e produza `docs/security/red-team-report.md` com:

1. Superfícies de ataque, ativos sensíveis, fluxos de confiança e threat model.
2. Ataques de domínio (estoque, números, rankings, indicações, auditoria e extração).
3. Segurança de autenticação, recuperação de conta, autorização por recurso, multi-tenant, CSRF, SSRF, XSS, SQLi e upload.
4. Webhook replay, falsificação de pagamento, duplicação de prêmio e manipulação de conciliação.
5. Privacidade LGPD, exportação, retenção, armazenamento de credenciais e vazamento em logs.
6. Severidade, prova de conceito **somente em ambiente local autorizado**, correção, re-teste e risco residual.

O revisor Red Team deve contestar a própria solução proposta, não apenas confirmar testes felizes. Não realizar varredura contra sites ou provedores terceiros sem autorização.

## 10. Regra de não bloqueio, mas com gates obrigatórios

Pode concluir toda a solução em modo `DEMO`: UX, simulação de pagamento, números, auditoria, ranking fictício, importação de extração simulada, relatórios e deployment local. A única coisa que não pode é declarar operação pública pronta sem os gates externos.

### Gates humanos não automatizáveis

- **G-LEGAL:** advogado/organizador aprovou a modalidade, regulamento, promotor e escopo; certificado válido anexado quando necessário.
- **G-PSP:** contrato e homologação do PSP autorizam a atividade e conta receptora.
- **G-FINANCE:** tributação, origem/destinação de recursos, eventual remuneração e custos foram aprovados.
- **G-SECURITY:** revisão independente de segurança, privacidade e recuperação de desastre aceita.
- **G-OPERATIONS:** domínio, backups, alertas, monitoramento e atendimento validados.
- **G-LAUNCH:** responsável habilitou explicitamente a campanha e os recursos legais específicos (não apenas um toggle).

Falta de gate impede **produção real**, não testes locais e homologação.

## 11. Guia de comportamento final do agente

- Comece por `P00`, não pule dependências nem invente decisões legais.
- Ao finalizar cada marco, forneça resumo, comandos de teste e o que ainda falta para o próximo.
- Quando ocorrer erro, investigue a causa raiz; não remova teste nem desabilite checagem para produzir verde falso.
- Não hardcode credenciais, nomes de empresas, CPF, valores de gateway, datas oficiais de extração, certificados ou resultados vencedores reais.
- Mostre configurações reais em `.env.example`, com valores não sensíveis e seguros para desenvolvimento.
- Se houver ambiguidade, escolha a alternativa mais segura e reversível, registre-a e continue.
- **Nunca toque em recursos de outros projetos/workspaces** (arquivos, dados, volumes/contêineres/imagens Docker, branches/worktrees, chaves). Limite toda operação ao escopo deste repositório, filtre por nome/prefixo do projeto e, na dúvida sobre a propriedade, **não remova — pergunte**. Prefira comandos específicos a `prune`/remoções amplas.
- Ao encerrar a implementação, produza `README.md`, instruções locais testadas, OpenAPI, documentação de infraestrutura, matriz de rastreabilidade e relatório das gates externos. **Não afirmar que houve autorização jurídica.**

## 12. Referências normativas e técnicas a revisar antes do lançamento

- ANPD e LGPD: https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/guia-orientativo-sobre-seguranca-da-informacao-para-agentes-de-tratamento-de-pequeno-porte
- Spring Boot requisitos: https://docs.spring.io/spring-boot/4.0/system-requirements.html
- Spring Security Passkeys: https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passkeys.html
- MySQL SKIP LOCKED: https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html
- Codex / AGENTS.md: https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex


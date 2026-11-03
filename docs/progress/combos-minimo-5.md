# Combos — mínimo de 5 números e combos até 5.000

- **Commit/branch ou estado local:** `feature/combos-minimo-5`, criada de `feature/p20-fase2` @ `a29f2cb`. Commit local,
  nada enviado ao remoto.

- **Tarefas concluídas e arquivos alterados:**
  - Ofertas de 5 (R$ 5,00), 10 (R$ 9,50, 5%), 100 (R$ 87,00, 13%), 1.000 (R$ 740,00, 26%) e 5.000 (R$ 3.450,00, 31%).
  - Mínimo de 5 números e R$ 5,00, máximo de 5.000: `PurchaseOffer`, `PurchaseOfferService` e migração `V025`.
    Ofertas antigas abaixo do mínimo ficam inativas, sem apagar histórico.
  - `GET .../offers` passa a devolver `unitCents` e `discountPct`, por quantidade crescente; `GET /campaigns/public/{id}`
    devolve `entryPriceCents`. `ComboCards`, CTAs e texto de indicação consomem esses valores; `PRICE_UNIT_CENTS` removido.
  - Limite padrão por participante: de 100 para 10.000 números (D-012).
  - "Meus números": totais por SQL, sem depender da lista limitada (agora 10.000 linhas).
  - Reserva, confirmação, indicação e estorno sem custo quadrático: comandos em lote e leitura por conjunto de ids.
  - Correção encontrada no caminho: `/participar?combo=` nunca pré-selecionava, pois comparava `combo10` com o UUID da oferta.
  - Seed local: combos atuais e 20.000 números; banco local antigo é atualizado ao subir.
  - Página de pagamento mostra 60 números e aponta para Meus números nos combos maiores.

- **Comandos executados e resultados:**
  - `./mvnw -B verify`, 1ª execução → FAIL: 247 testes, 4 falhas (cenários antigos com oferta de 2 números).
  - 2ª e 3ª execuções → FAIL: 247 testes, 1 erro em `LoadAndFreezeIntegrationTest` (`Connection is not available`
    após 60 s de espera no pool).
  - Comparação de `ConcurrencyLimits`, `HighVolumeThroughput` e `LoadAndFreeze`, duas rodadas em cada versão:
    `a29f2cb` 36–39 s para 250 participações; código atual 37–39 s; todas passaram. Sem regressão medida.
  - Causa provável do erro, não confirmada por experimento isolado: margem curta do teste na suíte completa (7 contêineres
    MySQL em Docker de 3 GB, 250 pedidos em fila num pool de 50). A espera do pool nos três testes de carga subiu de 60 s
    para 120 s; as verificações não mudaram.
  - `./mvnw -B verify`, 4ª execução → **247 testes, 0 falhas, 0 erros, BUILD SUCCESS**. O erro ocorreu em 2 de 3 execuções
    completas antes do ajuste; só houve uma execução completa depois dele.
  - `npm run typecheck` e `npm run lint` → OK; `npm run build` → OK.
  - `npx playwright test` → 15 passed. Executado antes do último ajuste, de 4 px, no espaçamento do card de combo.
  - Navegador: 5 cards com preços e descontos do servidor; `/participar?combo=` do combo de 5.000 mostra R$ 3.450,00;
    sem rolagem horizontal em 375 px.

- **Testes, cobertura e logs relevantes:** novo `LargeComboIntegrationTest` (limites, descontos e combo de 5.000 ponta a
  ponta, com indicação e estorno). Tempos do combo de 5.000 no Testcontainers, com a campanha travada na reserva e na
  confirmação:

  | Etapa | Isolado | Suíte completa |
  |---|---|---|
  | Reserva | 4,2 s | 3,9–15,4 s |
  | Confirmação | 5,9 s (144 s antes da correção) | 6,8–32,7 s |
  | Estorno | 4,5 s | 8,0–61,3 s |

  Cobertura JaCoCo não medida.

- **Contratos/ADRs/migrações alterados:** migração `V025`; campos novos em ofertas e no estado público; `quantity` 5..5000 e
  `totalCents` ≥ 500 na criação de oferta; propriedades `hibernate.jdbc.batch_size=100` e `order_updates=true`; D-012.

- **Riscos, limitações e bloqueios:**
  - Combo de 5.000 trava a campanha por segundos: outras compras esperam nesse intervalo. Sem medição em produção.
  - O estorno de 5.000 ainda grava uma auditoria por número.
  - Fluxo de 5.000 pela interface não testado no E2E; coberto no serviço. Combo de 1.000 não medido à parte.
  - `frontend/src/app/regulamento/page.tsx` continua dizendo "Cada número custa R$ 1,00". Não alterado: documento legal
    depende de aprovação humana.
  - A reserva pega os menores números disponíveis (`order by numberValue`), mas o regulamento fala em atribuição aleatória.
    Comportamento anterior a esta mudança, mais visível em combos grandes. Não alterado.
  - Limite de 10.000 números por participante é escolha reversível, a confirmar.
  - `docs/compliance/gates-*` seguem com os valores registrados pelos gates (R$ 1,00 e R$ 0,56).

- **Critérios de aceite:**
  | Item | Resultado |
  |---|---|
  | Mínimo de 5 números por R$ 5,00 no servidor e no banco | PASS |
  | Combos e descontos servidos pelo backend e exibidos | PASS |
  | Combo de 5.000 ponta a ponta (serviço) | PASS |
  | Suíte completa e E2E | PASS (ver ressalvas acima) |
  | Combo de 5.000 pela interface | NÃO EXECUTADO |

- **Próxima etapa:** aprovar o texto do regulamento e decidir a atribuição aleatória.

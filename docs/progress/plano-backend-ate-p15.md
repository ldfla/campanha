# Plano de execução do backend até P15

Base verificada: `origin/main` em `76275d0`, Java 25.0.3 e MySQL 8.4 via Testcontainers. Em 31/10/2026, `./backend/mvnw -B verify` concluiu com 101 testes, zero falhas e zero erros. Os checkboxes oficiais permanecem em `IMPLEMENTATION_PLAN.md` e só serão alterados com evidência por fase.

1. Revisar pendências de P07–P10, distinguindo tarefas de interface, regras dependentes de autorização e cobertura de backend. Corrigir falhas de backend comprovadas; manter mocks para integrações externas e registrar testes ainda ausentes.
2. P11: reconciliar o protótipo existente com a main e a migração V011 de ranking, criar migração forward-only livre, compromisso e segredo cifrado, publicação antes da abertura, revelação autorizada, testes negativos e evidência.
3. P12: manter fora do escopo por instrução anterior; P13 deve permanecer bloqueada nos fluxos que exigem resultados apurados enquanto não houver contrato verificável de resultados, mas componentes independentes podem ser desenvolvidos e testados.
4. P13: modelar o fluxo de entrega com evidências privadas, autorização por organização, estados e testes; não inventar contemplados ou resultados.
5. P14: implementar simulação em centavos, conciliação e relatórios autorizados, exportação CSV protegida e testes financeiros negativos.
6. P15: implementar templates versionados, outbox com retry e deduplicação, comunicação simulada, suporte e testes de falhas/concurrency.
7. Para cada fase: aplicar o guia Java à concorrência e contratos, executar testes direcionados e `./backend/mvnw -B verify`, atualizar evidências e checkboxes comprovados, commitar em branch própria e criar PR conforme autorização existente. Usar SKIP LOCKED exclusivamente para filas.
8. Executar rodada Red Team independente após mudanças sensíveis e retestes após correções; executar Trivy e registrar resultados sem alegar ausência absoluta de riscos.

Restrições: nenhuma integração real, pagamento, divulgação, deploy ou autorização jurídica inferida. P06 está fora do escopo backend. A implementação e o fechamento de findings devem respeitar a separação de papéis prevista na skill `red-team-security`.

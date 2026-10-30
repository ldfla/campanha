# ADR-0001 — Monólito modular

**Status:** Aceito  
**Data:** 2026-10-30  
**Decisores:** Equipe de engenharia

## Contexto

A plataforma precisa de múltiplos domínios (identidade, campanhas, pagamentos, sorteios, auditoria) mas o volume previsto é de ~100k transações/mês com um único time de desenvolvimento.

## Decisão

Adotar **monólito modular** com pacotes Java separados por domínio, limites explícitos via interfaces de aplicação e eventos internos. Sem microsserviços, sem Kafka, sem Redis na fase inicial.

## Justificativa

- Volume não justifica complexidade distribuída
- Um único artefato simplifica deploy, debugging e transações
- Pacotes por domínio permitem extração futura se necessário
- Flyway gerencia schema único com migrações por domínio

## Consequências

- Todos os domínios compartilham o mesmo processo JVM e banco MySQL
- Comunicação entre domínios via eventos na mesma transação (outbox)
- Escalabilidade vertical é suficiente para o volume previsto
- Extração de microsserviço requer refatoração de interfaces

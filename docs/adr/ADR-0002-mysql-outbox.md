# ADR-0002 — MySQL como banco único + padrão outbox

**Status:** Aceito  
**Data:** 2026-10-30  
**Decisores:** Equipe de engenharia

## Contexto

Necessidade de fila de eventos, jobs assíncronos e consistência transacional entre domínios sem infraestrutura adicional.

## Decisão

Usar **MySQL 8.4 LTS** como banco único, com tabelas `outbox_events` e `inbox_events` para comunicação assíncrona. Workers usam `SELECT ... FOR UPDATE SKIP LOCKED` para processamento de filas.

## Justificativa

- Consistência transacional entre dados de negócio e eventos na mesma transação
- `SKIP LOCKED` evita bloqueio entre workers concorrentes
- Sem dependência de Redis/Kafka/RabbitMQ na fase inicial
- MySQL 8.4 LTS com suporte até 2032

## Restrições

- `SKIP LOCKED` é usado **exclusivamente para filas** — nunca para calcular rankings, apurações ou selecionar números
- Rankings e apurações usam snapshots materializados com leitura consistente
- Workers com retry exponencial e dead letter para eventos falhos

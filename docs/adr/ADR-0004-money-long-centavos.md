# ADR-0004 — Representação monetária com long centavos

**Status:** Aceito  
**Data:** 2026-10-30  
**Decisores:** Equipe de engenharia

## Contexto

O sistema manipula valores monetários em BRL para pagamentos, prêmios, comissões e simulações financeiras. Necessidade de precisão absoluta e performance.

## Decisão

Usar `long` (centavos inteiros) com record `Money` imutável. Sem JSR-354/Moneta, sem `BigDecimal` para armazenamento.

## Justificativa

- Moeda única (BRL), sem frações de centavo, sem câmbio
- Mapeamento direto `long` ↔ MySQL `BIGINT` sem conversão
- `long` suporta até R$ 92 quadrilhões — overflow impossível neste domínio
- Padrão da indústria: Stripe, Square, PagSeguro usam centavos inteiros
- JSR-354/Moneta internamente usa BigDecimal (~10x mais lento), sem ganho para este caso
- Math.addExact/subtractExact/multiplyExact detectam overflow

## Banco

`BIGINT amount_cents NOT NULL, CHAR(3) currency DEFAULT 'BRL'`

## API/JSON

Trafegar `amountCents` (long) + `currency` (string), nunca float.

## Frontend

Formatar com `Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'})`.

## Migração futura

Se multi-moeda for necessário, migrar para JSR-354 é trivial — basta mudar a implementação interna do record `Money`.

# ADR-0007 — Modalidade: Promoção Comercial (Sorteio) via título de capitalização premiável

**Status:** Aceito (decisão de negócio/jurídica) — **implementação pendente**  
**Data:** 2026-11-01  
**Decisores:** Organizador / Jurídico (G-LEGAL)

## Contexto

A plataforma foi construída em **DEMO**, com `COMMERCIAL_PROMOTION` recusando cobrança e prêmio em
dinheiro (AGENTS.md §5.1). O organizador definiu a modalidade real da operação.

## Decisão

A operação será de **Promoção Comercial na forma de Sorteio**, vinculada à **Extração da Loteria
Federal** (data a definir), operada como **título de capitalização premiável** emitido por **ViaCap**
— **Autorização SUSEP nº 15414.657358/2026-61**. Haverá:
- prêmio principal por **número da sorte** (apuração pela Loteria Federal);
- **apuração complementar por performance de indicação**;
- prêmios a **maiores contribuidores/indicadores**.

O Regulamento detalhará: formação do número da sorte, data da apuração e **regra de desempate**
(volume de números vendidos e timestamp da primeira compra).

## Justificativa

É o instrumento autorizado que permite **cobrança** e **premiação (inclusive em dinheiro)** de forma
regulada (SUSEP), superando a vedação genérica de dinheiro em promoção comercial que vale para
promoções gratuitas.

## Consequências

- **Não muda por flag.** A modalidade só se torna executável quando o **documento do título**
  (autorização SUSEP) e o **regulamento versionado** estiverem anexados e aprovados por dois papéis
  independentes (fluxo `campaign_authorizations` + gates). Até lá, a plataforma permanece **DEMO**.
- Mudança de domínio ampla (a implementar, requer ADR de execução + testes):
  1. novo valor de `CampaignMode` para **título de capitalização premiável** (não reutilizar
     `COMMERCIAL_PROMOTION`, para não confundir os dois regimes);
  2. permitir cobrança (PSP) **somente** nessa modalidade e com autorização válida;
  3. permitir prêmio em dinheiro **somente** nessa modalidade (ajustar `PrizeType`/validação e o
     teste que hoje bloqueia dinheiro em todos os modos);
  4. apuração: manter o motor da P12 (Federal) e acrescentar a **apuração complementar por
     indicação**, com desempate fixo (volume + timestamp da 1ª compra) — ver ADR-0006/0009;
  5. registrar a **regra de desempate** no regulamento versionado (`approved_regulation_sha256`).
- Enquanto não implementado, `COMMERCIAL_PROMOTION` continua recusando cobrança e dinheiro.

## Riscos

- Regime de capitalização premiável tem regras próprias (SUSEP) que precisam ser refletidas no
  regulamento e na apuração; a fórmula fechada segue **OPEN** (D-OPEN-008).
- Não confundir "promoção comercial gratuita" com "título de capitalização": são regimes distintos.

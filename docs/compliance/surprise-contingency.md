# Contingência — número-surpresa não atribuído (P11)

> **Provisório e sujeito a revisão jurídica (`G-LEGAL`).** Não é parecer jurídico nem
> substitui o regulamento aprovado. Nenhum algoritmo é inventado aqui.

## Contexto

A campanha de referência distribui 100.000 posições (0–99.999). Em DEMO, 110 números são
sorteados por `SecureRandom` e selados por commit-reveal antes da abertura
(2 tiers: 10×R$ 100 e 100×R$ 10).

## Regra proposta (a fixar no regulamento antes da abertura)

1. **Número revelado mas não atribuído.** Se, na data da apuração, um número-surpresa revelado
   não estiver atribuído a nenhum participante elegível, o prêmio correspondente é declarado
   **deserto** — não é redistribuído, não é convertido em bônus e não altera os demais tiers.
2. **Proibição de re-sorteio.** É vedado substituir o número deserto por outro, ainda que o novo
   número seja publicado depois. A integridade do compromisso SHA-256 é pré-condição.
3. **Registro.** O número deserto e a justificativa são publicados no pacote de transparência da
   apuração, com o hash do congelamento das participações elegíveis.
4. **Efeito financeiro.** O valor do prêmio deserto não é pago a ninguém e não é realocado;
   a prestação de contas registra o valor não utilizado.

## Pendências que dependem de decisão humana

- Confirmar se o valor deserto é realocado para outra finalidade (doação) ou permanece retido —
  decisão jurídica/contábil, não técnica (`G-FINANCE`).
- Vincular esta regra ao texto do regulamento versionado (P05/P12) antes da abertura.
- Confirmar tratamento do `00000` e de números não distribuídos na apuração principal (P12).

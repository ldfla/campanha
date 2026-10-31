# Simulação financeira hipotética

`POST /financial/campaigns/{campaignId}/simulations` calcula um cenário sem persistir movimentações ou alterar a campanha. Requer sessão autenticada, CSRF e papel financeiro, gestor de campanha ou administrador de sistema com vínculo ativo na organização da campanha; a conta também precisa estar ativa.

```json
{
  "priceCents": 100,
  "unitCostCents": 56,
  "capacity": 100,
  "quantity": 100,
  "feeBasisPoints": 1000,
  "taxBasisPoints": 0,
  "targetProfitCents": 2000,
  "fixedCosts": [{"category": "PRIZE", "cents": 1000}]
}
```

Todos os valores monetários são inteiros em centavos de BRL. Quantidades e pontos-base também usam tokens JSON inteiros; frações, notação exponencial e strings numéricas são rejeitadas. As categorias aceitas são `PRIZE`, `COMMISSION`, `PLATFORM`, `COMMUNICATION` e `OTHER`. Até 100 custos fixos e 100.000 unidades podem ser informados. Taxas e tributos incidem separadamente sobre a receita bruta agregada, com arredondamento ao centavo mais próximo e metade para cima. A soma de seus pontos-base não pode superar 10.000. Tributos são apenas parâmetros a validar; o cálculo não determina regime fiscal.

A resposta identifica `currency: BRL`, `hypothetical: true`, informa receita, custos, taxas, tributos, lucro, aviso e quantidades mínimas para equilíbrio e lucro-alvo. Uma quantidade é `null` quando a meta não é atingida na capacidade. Cenários hipotéticos não representam caixa real, elegibilidade de prêmios, aprovação jurídica ou permissão para cobrar.

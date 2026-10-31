export const CAMPAIGN_MODE = "DEMO" as const;

export const CAMPAIGN_STATUS: "ACTIVE" | "SOLD_OUT" | "SUSPENDED" = "ACTIVE";

export const PRICE_UNIT_CENTS = 65;

export const TOTAL_NUMBERS = 100_000;

export const TOTAL_PRIZES_CENTS = 2_530_000;

export const ORGANIZER = {
  name: "LDF Tecnologia da Informação",
  cnpj: "34.352.581/0001-42",
  cnpjMasked: "34.352.***/**01-42",
} as const;

export const DRAWING_DATE = "2026-12-20" as const;

export const COMBOS = [
  { id: "curioso", qty: 10, totalCents: 650, label: "Curioso", unitCents: 65, badge: null, popular: false },
  { id: "torcida", qty: 100, totalCents: 5000, label: "Torcida", unitCents: 50, badge: "Mais Popular", popular: true },
  { id: "campeao", qty: 1000, totalCents: 46000, label: "Campeão", unitCents: 46, badge: "Máximo Desconto", popular: false },
  { id: "anjo", qty: 5000, totalCents: 220_000, label: "Anjo", unitCents: 44, badge: "Apoiador", popular: false },
] as const;

export const PRIZES = [
  {
    tag: "1º Prêmio",
    name: "Kit Fotográfico Canon",
    items: "Canon D70 + Lente 24-70 f/4 + Flash 430EX",
    valueCents: 1_000_000,
  },
  {
    tag: "2º Prêmio",
    name: "MacBook Air M1",
    items: '13" · 8GB · Apple Silicon',
    valueCents: 400_000,
  },
  {
    tag: "3º Prêmio",
    name: "Kit Canon T1i Rebel",
    items: "Câmera + acessórios",
    valueCents: 200_000,
  },
  {
    tag: "4º Prêmio",
    name: "Pix na sua conta",
    items: "Transferência direta",
    valueCents: 100_000,
  },
  {
    tag: "5º Prêmio",
    name: "Pix na sua conta",
    items: "Transferência direta",
    valueCents: 50_000,
  },
] as const;

export const RANKING_REWARDS = [
  {
    scope: "BUYERS" as const,
    title: "Maiores apoiadores",
    subtitle: "Quem mais comprou números",
    tiebreaker: "Data da primeira cota apoiada",
    positions: [
      { rank: 1, label: "1º apoiador", valueCents: 150_000 },
      { rank: 2, label: "2º apoiador", valueCents: 90_000 },
      { rank: 3, label: "3º apoiador", valueCents: 50_000 },
    ],
  },
  {
    scope: "INFLUENCERS" as const,
    title: "Maiores divulgadores",
    subtitle: "Mais indicações convertidas pelo link",
    tiebreaker: "Data da primeira cota apoiada",
    positions: [
      { rank: 1, label: "1º divulgador", valueCents: 150_000 },
      { rank: 2, label: "2º divulgador", valueCents: 90_000 },
      { rank: 3, label: "3º divulgador", valueCents: 50_000 },
    ],
  },
] as const;

export const SURPRISE_PRIZES = [
  { count: 10, valueCentsEach: 10_000, label: "10 prêmios-surpresa", unitLabel: "R$ 100,00 cada" },
  { count: 100, valueCentsEach: 1_000, label: "100 prêmios-surpresa", unitLabel: "R$ 10,00 cada" },
] as const;

export function formatBRL(cents: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(cents / 100);
}

export function isSoldOut(): boolean {
  return CAMPAIGN_STATUS === "SOLD_OUT";
}

export function isSuspended(): boolean {
  return CAMPAIGN_STATUS === "SUSPENDED";
}

export function isDemoMode(): boolean {
  return CAMPAIGN_MODE === "DEMO";
}

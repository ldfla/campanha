export const CAMPAIGN_MODE = "DEMO" as const;

export const CAMPAIGN_STATUS: "ACTIVE" | "SOLD_OUT" = "ACTIVE";

export const PRICE_UNIT_CENTS = 65;

export const TOTAL_NUMBERS = 100_000;

export const TOTAL_PRIZES_CENTS = 1_950_000;

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

export function isDemoMode(): boolean {
  return CAMPAIGN_MODE === "DEMO";
}

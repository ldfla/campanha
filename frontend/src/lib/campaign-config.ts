export const CAMPAIGN_MODE = "DEMO" as const;

export const CAMPAIGN_STATUS: "ACTIVE" | "SOLD_OUT" | "SUSPENDED" = "ACTIVE";

export const PRICE_UNIT_CENTS = 100;

export const TOTAL_NUMBERS = 100_000;

export const TOTAL_PRIZES_CENTS = 2_530_000;

export const ORGANIZER = {
  name: "LDF Tecnologia da Informação",
  cnpj: "34.352.581/0001-42",
  cnpjMasked: "34.352.***/**01-42",
} as const;

export const DRAWING_DATE = "2026-12-20" as const;

export const COMBOS = [
  { id: "combo10", qty: 10, totalCents: 950, label: "Combo 10", unitCents: 95, discountPct: 5, badge: null, popular: false },
  { id: "combo100", qty: 100, totalCents: 8_700, label: "Combo 100", unitCents: 87, discountPct: 13, badge: "Mais Popular", popular: true },
  { id: "combo1000", qty: 1_000, totalCents: 74_000, label: "Combo 1.000", unitCents: 74, discountPct: 26, badge: "Máximo Desconto", popular: false },
  { id: "combo5000", qty: 5_000, totalCents: 345_000, label: "Combo 5.000", unitCents: 69, discountPct: 31, badge: "Apoiador", popular: false },
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
    scope: "PARTICIPANT" as const,
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
    scope: "PROMOTER" as const,
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

export const BENEFICIARY_ENTITIES = [
  { id: "graacc", name: "GRAACC", color: "#E4002B", logo: "/logos/graacc.svg", url: "https://graacc.org.br" },
  { id: "aacd", name: "AACD", color: "#0072BC", logo: "/logos/aacd.png", url: "https://aacd.org.br" },
  { id: "tucca", name: "TUCCA", color: "#8B5CF6", logo: "/logos/tucca.png", url: "https://tucca.org.br" },
  { id: "aacc", name: "AACC", color: "#EC4899", logo: "/logos/aacc.png", url: "https://aacc.org.br" },
  { id: "itaci", name: "ITACI", color: "#FF6B35", logo: "/logos/itaci.webp", url: "https://itaci.org.br" },
  { id: "casa-hope", name: "Casa Hope", color: "#00A651", logo: "/logos/casa-hope.png", url: "https://hope.org.br" },
] as const;

export const BENEFICIARY_POOL_CENTS = 1_000_000;

export const PRODUCT_LINKS: Record<string, string> = {
  "1-premio": "https://meli.la/13N8mXA",
  "2-premio": "https://meli.la/2Tpgeiy",
};

/** Identificador público (não secreto) da campanha exibida; padrão DEMO local. */
export const DEMO_CAMPAIGN_ID = process.env.NEXT_PUBLIC_CAMPAIGN_ID ?? "demo";

/** Normaliza o WhatsApp digitado para E.164 (+55…); a validação definitiva é do backend. */
export function toE164(input: string): string {
  const digits = input.replace(/\D/g, "");
  const withCountry = digits.startsWith("55") && digits.length >= 12 ? digits : `55${digits}`;
  return `+${withCountry}`;
}

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

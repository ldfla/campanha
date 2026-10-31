"use client";

import { useEffect, useState } from "react";

const NAMES = [
  "Ana M.", "Carlos S.", "Julia R.", "Pedro L.", "Fernanda C.",
  "Lucas O.", "Mariana T.", "Gustavo P.", "Camila D.", "Rafael B.",
  "Beatriz N.", "Diego F.", "Larissa K.", "Thiago V.", "Amanda G.",
  "Bruno H.", "Isabela A.", "Matheus E.", "Letícia W.", "João M.",
];

const QUANTITIES = [1, 1, 2, 1, 3, 1, 5, 1, 2, 1, 10, 1, 1, 2, 1];
const CITIES = [
  "São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba",
  "Salvador", "Brasília", "Fortaleza", "Recife", "Porto Alegre",
];

function seededRandom(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function pick<T>(arr: readonly T[], random: () => number): T {
  const item = arr[Math.floor(random() * arr.length)];
  if (item === undefined) {
    throw new Error("Lista de sorteio vazia");
  }
  return item;
}

function randomMinutes(random: () => number): number {
  return Math.floor(random() * 15) + 1;
}

interface Purchase {
  id: number;
  name: string;
  qty: number;
  city: string;
  minutes: number;
}

function initialPurchases(): Purchase[] {
  const random = seededRandom(20261221);
  return Array.from({ length: 8 }, (_, i) => ({
    id: i,
    name: pick(NAMES, random),
    qty: pick(QUANTITIES, random),
    city: pick(CITIES, random),
    minutes: randomMinutes(random),
  }));
}

export function LiveTicker() {
  const [purchases] = useState<Purchase[]>(initialPurchases);
  const [index, setIndex] = useState(0);
  const [show, setShow] = useState(true);

  useEffect(() => {
    if (purchases.length === 0) return;
    const interval = setInterval(() => {
      setIndex((current) => (current + 1) % purchases.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [purchases.length]);

  const visible = purchases[index];

  if (!visible || !show) return null;

  return (
    <div className="fixed bottom-[76px] left-3 z-40 max-w-[280px] sm:max-w-[300px] md:bottom-5 md:left-5">
      <div
        key={`${visible.id}-${visible.name}`}
        className="flex animate-slide-in items-center gap-2.5 rounded-xl border border-black/[0.06] bg-white/95 px-3.5 py-2.5 shadow-[0_4px_20px_rgba(0,0,0,0.1)] backdrop-blur-sm"
      >
        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-[var(--color-green)] text-[10px] font-bold text-[var(--color-forest)]">
          {visible.name.charAt(0)}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[12px] leading-tight text-[var(--color-ink)] sm:text-[13px]">
            <strong className="font-semibold">{visible.name}</strong>{" "}
            <span className="text-[var(--color-ink-soft)]">comprou</span>{" "}
            <strong className="font-semibold text-[var(--color-orange)]">
              {visible.qty} {visible.qty === 1 ? "nº" : "nºs"}
            </strong>
          </p>
          <p className="mt-px text-[10px] text-[var(--color-ink-soft)] sm:text-[11px]">
            {visible.city} · {visible.minutes}min
          </p>
        </div>
        <button
          onClick={() => setShow(false)}
          className="shrink-0 p-1 text-[var(--color-ink-soft)]/60 transition-colors hover:text-[var(--color-ink)]"
          aria-label="Fechar notificações"
        >
          <svg viewBox="0 0 12 12" fill="none" className="h-3 w-3" aria-hidden="true">
            <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  );
}

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

function randomItem<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomMinutes(): number {
  return Math.floor(Math.random() * 15) + 1;
}

interface Purchase {
  id: number;
  name: string;
  qty: number;
  city: string;
  minutes: number;
}

export function LiveTicker() {
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [visible, setVisible] = useState<Purchase | null>(null);
  const [show, setShow] = useState(true);

  useEffect(() => {
    const initial: Purchase[] = Array.from({ length: 8 }, (_, i) => ({
      id: i,
      name: randomItem(NAMES),
      qty: randomItem(QUANTITIES),
      city: randomItem(CITIES),
      minutes: randomMinutes(),
    }));
    setPurchases(initial);
    setVisible(initial[0]);
  }, []);

  useEffect(() => {
    if (purchases.length === 0) return;
    let idx = 0;
    const interval = setInterval(() => {
      idx = (idx + 1) % purchases.length;
      setVisible(purchases[idx]);
    }, 4000);
    return () => clearInterval(interval);
  }, [purchases]);

  if (!visible || !show) return null;

  return (
    <div className="fixed bottom-[76px] left-3 z-40 md:bottom-5 md:left-5 max-w-[280px] sm:max-w-[300px]">
      <div
        key={visible.id + "-" + visible.name}
        className="animate-slide-in bg-white/95 backdrop-blur-sm rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.1)] border border-black/[0.06] px-3.5 py-2.5 flex items-center gap-2.5"
      >
        <div className="w-7 h-7 rounded-full bg-[var(--color-lime)]/80 flex items-center justify-center text-[var(--color-charcoal)] text-[10px] font-bold shrink-0">
          {visible.name.charAt(0)}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[12px] sm:text-[13px] text-[var(--color-charcoal)] leading-tight">
            <strong className="font-semibold">{visible.name}</strong>{" "}
            <span className="text-[var(--color-charcoal)]/60">comprou</span>{" "}
            <strong className="text-[var(--color-terracotta)] font-semibold">
              {visible.qty} {visible.qty === 1 ? "nº" : "nºs"}
            </strong>
          </p>
          <p className="text-[10px] sm:text-[11px] text-[var(--color-charcoal)]/40 mt-px">
            {visible.city} · {visible.minutes}min
          </p>
        </div>
        <button
          onClick={() => setShow(false)}
          className="text-[var(--color-charcoal)]/25 hover:text-[var(--color-charcoal)]/60 transition-colors shrink-0 p-1"
          aria-label="Fechar notificações"
        >
          <svg viewBox="0 0 12 12" fill="none" className="w-3 h-3" aria-hidden="true">
            <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  );
}

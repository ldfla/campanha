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

  useEffect(() => {
    const initial: Purchase[] = Array.from({ length: 5 }, (_, i) => ({
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
    }, 3500);
    return () => clearInterval(interval);
  }, [purchases]);

  if (!visible) return null;

  return (
    <div className="fixed bottom-20 left-4 z-40 md:bottom-6 md:left-6 max-w-[320px]">
      <div
        key={visible.id + "-" + visible.name}
        className="animate-slide-in bg-white rounded-xl shadow-lg border border-black/5 px-4 py-3 flex items-start gap-3"
      >
        <div className="w-8 h-8 rounded-full bg-[var(--color-lime)] flex items-center justify-center text-[var(--color-charcoal)] text-xs font-bold shrink-0">
          {visible.name.charAt(0)}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-medium text-[var(--color-charcoal)] leading-tight">
            <strong>{visible.name}</strong> comprou{" "}
            <strong className="text-[var(--color-terracotta)]">
              {visible.qty} {visible.qty === 1 ? "número" : "números"}
            </strong>
          </p>
          <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">
            {visible.city} · há {visible.minutes} min
          </p>
        </div>
      </div>
    </div>
  );
}

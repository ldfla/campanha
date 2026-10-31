"use client";

import { useEffect, useState } from "react";

const TOTAL_NUMBERS = 100_000;

export function UrgencyBar() {
  const sold = 67_432;
  const remaining = TOTAL_NUMBERS - sold;
  const percentage = Math.round((sold / TOTAL_NUMBERS) * 100);

  const [displayPercentage, setDisplayPercentage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => setDisplayPercentage(percentage), 100);
    return () => clearTimeout(timer);
  }, [percentage]);

  return (
    <div className="bg-[var(--color-charcoal)] text-[var(--color-cream)] py-2.5 px-4">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-4 text-sm">
        <div className="flex items-center gap-2 shrink-0">
          <span className="inline-block w-2 h-2 rounded-full bg-[var(--color-lime)] animate-pulse-dot" />
          <span className="font-semibold text-[var(--color-lime)]">
            {remaining.toLocaleString("pt-BR")} restantes
          </span>
        </div>

        <div className="hidden sm:flex items-center gap-3 flex-1 max-w-xs">
          <div className="flex-1 h-2 bg-white/10 rounded-full overflow-hidden">
            <div
              className="h-full bg-[var(--color-lime)] rounded-full transition-all duration-1000 ease-out"
              style={{ width: `${displayPercentage}%` }}
            />
          </div>
          <span className="text-xs text-white/60 tabular-nums shrink-0">{percentage}% vendido</span>
        </div>

        <span className="text-white/50 text-xs hidden md:block">
          Sorteio 20/12/2026
        </span>
      </div>
    </div>
  );
}

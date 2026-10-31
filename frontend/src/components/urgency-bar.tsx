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
    <div className="bg-[var(--color-charcoal)] text-[var(--color-cream)] py-2 px-4">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-3 text-sm">
        <div className="flex items-center gap-2 shrink-0">
          <span className="inline-block w-2 h-2 rounded-full bg-[var(--color-lime)] animate-pulse-dot" />
          <span className="font-semibold text-[var(--color-lime)] text-xs sm:text-sm">
            {remaining.toLocaleString("pt-BR")} restantes
          </span>
        </div>

        {/* Progress bar — visível em todas as telas */}
        <div className="flex items-center gap-2 flex-1 max-w-[200px] sm:max-w-xs">
          <div className="flex-1 h-1.5 sm:h-2 bg-white/10 rounded-full overflow-hidden">
            <div
              className="h-full bg-[var(--color-lime)] rounded-full transition-all duration-1000 ease-out"
              style={{ width: `${displayPercentage}%` }}
            />
          </div>
          <span className="text-[10px] sm:text-xs text-white/50 tabular-nums shrink-0">{percentage}%</span>
        </div>

        <span className="text-white/40 text-[10px] sm:text-xs shrink-0">
          Sorteio 20/12
        </span>
      </div>
    </div>
  );
}

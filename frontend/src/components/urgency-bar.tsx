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
    <div className="border-b border-white/10 bg-[var(--color-forest)] px-5 py-2.5 text-white">
      <div className="mx-auto flex max-w-[1140px] items-center justify-between gap-3 text-sm">
        <div className="flex shrink-0 items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
          <span className="text-xs font-semibold tabular-nums text-[var(--color-green)] sm:text-sm">
            {remaining.toLocaleString("pt-BR")} restantes
          </span>
        </div>

        <div className="flex max-w-[200px] flex-1 items-center gap-2 sm:max-w-xs">
          <div
            className="h-1.5 flex-1 overflow-hidden rounded-full bg-[#26362f] sm:h-2"
            role="progressbar"
            aria-valuenow={percentage}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label="Números vendidos"
          >
            <div
              className="h-full rounded-full bg-[var(--color-green)] transition-all duration-1000 ease-out"
              style={{ width: `${displayPercentage}%` }}
            />
          </div>
          <span className="shrink-0 text-[10px] tabular-nums text-white/60 sm:text-xs">
            {percentage}%
          </span>
        </div>

        <span className="shrink-0 text-[10px] tabular-nums text-white/50 sm:text-xs">
          Sorteio 20/12
        </span>
      </div>
    </div>
  );
}

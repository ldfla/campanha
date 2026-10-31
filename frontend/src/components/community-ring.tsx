"use client";

import { useEffect, useState } from "react";

const TOTAL = 100_000;
const SOLD = 67_432;

export function CommunityRing() {
  const percentage = Math.round((SOLD / TOTAL) * 100);
  const remaining = TOTAL - SOLD;

  const radius = 80;
  const stroke = 10;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percentage / 100) * circumference;

  const [animated, setAnimated] = useState(false);
  const [displayCount, setDisplayCount] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => setAnimated(true), 200);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!animated) return;
    const duration = 1800;
    const start = performance.now();
    let frame: number;

    function tick(now: number) {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplayCount(Math.round(eased * SOLD));
      if (progress < 1) frame = requestAnimationFrame(tick);
    }

    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [animated]);

  return (
    <div className="animate-fade-in relative inline-flex items-center justify-center">
      <svg
        width="184"
        height="184"
        viewBox="0 0 184 184"
        className="-rotate-90 transform"
        role="img"
        aria-label={`Progresso: ${percentage}% dos números vendidos`}
      >
        <circle
          cx="92"
          cy="92"
          r={radius}
          fill="none"
          stroke="rgba(255, 255, 255, 0.12)"
          strokeWidth={stroke}
        />
        <circle
          cx="92"
          cy="92"
          r={radius}
          fill="none"
          stroke="var(--color-green)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={animated ? offset : circumference}
          className="transition-[stroke-dashoffset] duration-[2000ms] ease-[cubic-bezier(0.33,1,0.68,1)]"
        />
      </svg>

      <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
        <span className="font-[family-name:var(--font-heading)] text-4xl font-bold leading-none tabular-nums text-white md:text-5xl">
          {displayCount.toLocaleString("pt-BR")}
        </span>
        <span className="mt-1.5 text-xs text-white/60">participando</span>
        <span className="mt-2 flex items-center gap-1.5">
          <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
          <span className="text-[11px] font-medium tabular-nums text-[var(--color-green)]">
            {remaining.toLocaleString("pt-BR")} restantes
          </span>
        </span>
      </div>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";

export function SocialProof() {
  const target = 67_432;
  const [count, setCount] = useState(0);

  useEffect(() => {
    let frame: number;
    const duration = 1200;
    const start = performance.now();

    function tick(now: number) {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.round(eased * target));
      if (progress < 1) frame = requestAnimationFrame(tick);
    }

    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, []);

  return (
    <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 py-5 px-4 bg-[var(--color-cream-warm)] border-y border-[var(--color-charcoal)]/5">
      <div className="flex items-center gap-2">
        <div className="flex -space-x-2">
          {["A", "M", "C", "R", "J"].map((letter, i) => (
            <div
              key={letter}
              className="w-7 h-7 rounded-full border-2 border-[var(--color-cream-warm)] flex items-center justify-center text-[10px] font-bold text-white"
              style={{
                backgroundColor: ["#B5502F", "#2B6CB0", "#38A169", "#D69E2E", "#805AD5"][i],
                zIndex: 5 - i,
              }}
            >
              {letter}
            </div>
          ))}
        </div>
        <span className="text-sm text-[var(--color-charcoal)]">
          <strong className="tabular-nums">{count.toLocaleString("pt-BR")}</strong> pessoas participando
        </span>
      </div>
      <span className="text-[var(--color-charcoal)]/30 hidden sm:inline">|</span>
      <div className="flex items-center gap-1.5 text-sm text-[var(--color-charcoal)]/70">
        <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" aria-hidden="true">
          <path d="M8 1L10.2 5.5L15 6.2L11.5 9.6L12.3 14.4L8 12.1L3.7 14.4L4.5 9.6L1 6.2L5.8 5.5Z" fill="var(--color-terracotta)" />
        </svg>
        <span>Sorteio pela Loteria Federal</span>
      </div>
      <span className="text-[var(--color-charcoal)]/30 hidden sm:inline">|</span>
      <div className="flex items-center gap-1.5 text-sm text-[var(--color-charcoal)]/70">
        <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4" aria-hidden="true">
          <path d="M13.5 4.5L6 12L2.5 8.5" stroke="var(--color-terracotta)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span>Pagamento via Pix</span>
      </div>
    </div>
  );
}

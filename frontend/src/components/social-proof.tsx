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
    <div className="bg-[var(--color-cream-warm)] border-y border-[var(--color-charcoal)]/[0.06] py-4 px-4 overflow-hidden">
      <div className="max-w-5xl mx-auto flex flex-wrap items-center justify-center gap-x-5 gap-y-3">
        <div className="flex items-center gap-2.5">
          <div className="flex -space-x-1.5">
            {["A", "M", "C", "R", "J"].map((letter, i) => (
              <div
                key={letter}
                className="w-6 h-6 sm:w-7 sm:h-7 rounded-full border-[1.5px] border-[var(--color-cream-warm)] flex items-center justify-center text-[9px] sm:text-[10px] font-bold text-white"
                style={{
                  backgroundColor: ["#B5502F", "#2B6CB0", "#38A169", "#D69E2E", "#805AD5"][i],
                  zIndex: 5 - i,
                }}
              >
                {letter}
              </div>
            ))}
          </div>
          <span className="text-[13px] sm:text-sm text-[var(--color-charcoal)]">
            <strong className="tabular-nums font-bold">{count.toLocaleString("pt-BR")}</strong> participando
          </span>
        </div>

        <span className="w-px h-4 bg-[var(--color-charcoal)]/10 hidden sm:block" />

        <div className="flex items-center gap-1.5 text-[13px] sm:text-sm text-[var(--color-charcoal)]/65">
          <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5 shrink-0" aria-hidden="true">
            <path d="M8 1L10.2 5.5L15 6.2L11.5 9.6L12.3 14.4L8 12.1L3.7 14.4L4.5 9.6L1 6.2L5.8 5.5Z" fill="var(--color-terracotta)" opacity="0.7" />
          </svg>
          <span>Loteria Federal</span>
        </div>

        <span className="w-px h-4 bg-[var(--color-charcoal)]/10 hidden sm:block" />

        <div className="flex items-center gap-1.5 text-[13px] sm:text-sm text-[var(--color-charcoal)]/65">
          <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5 shrink-0" aria-hidden="true">
            <path d="M13.5 4.5L6 12L2.5 8.5" stroke="var(--color-terracotta)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
          </svg>
          <span>Pix</span>
        </div>

        <span className="w-px h-4 bg-[var(--color-charcoal)]/10 hidden sm:block" />

        <div className="flex items-center gap-1.5 text-[13px] sm:text-sm text-[var(--color-charcoal)]/65">
          <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5 shrink-0" aria-hidden="true">
            <rect x="2" y="3" width="12" height="10" rx="2" stroke="var(--color-terracotta)" strokeWidth="1.3" opacity="0.7" />
            <path d="M5 7.5h6M5 9.5h4" stroke="var(--color-terracotta)" strokeWidth="1" strokeLinecap="round" opacity="0.5" />
          </svg>
          <span>Auditável</span>
        </div>
      </div>
    </div>
  );
}

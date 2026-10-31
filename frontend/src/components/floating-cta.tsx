"use client";

import { useEffect, useState } from "react";

export function FloatingCTA() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    function onScroll() {
      setShow(window.scrollY > 500);
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (!show) return null;

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 border-t border-white/10 bg-[var(--color-forest)]/97 px-5 py-3 pb-safe backdrop-blur-md md:hidden">
      <div className="mx-auto flex max-w-[1140px] items-center justify-between gap-3">
        <div className="flex min-w-0 flex-col">
          <span className="text-[10px] leading-tight text-white/60">
            Total em prêmios
          </span>
          <span className="font-[family-name:var(--font-heading)] text-base font-bold leading-tight text-[var(--color-gold-metal)]">
            R$ 18.500
          </span>
        </div>
        <a
          href="/participar"
          className="flex shrink-0 items-center justify-center gap-2 rounded-xl bg-[var(--color-orange)] px-6 py-3.5 text-sm font-bold text-white transition-transform active:scale-[0.97]"
        >
          Participar
          <span className="rounded bg-white/25 px-1.5 py-0.5 text-[10px] font-extrabold tabular-nums">
            R$&nbsp;0,65
          </span>
        </a>
      </div>
    </div>
  );
}

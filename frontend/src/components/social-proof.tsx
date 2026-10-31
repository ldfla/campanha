"use client";

import { useEffect, useState } from "react";

export function SocialProof() {
  const target = 1284;
  const [count, setCount] = useState(0);

  useEffect(() => {
    let frame: number;
    const duration = 1800;
    const start = performance.now();

    function tick(now: number) {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.round(eased * target));

      if (progress < 1) {
        frame = requestAnimationFrame(tick);
      }
    }

    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, []);

  return (
    <section className="bg-[var(--color-charcoal)] px-6 md:px-16 lg:px-24 xl:px-32 py-14 md:py-16">
      <div className="max-w-5xl mx-auto flex flex-col md:flex-row items-center justify-center gap-4 md:gap-8">
        <div className="flex items-baseline gap-3">
          <span className="font-[family-name:var(--font-heading)] text-[clamp(3rem,8vw,5.5rem)] font-bold text-[var(--color-lime)] tabular-nums leading-none">
            {count.toLocaleString("pt-BR")}
          </span>
          <span className="text-[var(--color-lime)] text-2xl md:text-3xl font-light opacity-80">+</span>
        </div>
        <p className="text-[var(--color-cream)] opacity-80 text-lg md:text-xl text-center md:text-left leading-snug max-w-xs">
          pessoas já garantiram
          <br />
          seus números da sorte
        </p>
      </div>
    </section>
  );
}

"use client";

import { useEffect, useState } from "react";

export function SocialProof() {
  const target = 1284;
  const [count, setCount] = useState(0);

  useEffect(() => {
    let frame: number;
    const duration = 1500;
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
    <section className="px-6 md:px-16 lg:px-24 xl:px-32 py-16 border-t border-[var(--color-charcoal)]/8 bg-[var(--color-cream)]">
      <p className="max-w-3xl font-[family-name:var(--font-heading)] text-[clamp(1.25rem,3vw,2rem)] leading-snug text-[var(--color-charcoal)]">
        Já são{" "}
        <span className="tabular-nums font-semibold">
          {count.toLocaleString("pt-BR")}
        </span>{" "}
        pessoas apostando no meu sonho. E contando.
      </p>
    </section>
  );
}

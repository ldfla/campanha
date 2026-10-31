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

  const avatars = [
    { letter: "A", bg: "#FF6347" },
    { letter: "M", bg: "#7C5CFC" },
    { letter: "C", bg: "#10B981" },
    { letter: "R", bg: "#FFB800" },
    { letter: "J", bg: "#FF3B7A" },
  ];

  return (
    <div className="bg-white border-y border-black/[0.04] py-5 px-4">
      <div className="max-w-5xl mx-auto flex flex-wrap items-center justify-center gap-x-6 gap-y-3">
        <div className="flex items-center gap-3">
          <div className="flex -space-x-2">
            {avatars.map((a, i) => (
              <div
                key={a.letter}
                className="w-7 h-7 sm:w-8 sm:h-8 rounded-full border-2 border-white flex items-center justify-center text-[10px] font-bold text-white shadow-sm"
                style={{ backgroundColor: a.bg, zIndex: 5 - i }}
              >
                {a.letter}
              </div>
            ))}
          </div>
          <span className="text-sm text-[var(--color-midnight)]">
            <strong className="tabular-nums font-bold">{count.toLocaleString("pt-BR")}</strong> participando
          </span>
        </div>

        <div className="hidden sm:flex items-center gap-5 text-sm text-[var(--color-midnight)]/50">
          <span className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-jade)]" />
            Loteria Federal
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-iris)]" />
            Pix
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-gold)]" />
            Auditável
          </span>
        </div>
      </div>
    </div>
  );
}

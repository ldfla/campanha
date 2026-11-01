"use client";

import { useState, useCallback, useRef } from "react";

export function RandomNumberDemo() {
  const [digits, setDigits] = useState<number[]>([0, 0, 0, 0, 0]);
  const [rolling, setRolling] = useState(false);
  const [hasRolled, setHasRolled] = useState(false);
  const intervalRefs = useRef<ReturnType<typeof setInterval>[]>([]);

  const generate = useCallback(() => {
    if (rolling) return;
    setRolling(true);
    setHasRolled(true);

    for (const ref of intervalRefs.current) clearInterval(ref);
    intervalRefs.current = [];

    const finalDigits = Array.from(
      { length: 5 },
      () => Math.floor(Math.random() * 10),
    );

    for (let i = 0; i < 5; i++) {
      const stopDelay = 400 + i * 220;

      const id = setInterval(() => {
        setDigits((prev) => {
          const next = [...prev];
          next[i] = Math.floor(Math.random() * 10);
          return next;
        });
      }, 60);
      intervalRefs.current.push(id);

      setTimeout(() => {
        clearInterval(id);
        setDigits((prev) => {
          const next = [...prev];
          const d = finalDigits[i];
          next[i] = d !== undefined ? d : 0;
          return next;
        });

        if (i === 4) {
          setRolling(false);
        }
      }, stopDelay);
    }
  }, [rolling]);

  return (
    <section className="section py-10 md:py-14">
      <div className="section-inner">
        <div className="mx-auto max-w-lg rounded-2xl border border-black/[0.06] bg-white p-6 md:p-8">
          <div className="mb-5 text-center">
            <h3 className="font-[family-name:var(--font-heading)] text-lg font-bold text-[var(--color-ink)] md:text-xl">
              Veja como funciona
            </h3>
            <p className="mt-1 text-sm text-[var(--color-ink-soft)]">
              Cada participante recebe números 100% aleatórios, gerados por algoritmo criptográfico seguro
            </p>
          </div>

          {/* Slot-style digits */}
          <div className="flex items-center justify-center gap-2 md:gap-3">
            {digits.map((d, i) => (
              <div
                key={i}
                className={`flex h-16 w-12 items-center justify-center rounded-xl border-2 text-2xl font-bold tabular-nums transition-all duration-150 md:h-20 md:w-16 md:text-3xl ${
                  rolling
                    ? "border-[var(--color-iris)]/30 bg-[var(--color-iris)]/[0.06] text-[var(--color-iris)]"
                    : hasRolled
                    ? "border-[var(--color-jade)]/30 bg-[var(--color-jade)]/[0.06] text-[var(--color-jade)]"
                    : "border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-ink)]/30"
                }`}
              >
                <span
                  className={rolling ? "animate-pulse" : ""}
                  style={{ fontVariantNumeric: "tabular-nums" }}
                >
                  {d}
                </span>
              </div>
            ))}
          </div>

          {/* Generate button */}
          <div className="mt-5 flex flex-col items-center gap-3">
            <button
              onClick={generate}
              disabled={rolling}
              className={`inline-flex items-center gap-2 rounded-xl px-6 py-3 text-sm font-semibold transition-all ${
                rolling
                  ? "cursor-wait bg-[var(--color-iris)]/10 text-[var(--color-iris)]/60"
                  : "bg-[var(--color-forest)] text-white hover:bg-[var(--color-forest-light)] active:scale-[0.97]"
              }`}
            >
              <svg viewBox="0 0 20 20" fill="none" className={`h-4 w-4 ${rolling ? "animate-spin" : ""}`} aria-hidden="true">
                <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              {rolling ? "Gerando..." : hasRolled ? "Gerar outro número" : "Gerar número aleatório"}
            </button>

            {hasRolled && !rolling && (
              <p className="text-center text-xs leading-relaxed text-[var(--color-ink-soft)]/60">
                Número <span className="font-bold tabular-nums text-[var(--color-ink)]">{digits.map(String).join("")}</span> gerado
                aleatoriamente. Clique de novo — cada vez é diferente.
              </p>
            )}
          </div>

          {/* Trust signals */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-x-5 gap-y-2 border-t border-black/[0.04] pt-5">
            <span className="flex items-center gap-1.5 text-xs text-[var(--color-ink-soft)]">
              <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5 text-[var(--color-jade)]" aria-hidden="true">
                <path d="M8 1.333A6.667 6.667 0 1014.667 8 6.674 6.674 0 008 1.333zm3.06 5.394L7.727 10.06a.667.667 0 01-.944 0L5.116 8.393a.667.667 0 11.944-.943l1.195 1.195L10.116 5.784a.667.667 0 11.944.943z" fill="currentColor" />
              </svg>
              SecureRandom
            </span>
            <span className="flex items-center gap-1.5 text-xs text-[var(--color-ink-soft)]">
              <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5 text-[var(--color-jade)]" aria-hidden="true">
                <path d="M8 1.333A6.667 6.667 0 1014.667 8 6.674 6.674 0 008 1.333zm3.06 5.394L7.727 10.06a.667.667 0 01-.944 0L5.116 8.393a.667.667 0 11.944-.943l1.195 1.195L10.116 5.784a.667.667 0 11.944.943z" fill="currentColor" />
              </svg>
              Distribuição equitativa
            </span>
            <span className="flex items-center gap-1.5 text-xs text-[var(--color-ink-soft)]">
              <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5 text-[var(--color-jade)]" aria-hidden="true">
                <path d="M8 1.333A6.667 6.667 0 1014.667 8 6.674 6.674 0 008 1.333zm3.06 5.394L7.727 10.06a.667.667 0 01-.944 0L5.116 8.393a.667.667 0 11.944-.943l1.195 1.195L10.116 5.784a.667.667 0 11.944.943z" fill="currentColor" />
              </svg>
              Sem escolha manual
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}

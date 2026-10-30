const prizes = [
  {
    name: "Kit Canon D70",
    desire: "A câmera que conta histórias profissionais.",
  },
  {
    name: "MacBook Air M1",
    desire: "O computador que cabe na bolsa e roda o mundo.",
  },
  {
    name: "Kit Canon T1i Rebel",
    desire: "Seu primeiro passo na fotografia de verdade.",
  },
  {
    name: "R$ 1.000",
    desire: "Pra fazer o que quiser. Sem regras.",
  },
  {
    name: "R$ 500",
    desire: "Um extra que chega na hora certa.",
  },
] as const;

export function Prizes() {
  return (
    <section className="bg-[var(--color-charcoal)] px-6 md:px-16 lg:px-24 py-24 md:py-32">
      <div className="max-w-4xl">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-semibold text-[var(--color-cream)] mb-16">
          O que você pode levar pra casa
        </h2>

        <div className="flex flex-col gap-10 md:gap-6">
          {prizes.map((prize) => (
            <div key={prize.name} className="border-b border-[var(--color-cream)]/10 pb-6 md:pb-4">
              <div className="flex flex-col md:flex-row md:items-baseline md:gap-6">
                <span className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-semibold text-[var(--color-cream)] shrink-0">
                  {prize.name}
                </span>
                <span className="text-[var(--color-cream)] opacity-30 hidden md:inline">
                  &mdash;
                </span>
                <span className="text-base text-[var(--color-cream)] opacity-60 mt-1 md:mt-0">
                  {prize.desire}
                </span>
              </div>
            </div>
          ))}
        </div>

        <p className="mt-12 text-sm text-[var(--color-cream)] opacity-40">
          + 10 prêmios-surpresa de R$ 100 para números aleatórios.
        </p>
      </div>
    </section>
  );
}

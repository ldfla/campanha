const prizes = [
  {
    name: "Kit Canon D70",
    value: "R$ 10.000",
    desire: "A câmera que conta histórias profissionais.",
    gradient: "from-[#3a2218] via-[#5a3828] to-[#2B1D16]",
    icon: (
      <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-16 h-16 md:w-20 md:h-20" aria-hidden="true">
        <rect x="12" y="22" width="56" height="36" rx="6" stroke="var(--color-cream)" strokeWidth="2" opacity="0.6" />
        <circle cx="44" cy="40" r="12" stroke="var(--color-cream)" strokeWidth="2" opacity="0.8" />
        <circle cx="44" cy="40" r="7" stroke="var(--color-lime)" strokeWidth="1.5" />
        <rect x="16" y="27" width="8" height="5" rx="1.5" stroke="var(--color-cream)" strokeWidth="1.5" opacity="0.4" />
        <circle cx="24" cy="40" r="2" fill="var(--color-cream)" opacity="0.3" />
      </svg>
    ),
    tag: "1º Prêmio",
  },
  {
    name: "MacBook Air M1",
    value: "R$ 4.000",
    desire: "O computador que cabe na bolsa e roda o mundo.",
    gradient: "from-[#2a2a2e] via-[#3d3d42] to-[#1a1a1e]",
    icon: (
      <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-16 h-16 md:w-20 md:h-20" aria-hidden="true">
        <rect x="14" y="18" width="52" height="34" rx="4" stroke="var(--color-cream)" strokeWidth="2" opacity="0.6" />
        <rect x="18" y="22" width="44" height="26" rx="2" stroke="var(--color-lime)" strokeWidth="1" opacity="0.4" />
        <path d="M8 56 H72" stroke="var(--color-cream)" strokeWidth="2" opacity="0.6" />
        <path d="M28 56 L24 62 H56 L52 56" stroke="var(--color-cream)" strokeWidth="1.5" opacity="0.4" />
      </svg>
    ),
    tag: "2º Prêmio",
  },
  {
    name: "Kit Canon T1i Rebel",
    value: "R$ 2.000",
    desire: "Seu primeiro passo na fotografia de verdade.",
    gradient: "from-[#3a2218] via-[#4a2c1e] to-[#2B1D16]",
    icon: (
      <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-16 h-16 md:w-20 md:h-20" aria-hidden="true">
        <rect x="15" y="24" width="50" height="32" rx="5" stroke="var(--color-cream)" strokeWidth="2" opacity="0.6" />
        <circle cx="42" cy="40" r="10" stroke="var(--color-cream)" strokeWidth="2" opacity="0.8" />
        <circle cx="42" cy="40" r="5.5" stroke="var(--color-lime)" strokeWidth="1.5" />
        <rect x="18" y="28" width="7" height="4" rx="1" stroke="var(--color-cream)" strokeWidth="1.5" opacity="0.4" />
      </svg>
    ),
    tag: "3º Prêmio",
  },
  {
    name: "R$ 1.000",
    value: "R$ 1.000",
    desire: "Pra fazer o que quiser. Sem regras.",
    gradient: "from-[var(--color-terracotta-dark)] via-[var(--color-terracotta)] to-[#944025]",
    icon: (
      <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-16 h-16 md:w-20 md:h-20" aria-hidden="true">
        <circle cx="40" cy="40" r="22" stroke="var(--color-cream)" strokeWidth="2" opacity="0.6" />
        <text x="40" y="46" textAnchor="middle" fontSize="20" fontWeight="700" fill="var(--color-cream)" fontFamily="system-ui" opacity="0.8">R$</text>
      </svg>
    ),
    tag: "4º Prêmio",
  },
  {
    name: "R$ 500",
    value: "R$ 500",
    desire: "Um extra que chega na hora certa.",
    gradient: "from-[var(--color-terracotta-dark)] via-[var(--color-terracotta)] to-[#7a3520]",
    icon: (
      <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-16 h-16 md:w-20 md:h-20" aria-hidden="true">
        <circle cx="40" cy="40" r="22" stroke="var(--color-cream)" strokeWidth="2" opacity="0.5" />
        <text x="40" y="46" textAnchor="middle" fontSize="20" fontWeight="700" fill="var(--color-cream)" fontFamily="system-ui" opacity="0.7">R$</text>
      </svg>
    ),
    tag: "5º Prêmio",
  },
] as const;

export function Prizes() {
  return (
    <section className="bg-[var(--color-charcoal)] px-6 md:px-16 lg:px-24 xl:px-32 py-24 md:py-32">
      <div className="max-w-6xl mx-auto">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-semibold text-[var(--color-cream)] mb-4 text-center">
          O que você pode levar pra casa
        </h2>
        <p className="text-center text-[var(--color-cream)] opacity-50 text-base md:text-lg mb-16 md:mb-20 max-w-lg mx-auto">
          Resultado pela Loteria Federal de 20/12/2026.
          <br />
          Transparente, auditável e aberto.
        </p>

        {/* Top 3 prizes — larger cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 md:gap-6 mb-5 md:mb-6">
          {prizes.slice(0, 3).map((prize) => (
            <article
              key={prize.name}
              className={`relative bg-gradient-to-br ${prize.gradient} rounded-2xl overflow-hidden group`}
            >
              {/* Tag */}
              <div className="absolute top-4 left-4 z-10">
                <span className="inline-block bg-[var(--color-lime)] text-[var(--color-charcoal)] text-xs font-bold uppercase tracking-wider px-3 py-1 rounded-full">
                  {prize.tag}
                </span>
              </div>

              {/* Icon area */}
              <div className="flex items-center justify-center pt-14 pb-6 md:pt-16 md:pb-8">
                {prize.icon}
              </div>

              {/* Content */}
              <div className="px-6 pb-6 md:pb-8">
                <h3 className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-semibold text-[var(--color-cream)] mb-1">
                  {prize.name}
                </h3>
                <p className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-lime)] mb-3">
                  {prize.value}
                </p>
                <p className="text-sm md:text-base text-[var(--color-cream)] opacity-60 leading-relaxed">
                  {prize.desire}
                </p>
              </div>
            </article>
          ))}
        </div>

        {/* 4th and 5th prizes — smaller, side by side */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5 md:gap-6">
          {prizes.slice(3).map((prize) => (
            <article
              key={prize.name}
              className={`relative bg-gradient-to-br ${prize.gradient} rounded-2xl overflow-hidden flex items-center gap-5 md:gap-6 px-6 py-6 md:py-8`}
            >
              <div className="absolute top-4 right-4">
                <span className="inline-block bg-[var(--color-cream)]/15 text-[var(--color-cream)] text-xs font-bold uppercase tracking-wider px-3 py-1 rounded-full">
                  {prize.tag}
                </span>
              </div>
              <div className="shrink-0">
                {prize.icon}
              </div>
              <div>
                <h3 className="font-[family-name:var(--font-heading)] text-lg md:text-xl font-semibold text-[var(--color-cream)] mb-1">
                  {prize.name}
                </h3>
                <p className="text-sm text-[var(--color-cream)] opacity-60">
                  {prize.desire}
                </p>
              </div>
            </article>
          ))}
        </div>

        {/* Surprise prizes */}
        <div className="mt-10 md:mt-14 text-center">
          <div className="inline-flex items-center gap-3 bg-[var(--color-cream)]/5 border border-[var(--color-cream)]/10 rounded-full px-6 py-3">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" aria-hidden="true">
              <path d="M12 2L15 8.5L22 9.5L17 14.5L18 21.5L12 18L6 21.5L7 14.5L2 9.5L9 8.5Z" stroke="var(--color-lime)" strokeWidth="1.5" strokeLinejoin="round" />
            </svg>
            <span className="text-sm md:text-base text-[var(--color-cream)] opacity-70">
              <strong className="text-[var(--color-lime)] font-semibold opacity-100">+ 10 prêmios-surpresa</strong>{" "}
              de R$ 100 para números aleatórios
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}

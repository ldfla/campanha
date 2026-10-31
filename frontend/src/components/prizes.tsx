import { CTAButton } from "@/components/cta-button";

const prizes = [
  {
    tag: "1º Prêmio",
    name: "Kit Fotográfico Canon",
    items: "Canon D70 + Lente 24-70 f/4 + Flash 430EX",
    value: "R$ 10.000",
    emoji: "📸",
    highlight: true,
  },
  {
    tag: "2º Prêmio",
    name: "MacBook Air M1",
    items: '13" · 8GB · Chip Apple Silicon',
    value: "R$ 4.000",
    emoji: "💻",
    highlight: true,
  },
  {
    tag: "3º Prêmio",
    name: "Kit Canon T1i Rebel",
    items: "Câmera + acessórios para começar",
    value: "R$ 2.000",
    emoji: "📷",
    highlight: true,
  },
  {
    tag: "4º Prêmio",
    name: "Prêmio em dinheiro",
    items: "Pix direto na sua conta",
    value: "R$ 1.000",
    emoji: "💰",
    highlight: false,
  },
  {
    tag: "5º Prêmio",
    name: "Prêmio em dinheiro",
    items: "Pix direto na sua conta",
    value: "R$ 500",
    emoji: "💵",
    highlight: false,
  },
] as const;

export function Prizes() {
  return (
    <section id="premios" className="px-4 md:px-8 lg:px-16 py-16 md:py-24 bg-[var(--color-cream)]">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-10 md:mb-14">
          <span className="inline-block bg-[var(--color-terracotta)] text-white text-[11px] font-bold uppercase tracking-widest px-3.5 py-1.5 rounded-full mb-4">
            5 prêmios + 10 surpresas
          </span>
          <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-charcoal)]">
            O que você pode ganhar
          </h2>
        </div>

        <div className="space-y-2.5 md:space-y-3">
          {prizes.map((prize) => (
            <article
              key={prize.tag}
              className={`group flex items-center gap-3.5 sm:gap-5 md:gap-6 rounded-xl sm:rounded-2xl px-4 py-4 sm:px-6 sm:py-5 md:px-8 md:py-6 transition-all duration-150 ${
                prize.highlight
                  ? "bg-white shadow-[0_2px_12px_rgba(0,0,0,0.04)] hover:shadow-[0_4px_20px_rgba(0,0,0,0.08)] border border-black/[0.04]"
                  : "bg-[var(--color-cream-warm)] hover:bg-white border border-transparent hover:border-black/[0.04] hover:shadow-sm"
              }`}
            >
              <div className="w-11 h-11 sm:w-14 sm:h-14 rounded-xl bg-[var(--color-charcoal)]/[0.04] flex items-center justify-center shrink-0">
                <span className="text-2xl sm:text-3xl" aria-hidden="true">{prize.emoji}</span>
              </div>

              <div className="flex-1 min-w-0">
                <span className={`text-[10px] sm:text-xs font-bold uppercase tracking-wider ${
                  prize.highlight ? "text-[var(--color-terracotta)]" : "text-[var(--color-charcoal)]/40"
                }`}>
                  {prize.tag}
                </span>
                <h3 className="font-[family-name:var(--font-heading)] text-sm sm:text-base md:text-lg font-semibold text-[var(--color-charcoal)] leading-tight">
                  {prize.name}
                </h3>
                <p className="text-xs sm:text-sm text-[var(--color-charcoal)]/50 mt-0.5 hidden sm:block">
                  {prize.items}
                </p>
              </div>

              <div className="shrink-0 text-right">
                <span className={`font-[family-name:var(--font-heading)] font-bold tabular-nums ${
                  prize.highlight
                    ? "text-lg sm:text-xl md:text-2xl text-[var(--color-terracotta)]"
                    : "text-base sm:text-lg md:text-xl text-[var(--color-charcoal)]/80"
                }`}>
                  {prize.value}
                </span>
              </div>
            </article>
          ))}

          {/* Prêmios-surpresa */}
          <div className="flex items-center gap-3.5 sm:gap-5 md:gap-6 rounded-xl sm:rounded-2xl px-4 py-4 sm:px-6 sm:py-5 md:px-8 md:py-5 bg-[var(--color-lime)]/[0.08] border border-[var(--color-lime)]/15">
            <div className="w-11 h-11 sm:w-14 sm:h-14 rounded-xl bg-[var(--color-lime)]/15 flex items-center justify-center shrink-0">
              <span className="text-2xl sm:text-3xl" aria-hidden="true">🎁</span>
            </div>
            <div className="flex-1">
              <h3 className="font-[family-name:var(--font-heading)] text-sm sm:text-base md:text-lg font-semibold text-[var(--color-charcoal)]">
                + 10 prêmios-surpresa
              </h3>
              <p className="text-xs sm:text-sm text-[var(--color-charcoal)]/50">
                R$ 100 cada · Números aleatórios
              </p>
            </div>
            <div className="shrink-0">
              <span className="font-[family-name:var(--font-heading)] font-bold text-base sm:text-lg md:text-xl text-[var(--color-charcoal)]/80">
                R$ 1.000
              </span>
            </div>
          </div>
        </div>

        {/* Total */}
        <div className="mt-5 p-4 md:p-5 rounded-xl bg-[var(--color-charcoal)] flex items-center justify-center gap-3">
          <span className="text-[var(--color-cream)]/60 text-sm">Total em prêmios:</span>
          <span className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-lime)]">
            R$ 18.500
          </span>
        </div>

        <div className="mt-10 flex flex-col items-center gap-3">
          <CTAButton showPrice />
          <p className="text-xs sm:text-sm text-[var(--color-charcoal)]/45">
            Quanto mais números, mais chances de ganhar
          </p>
        </div>
      </div>
    </section>
  );
}

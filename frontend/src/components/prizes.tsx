import { CTAButton } from "@/components/cta-button";

const prizes = [
  {
    tag: "1º Prêmio",
    name: "Kit Fotográfico Canon",
    items: "Canon D70 + Lente 24-70 f/4 + Flash 430EX",
    value: "R$ 10.000",
    valueCents: 1_000_000,
    emoji: "📸",
    highlight: true,
  },
  {
    tag: "2º Prêmio",
    name: "MacBook Air M1",
    items: '13" · 8GB · Chip Apple Silicon',
    value: "R$ 4.000",
    valueCents: 400_000,
    emoji: "💻",
    highlight: true,
  },
  {
    tag: "3º Prêmio",
    name: "Kit Canon T1i Rebel",
    items: "Câmera + acessórios para começar",
    value: "R$ 2.000",
    valueCents: 200_000,
    emoji: "📷",
    highlight: true,
  },
  {
    tag: "4º Prêmio",
    name: "Prêmio em dinheiro",
    items: "Pix direto na sua conta",
    value: "R$ 1.000",
    valueCents: 100_000,
    emoji: "💰",
    highlight: false,
  },
  {
    tag: "5º Prêmio",
    name: "Prêmio em dinheiro",
    items: "Pix direto na sua conta",
    value: "R$ 500",
    valueCents: 50_000,
    emoji: "💵",
    highlight: false,
  },
] as const;

export function Prizes() {
  return (
    <section id="premios" className="px-4 md:px-8 lg:px-16 py-16 md:py-24 bg-[var(--color-cream)]">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-10 md:mb-14">
          <span className="inline-block bg-[var(--color-terracotta)] text-white text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full mb-4">
            5 prêmios + 10 surpresas
          </span>
          <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-charcoal)]">
            O que você pode ganhar
          </h2>
        </div>

        <div className="grid gap-3 md:gap-4">
          {prizes.map((prize) => (
            <article
              key={prize.tag}
              className={`flex items-center gap-4 md:gap-6 rounded-2xl px-5 py-5 md:px-8 md:py-6 transition-shadow ${
                prize.highlight
                  ? "bg-white shadow-md hover:shadow-lg border border-black/5"
                  : "bg-[var(--color-cream-warm)] hover:bg-white border border-transparent hover:border-black/5"
              }`}
            >
              <div className="text-3xl md:text-4xl shrink-0" aria-hidden="true">
                {prize.emoji}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5">
                  <span className={`text-xs font-bold uppercase tracking-wider ${
                    prize.highlight ? "text-[var(--color-terracotta)]" : "text-[var(--color-charcoal)]/50"
                  }`}>
                    {prize.tag}
                  </span>
                </div>
                <h3 className="font-[family-name:var(--font-heading)] text-base md:text-lg font-semibold text-[var(--color-charcoal)] leading-tight">
                  {prize.name}
                </h3>
                <p className="text-sm text-[var(--color-charcoal)]/60 mt-0.5 hidden sm:block">
                  {prize.items}
                </p>
              </div>

              <div className="shrink-0 text-right">
                <span className={`font-[family-name:var(--font-heading)] font-bold tabular-nums ${
                  prize.highlight
                    ? "text-xl md:text-2xl text-[var(--color-terracotta)]"
                    : "text-lg md:text-xl text-[var(--color-charcoal)]"
                }`}>
                  {prize.value}
                </span>
              </div>
            </article>
          ))}

          <div className="flex items-center gap-4 md:gap-6 rounded-2xl px-5 py-4 md:px-8 md:py-5 bg-[var(--color-lime)]/10 border border-[var(--color-lime)]/20">
            <div className="text-2xl md:text-3xl shrink-0" aria-hidden="true">🎁</div>
            <div className="flex-1">
              <h3 className="font-[family-name:var(--font-heading)] text-base md:text-lg font-semibold text-[var(--color-charcoal)]">
                + 10 prêmios-surpresa
              </h3>
              <p className="text-sm text-[var(--color-charcoal)]/60">
                R$ 100 cada · Números sorteados aleatoriamente
              </p>
            </div>
            <div className="shrink-0">
              <span className="font-[family-name:var(--font-heading)] font-bold text-lg md:text-xl text-[var(--color-charcoal)]">
                R$ 1.000
              </span>
            </div>
          </div>
        </div>

        <div className="mt-6 p-4 md:p-6 rounded-xl bg-[var(--color-charcoal)] text-center">
          <div className="flex items-center justify-center gap-3 flex-wrap">
            <span className="text-[var(--color-cream)]/70 text-sm md:text-base">Total em prêmios:</span>
            <span className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-lime)]">
              R$ 18.500
            </span>
          </div>
        </div>

        <div className="mt-10 flex flex-col items-center gap-3">
          <CTAButton showPrice />
          <p className="text-sm text-[var(--color-charcoal)]/50">
            Quanto mais números, mais chances de ganhar
          </p>
        </div>
      </div>
    </section>
  );
}

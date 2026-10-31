const steps = [
  {
    number: "1",
    title: "Escolha quantos números quer",
    text: "A partir de R$ 0,56 por número. Sem limite.",
    icon: "🎯",
    color: "bg-[var(--color-terracotta)]/8",
  },
  {
    number: "2",
    title: "Pague via Pix",
    text: "Aprovação instantânea. Sem cartão, sem burocracia.",
    icon: "⚡",
    color: "bg-[var(--color-lime)]/15",
  },
  {
    number: "3",
    title: "Concorra automaticamente",
    text: "Resultado pela Loteria Federal em 20/12/2026.",
    icon: "🏆",
    color: "bg-[var(--color-terracotta)]/8",
  },
] as const;

export function HowItWorks() {
  return (
    <section className="px-4 md:px-8 lg:px-16 py-16 md:py-24 bg-white">
      <div className="max-w-4xl mx-auto">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.5rem,3.5vw,2.5rem)] font-bold text-[var(--color-charcoal)] text-center mb-4">
          Simples como comprar pão
        </h2>
        <p className="text-center text-sm sm:text-base text-[var(--color-charcoal)]/50 mb-12 md:mb-16 max-w-md mx-auto">
          Três passos e você já está concorrendo
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-6 lg:gap-10">
          {steps.map((step) => (
            <div key={step.number} className="flex flex-col items-center text-center">
              <div className={`w-20 h-20 md:w-24 md:h-24 rounded-2xl ${step.color} flex items-center justify-center mb-5 relative`}>
                <span className="text-3xl md:text-4xl" aria-hidden="true">{step.icon}</span>
                <span className="absolute -top-2 -right-2 w-7 h-7 rounded-full bg-[var(--color-terracotta)] text-white text-xs font-bold flex items-center justify-center shadow-sm">
                  {step.number}
                </span>
              </div>
              <h3 className="font-[family-name:var(--font-heading)] text-base md:text-lg font-semibold text-[var(--color-charcoal)] mb-2 leading-snug">
                {step.title}
              </h3>
              <p className="text-sm text-[var(--color-charcoal)]/55 leading-relaxed max-w-[220px] mx-auto">
                {step.text}
              </p>
            </div>
          ))}
        </div>

        {/* Connector dots on desktop */}
        <div className="hidden md:flex justify-center mt-10 gap-2">
          {[0, 1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className={`rounded-full ${i % 2 === 0 ? "w-2 h-2 bg-[var(--color-terracotta)]/20" : "w-1.5 h-1.5 bg-[var(--color-terracotta)]/10"}`}
            />
          ))}
        </div>
      </div>
    </section>
  );
}

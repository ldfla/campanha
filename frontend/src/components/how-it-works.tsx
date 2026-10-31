const steps = [
  {
    number: "1",
    title: "Escolha quantos números quer",
    text: "A partir de R$ 0,56 por número. Sem limite.",
    icon: "🎯",
  },
  {
    number: "2",
    title: "Pague via Pix",
    text: "Aprovação instantânea. Sem cartão, sem cadastro longo.",
    icon: "⚡",
  },
  {
    number: "3",
    title: "Concorra automaticamente",
    text: "Resultado pela Loteria Federal em 20/12/2026.",
    icon: "🏆",
  },
] as const;

export function HowItWorks() {
  return (
    <section className="px-4 md:px-8 lg:px-16 py-16 md:py-24 bg-white">
      <div className="max-w-4xl mx-auto">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.5rem,3.5vw,2.5rem)] font-bold text-[var(--color-charcoal)] text-center mb-12 md:mb-16">
          Simples como comprar pão
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
          {steps.map((step) => (
            <div key={step.number} className="text-center">
              <div className="text-4xl md:text-5xl mb-4" aria-hidden="true">{step.icon}</div>
              <div className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-[var(--color-terracotta)] text-white text-sm font-bold mb-3">
                {step.number}
              </div>
              <h3 className="font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-charcoal)] mb-2">
                {step.title}
              </h3>
              <p className="text-sm text-[var(--color-charcoal)]/60 leading-relaxed max-w-[240px] mx-auto">
                {step.text}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

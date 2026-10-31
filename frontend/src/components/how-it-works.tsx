import { CTAButton } from "@/components/cta-button";

const steps = [
  {
    number: "01",
    title: "Escolha seu número",
    text: "Um, dez, cem. Quanto mais números, mais chances de levar um prêmio pra casa.",
    icon: (
      <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-10 h-10 md:w-12 md:h-12" aria-hidden="true">
        <rect x="6" y="6" width="36" height="36" rx="10" stroke="var(--color-terracotta)" strokeWidth="2.5" />
        <text x="24" y="30" textAnchor="middle" fontSize="18" fontWeight="700" fill="var(--color-terracotta)" fontFamily="system-ui">#</text>
      </svg>
    ),
  },
  {
    number: "02",
    title: "Pague via Pix",
    text: "Rápido e seguro. Seu número é confirmado na hora, sem burocracia.",
    icon: (
      <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-10 h-10 md:w-12 md:h-12" aria-hidden="true">
        <path d="M24 6L38 24L24 42L10 24Z" stroke="var(--color-terracotta)" strokeWidth="2.5" strokeLinejoin="round" />
        <circle cx="24" cy="24" r="5" stroke="var(--color-terracotta)" strokeWidth="2" />
      </svg>
    ),
  },
  {
    number: "03",
    title: "Concorra aos prêmios",
    text: "Resultado pela Loteria Federal de 20/12/2026. Transparente e auditável.",
    icon: (
      <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-10 h-10 md:w-12 md:h-12" aria-hidden="true">
        <path d="M24 4L28.5 17.5H42.5L31 26L35.5 40L24 31L12.5 40L17 26L5.5 17.5H19.5Z" stroke="var(--color-terracotta)" strokeWidth="2.5" strokeLinejoin="round" />
      </svg>
    ),
  },
] as const;

export function HowItWorks() {
  return (
    <section className="px-6 md:px-16 lg:px-24 xl:px-32 py-24 md:py-32 bg-[var(--color-cream)]">
      <div className="max-w-5xl mx-auto">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-semibold text-[var(--color-charcoal)] mb-16 md:mb-20 text-center">
          Como funciona
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-12 md:gap-8 lg:gap-12">
          {steps.map((step) => (
            <div key={step.number} className="flex flex-col items-center text-center md:items-start md:text-left">
              <div className="w-20 h-20 md:w-24 md:h-24 rounded-2xl bg-[var(--color-terracotta)]/8 flex items-center justify-center mb-6">
                {step.icon}
              </div>
              <span className="font-[family-name:var(--font-heading)] text-sm font-medium text-[var(--color-terracotta)] tracking-widest uppercase mb-2">
                Passo {step.number}
              </span>
              <h3 className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-semibold text-[var(--color-charcoal)] mb-3">
                {step.title}
              </h3>
              <p className="text-base md:text-lg leading-relaxed text-[var(--color-charcoal)] opacity-70 max-w-xs">
                {step.text}
              </p>
            </div>
          ))}
        </div>

        <div className="mt-16 md:mt-20 flex flex-col items-center gap-4">
          <CTAButton size="md" />
          <p className="text-sm text-[var(--color-charcoal)] opacity-50">
            Sem cadastro complicado. 1 minuto e você está dentro.
          </p>
        </div>
      </div>
    </section>
  );
}

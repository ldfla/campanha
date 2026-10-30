import { CTAButton } from "@/components/cta-button";

const steps = [
  {
    number: "01",
    text: "Escolha seu número. Um, dez, cem. Quanto mais números, mais chances.",
  },
  {
    number: "02",
    text: "Concorra toda semana. Sorteios com prêmios reais, divulgados abertos.",
  },
  {
    number: "03",
    text: "Veja a empresa nascer. Cada número comprado vira tijolo. Você está construindo junto.",
  },
] as const;

export function HowItWorks() {
  return (
    <section className="px-6 md:px-16 lg:px-24 xl:px-32 py-24 md:py-32 bg-[var(--color-cream)]">
      <div className="max-w-3xl">
        <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-semibold text-[var(--color-charcoal)] mb-16">
          Como funciona
        </h2>

        <div className="flex flex-col gap-16">
          {steps.map((step) => (
            <div key={step.number} className="flex gap-6 md:gap-10 items-baseline">
              <span className="font-[family-name:var(--font-heading)] text-[clamp(2.5rem,5vw,4.5rem)] font-light text-[var(--color-terracotta)] opacity-60 leading-none shrink-0">
                {step.number}
              </span>
              <p className="text-lg md:text-xl leading-relaxed text-[var(--color-charcoal)]">
                {step.text}
              </p>
            </div>
          ))}
        </div>

        <div className="mt-20">
          <CTAButton size="md" />
          <p className="mt-4 text-sm text-[var(--color-charcoal)] opacity-50">
            Sem cadastro complicado. 1 minuto e você está dentro.
          </p>
        </div>
      </div>
    </section>
  );
}

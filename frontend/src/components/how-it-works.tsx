import { Section } from "@/components/section";

const steps = [
  {
    title: "Escolha quantos números quer",
    text: "A partir de R$ 1,00 por número. Sem limite.",
    icon: (
      <svg viewBox="0 0 40 40" fill="none" className="h-10 w-10" aria-hidden="true">
        <rect x="6" y="6" width="28" height="28" rx="8" stroke="var(--color-green)" strokeWidth="2" />
        <path d="M15 20h10M20 15v10" stroke="var(--color-green)" strokeWidth="2.5" strokeLinecap="round" />
      </svg>
    ),
    color: "var(--color-green)",
  },
  {
    title: "Pague via Pix",
    text: "Aprovação instantânea. Sem cartão, sem burocracia.",
    icon: (
      <svg viewBox="0 0 40 40" fill="none" className="h-10 w-10" aria-hidden="true">
        <path d="M8 20l10-10 5 5 9-9" stroke="var(--color-iris)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M8 28l10-10 5 5 9-9" stroke="var(--color-iris)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" opacity="0.3" />
      </svg>
    ),
    color: "var(--color-iris)",
  },
  {
    title: "Concorra automaticamente",
    text: "Resultado pela Loteria Federal em 20/12/2026.",
    icon: (
      <svg viewBox="0 0 40 40" fill="none" className="h-10 w-10" aria-hidden="true">
        <circle cx="20" cy="18" r="10" stroke="var(--color-orange)" strokeWidth="2" />
        <path d="M20 8v3M20 25v3M12 11l2 1.5M27 23l2 1.5M10 18h3M27 18h3M12 25l2-1.5M27 13l2-1.5" stroke="var(--color-orange)" strokeWidth="1.5" strokeLinecap="round" />
        <path d="M14 32h12" stroke="var(--color-orange)" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
    color: "var(--color-orange)",
  },
] as const;

export function HowItWorks() {
  return (
    <Section tone="snow">
      <h2 className="text-center font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-ink)] md:text-3xl">
        Como participar
      </h2>
      <p className="mt-3 max-w-sm text-center text-sm text-[var(--color-ink-soft)]">
        Três passos e você já está concorrendo
      </p>

      <div className="mt-10 flex w-full flex-col items-stretch gap-5 md:mt-14 md:flex-row md:items-stretch md:justify-center md:gap-5">
        {steps.map((step, index) => (
          <div
            key={step.title}
            className="flex w-full flex-col items-center rounded-2xl border p-6 text-center md:max-w-[320px] md:flex-1"
            style={{
              backgroundColor: `color-mix(in srgb, ${step.color} 4%, white)`,
              borderColor: `color-mix(in srgb, ${step.color} 16%, transparent)`,
            }}
          >
            <span
              className="mb-4 inline-flex h-7 items-center rounded-full px-3 text-[11px] font-bold text-white"
              style={{ backgroundColor: step.color }}
            >
              Passo {index + 1}
            </span>
            <div
              className="mb-4 flex h-16 w-16 items-center justify-center rounded-xl bg-white shadow-sm"
              style={{ border: `1px solid color-mix(in srgb, ${step.color} 18%, transparent)` }}
            >
              {step.icon}
            </div>
            <h3 className="mb-2 font-[family-name:var(--font-heading)] text-base font-semibold leading-snug text-[var(--color-ink)]">
              {step.title}
            </h3>
            <p className="text-sm text-[var(--color-ink-soft)]">
              {step.text}
            </p>
          </div>
        ))}
      </div>
    </Section>
  );
}

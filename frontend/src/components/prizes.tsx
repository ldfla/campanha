import { CTAButton } from "@/components/cta-button";
import { Section } from "@/components/section";
import { PRIZES, SURPRISE_PRIZES, formatBRL } from "@/lib/campaign-config";

const prizeStyles = [
  {
    accent: "var(--color-gold)",
    ink: "var(--color-gold-ink)",
    glow: true,
    icon: (
      <svg viewBox="0 0 48 48" fill="none" className="h-12 w-12" aria-hidden="true">
        <circle cx="24" cy="22" r="12" stroke="var(--color-gold)" strokeWidth="1.5" />
        <circle cx="24" cy="22" r="7" stroke="var(--color-gold)" strokeWidth="1.5" />
        <circle cx="24" cy="22" r="3" fill="var(--color-gold)" />
        <rect x="17" y="34" width="14" height="6" rx="2" stroke="var(--color-gold)" strokeWidth="1.5" />
      </svg>
    ),
  },
  {
    accent: "var(--color-iris)",
    ink: "var(--color-iris)",
    glow: false,
    icon: (
      <svg viewBox="0 0 48 48" fill="none" className="h-12 w-12" aria-hidden="true">
        <rect x="8" y="10" width="32" height="20" rx="3" stroke="var(--color-iris)" strokeWidth="1.5" />
        <rect x="13" y="14" width="22" height="12" rx="1" stroke="var(--color-iris)" strokeWidth="1" opacity="0.4" />
        <path d="M4 36h40" stroke="var(--color-iris)" strokeWidth="1.5" strokeLinecap="round" />
        <path d="M17 30h14v6H17z" stroke="var(--color-iris)" strokeWidth="1.5" />
      </svg>
    ),
  },
  {
    accent: "var(--color-orange)",
    ink: "#B4400F",
    glow: false,
    icon: (
      <svg viewBox="0 0 48 48" fill="none" className="h-12 w-12" aria-hidden="true">
        <rect x="8" y="14" width="32" height="20" rx="5" stroke="var(--color-orange)" strokeWidth="1.5" />
        <circle cx="27" cy="24" r="6" stroke="var(--color-orange)" strokeWidth="1.5" />
        <circle cx="27" cy="24" r="2.5" fill="var(--color-orange)" />
        <rect x="12" y="18" width="6" height="4" rx="1" stroke="var(--color-orange)" strokeWidth="1" />
      </svg>
    ),
  },
  {
    accent: "var(--color-jade)",
    ink: "#0F7A44",
    glow: false,
    icon: (
      <svg viewBox="0 0 48 48" fill="none" className="h-12 w-12" aria-hidden="true">
        <circle cx="24" cy="24" r="14" stroke="var(--color-jade)" strokeWidth="1.5" />
        <path d="M18 24h12M24 18v12" stroke="var(--color-jade)" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    accent: "var(--color-jade)",
    ink: "#0F7A44",
    glow: false,
    icon: (
      <svg viewBox="0 0 48 48" fill="none" className="h-12 w-12" aria-hidden="true">
        <circle cx="24" cy="24" r="14" stroke="var(--color-jade)" strokeWidth="1.5" />
        <path d="M18 24h12" stroke="var(--color-jade)" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
] as const;

const totalPrizesCents =
  PRIZES.reduce((sum, p) => sum + p.valueCents, 0) +
  SURPRISE_PRIZES.count * SURPRISE_PRIZES.valueCentsEach;

export function Prizes() {
  return (
    <Section tone="white">
      <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,2.75rem)] font-bold text-[var(--color-ink)]">
        Como você é retribuído
      </h2>
      <p className="mt-2 text-center text-sm text-[var(--color-ink-soft)]">
        {PRIZES.length} retribuições principais + {SURPRISE_PRIZES.count}{" "}
        surpresas
      </p>

      <div className="mt-8 flex w-full max-w-lg items-center justify-center gap-3 rounded-2xl border border-[var(--color-gold)]/35 bg-[var(--color-gold)]/[0.08] px-5 py-3">
        <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[var(--color-ink-soft)]">
          Total em prêmios
        </span>
        <span className="font-[family-name:var(--font-heading)] text-2xl font-bold tabular-nums text-[var(--color-gold-ink)]">
          {formatBRL(totalPrizesCents)}
        </span>
      </div>

      <div className="mt-10 grid w-full grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {PRIZES.map((prize, i) => {
          const style = prizeStyles[i];
          return (
            <article
              key={prize.tag}
              className={`flex flex-col items-center rounded-2xl border border-black/[0.06] p-5 text-center transition-shadow hover:shadow-lg ${
                style?.glow ? "animate-gold-glow" : ""
              }`}
            >
              <div
                className="mb-4 flex h-16 w-16 items-center justify-center rounded-xl"
                style={{
                  backgroundColor: `color-mix(in srgb, ${style?.accent ?? "var(--color-jade)"} 8%, transparent)`,
                }}
              >
                {style?.icon}
              </div>
              <span
                className="mb-1.5 text-[10px] font-bold tracking-wider"
                style={{ color: style?.ink ?? "var(--color-ink)" }}
              >
                {prize.tag}
              </span>
              <h3 className="mb-1 font-[family-name:var(--font-heading)] text-sm font-semibold leading-tight text-[var(--color-ink)]">
                {prize.name}
              </h3>
              <p className="mb-3 text-xs leading-snug text-[var(--color-ink-soft)]">
                {prize.items}
              </p>
              <span
                className="mt-auto font-[family-name:var(--font-heading)] text-xl font-bold tabular-nums"
                style={{ color: style?.ink ?? "var(--color-ink)" }}
              >
                {formatBRL(prize.valueCents)}
              </span>
            </article>
          );
        })}
      </div>

      <div className="mt-6 flex w-full items-center gap-4 rounded-2xl border border-[var(--color-iris)]/15 bg-gradient-to-r from-[var(--color-iris)]/[0.06] to-transparent px-5 py-4">
        <div className="flex-1">
          <span className="text-xs font-semibold text-[var(--color-iris)]">
            Bônus
          </span>
          <h3 className="font-[family-name:var(--font-heading)] text-sm font-semibold text-[var(--color-ink)] sm:text-base">
            {SURPRISE_PRIZES.count} prêmios-surpresa
          </h3>
          <p className="text-xs text-[var(--color-ink-soft)]">
            {formatBRL(SURPRISE_PRIZES.valueCentsEach)} cada, números aleatórios
            selados
          </p>
        </div>
        <span className="shrink-0 font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-iris)]">
          {formatBRL(SURPRISE_PRIZES.count * SURPRISE_PRIZES.valueCentsEach)}
        </span>
      </div>

      <div className="mt-14 flex w-full flex-col items-center gap-4">
        <CTAButton showPrice />
        <p className="text-xs text-[var(--color-ink-soft)] sm:text-sm">
          Quanto mais você contribui, mais chances de retribuição
        </p>
      </div>
    </Section>
  );
}

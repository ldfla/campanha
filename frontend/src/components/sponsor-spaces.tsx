import { Section } from "@/components/section";

const SPONSOR_TIERS = [
  {
    name: "Apoiador",
    description: "Logo no rodapé da página",
    priceCents: 10_000,
    available: 10,
  },
  {
    name: "Parceiro",
    description: "Banner lateral + menção nas redes sociais",
    priceCents: 25_000,
    available: 5,
  },
  {
    name: "Patrocinador",
    description: "Banner destaque + stories + menção no sorteio ao vivo",
    priceCents: 50_000,
    available: 3,
  },
] as const;

function formatBRL(cents: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(cents / 100);
}

export function SponsorSpaces() {
  return (
    <Section tone="white">
      <div className="mx-auto max-w-2xl">
        <div className="text-center">
          <span className="inline-block rounded-full bg-[var(--color-ember)]/10 px-4 py-1.5 text-xs font-bold text-[var(--color-ember)]">
            Espaços limitados
          </span>
        </div>
        <h2 className="mt-4 text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
          Anuncie na Ação entre Amigos
        </h2>
        <p className="mt-3 text-center text-base leading-relaxed text-[var(--color-ink-soft)]">
          Sua marca ao lado de uma causa que conecta pessoas.
          Alcance milhares de participantes e apoie o projeto.
        </p>

        <div className="mt-10 grid grid-cols-1 gap-4 sm:grid-cols-3">
          {SPONSOR_TIERS.map((tier) => (
            <div
              key={tier.name}
              className="flex flex-col rounded-2xl border border-black/[0.06] bg-white p-5 text-center transition-shadow hover:shadow-lg"
            >
              <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-ember)]">
                {tier.name}
              </span>
              <h3 className="mt-3 font-[family-name:var(--font-heading)] text-xl font-bold tabular-nums text-[var(--color-ink)]">
                {formatBRL(tier.priceCents)}
              </h3>
              <p className="mt-2 flex-1 text-sm leading-relaxed text-[var(--color-ink-soft)]">
                {tier.description}
              </p>
              <p className="mt-3 text-xs text-[var(--color-ink-soft)]">
                {tier.available} {(tier.available as number) === 1 ? "vaga" : "vagas"}
              </p>
              <a
                href={`https://wa.me/5511999999999?text=${encodeURIComponent(`Interesse no espaço ${tier.name} da Ação entre Amigos`)}`}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[var(--color-midnight)] px-4 py-3 text-sm font-bold text-white transition-colors hover:bg-[var(--color-midnight)]/90"
              >
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4" aria-hidden="true">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
                  <path d="M12 2C6.477 2 2 6.477 2 12c0 1.89.525 3.66 1.438 5.168L2 22l4.832-1.438A9.955 9.955 0 0012 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18a7.96 7.96 0 01-4.1-1.131l-.29-.174-3.01.894.81-2.953-.186-.3A7.96 7.96 0 014 12c0-4.411 3.589-8 8-8s8 3.589 8 8-3.589 8-8 8z" />
                </svg>
                Quero anunciar
              </a>
            </div>
          ))}
        </div>

        <p className="mt-8 text-center text-xs text-[var(--color-ink-soft)]">
          Entre em contato via WhatsApp para negociar seu espaço.
          Valores e formatos flexíveis para sua marca.
        </p>
      </div>
    </Section>
  );
}

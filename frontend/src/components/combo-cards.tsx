const COMBOS = [
  {
    id: "bronze",
    tier: "Bronze",
    quantity: "10 números",
    total: "R$ 6,00",
    unit: "R$ 0,60 / número",
    badge: null,
    accent: "#CD7F32",
    tint: "rgba(205, 127, 50, 0.14)",
    metal: "linear-gradient(135deg, #e9b07a 0%, #cd7f32 100%)",
    featured: false,
  },
  {
    id: "prata",
    tier: "Prata",
    quantity: "100 números",
    total: "R$ 50,00",
    unit: "R$ 0,50 / número",
    badge: null,
    accent: "#A8A8A8",
    tint: "rgba(192, 192, 192, 0.18)",
    metal: "linear-gradient(135deg, #f5f5f5 0%, #c0c0c0 100%)",
    featured: false,
  },
  {
    id: "ouro",
    tier: "Ouro",
    quantity: "1.000 números",
    total: "R$ 460,00",
    unit: "R$ 0,46 / número",
    badge: "Mais Popular",
    accent: "#E0B400",
    tint: "rgba(255, 215, 0, 0.16)",
    metal: "linear-gradient(135deg, #fff3a3 0%, #ffd700 100%)",
    featured: true,
  },
  {
    id: "diamante",
    tier: "Diamante",
    quantity: "5.000 números",
    total: "R$ 2.100,00",
    unit: "R$ 0,42 / número",
    badge: "Nível Máximo",
    accent: "#8FD9E8",
    tint: "rgba(185, 242, 255, 0.28)",
    metal: "linear-gradient(135deg, #eafcff 0%, #b9f2ff 100%)",
    featured: false,
  },
] as const;

export function ComboCards() {
  return (
    <section className="section bg-[var(--color-snow)] py-16 md:py-24">
      <div className="section-inner">
        <h2 className="text-center font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-ink)] md:text-3xl">
          Escolha seu combo e participe
        </h2>
        <p className="mt-3 max-w-md text-center text-sm text-[var(--color-ink-soft)]">
          Quanto maior o combo, menor o valor por número
        </p>

        <div className="mt-10 grid w-full grid-cols-1 gap-4 sm:grid-cols-2 md:mt-12 lg:grid-cols-4 lg:gap-5">
          {COMBOS.map((combo) => (
            <a
              key={combo.id}
              href={`/participar?combo=${combo.id}`}
              className={`group flex flex-col items-center rounded-2xl border-2 bg-white px-5 py-6 text-center transition-shadow hover:shadow-lg ${
                combo.featured
                  ? "shadow-[0_10px_30px_rgba(224,180,0,0.18)]"
                  : "shadow-[0_2px_10px_rgba(11,43,38,0.05)]"
              }`}
              style={{
                borderColor: combo.accent,
                backgroundImage: `linear-gradient(180deg, ${combo.tint} 0%, #ffffff 46%)`,
              }}
            >
              <span
                className="flex h-11 w-11 items-center justify-center rounded-full text-xs font-bold text-[var(--color-forest)] shadow-sm"
                style={{ backgroundImage: combo.metal }}
                aria-hidden="true"
              >
                {combo.tier.slice(0, 1)}
              </span>

              <span className="mt-3 text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-ink-soft)]">
                {combo.tier}
              </span>

              {combo.badge ? (
                <span
                  className="mt-2 rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wide"
                  style={{
                    backgroundColor: combo.tint,
                    color: combo.featured ? "var(--color-gold-ink)" : "var(--color-forest)",
                    border: `1px solid ${combo.accent}`,
                  }}
                >
                  {combo.badge}
                </span>
              ) : (
                <span className="mt-2 h-[22px]" aria-hidden="true" />
              )}

              <p className="mt-2 font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-ink)]">
                {combo.quantity}
              </p>

              <p className="mt-3 font-[family-name:var(--font-heading)] text-2xl font-bold tabular-nums text-[var(--color-ink)] md:text-3xl">
                {combo.total}
              </p>
              <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                {combo.unit}
              </p>

              <span className="mt-5 inline-flex w-full items-center justify-center rounded-xl bg-[var(--color-orange)] px-4 py-3 text-sm font-bold text-white transition-colors group-hover:bg-[#e85c28]">
                Quero este
              </span>
            </a>
          ))}
        </div>

        <p className="mt-8 text-center">
          <a
            href="/participar"
            className="text-sm font-medium text-[var(--color-iris)] underline-offset-4 hover:underline"
          >
            Ou compre a quantidade que quiser a R$ 0,65 cada
          </a>
        </p>
      </div>
    </section>
  );
}

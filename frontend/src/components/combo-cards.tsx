"use client";

import { useEffect, useState } from "react";
import { participations, type OfferResponse } from "@/lib/api-client";
import { COMBOS, DEMO_CAMPAIGN_ID, formatBRL } from "@/lib/campaign-config";
import { ctaLabel, useCampaignState } from "@/lib/use-campaign-state";

const VISUAL = [
  {
    accent: "#3F9C7C",
    tint: "rgba(63, 156, 124, 0.14)",
    metal: "linear-gradient(135deg, #a9e0cb 0%, #3f9c7c 100%)",
  },
  {
    accent: "#CD7F32",
    tint: "rgba(205, 127, 50, 0.14)",
    metal: "linear-gradient(135deg, #e9b07a 0%, #cd7f32 100%)",
  },
  {
    accent: "#E0B400",
    tint: "rgba(255, 215, 0, 0.16)",
    metal: "linear-gradient(135deg, #fff3a3 0%, #ffd700 100%)",
  },
  {
    accent: "#8FD9E8",
    tint: "rgba(185, 242, 255, 0.28)",
    metal: "linear-gradient(135deg, #eafcff 0%, #b9f2ff 100%)",
  },
  {
    accent: "#C084FC",
    tint: "rgba(192, 132, 252, 0.14)",
    metal: "linear-gradient(135deg, #e9d5ff 0%, #c084fc 100%)",
  },
] as const;

export function ComboCards() {
  const { purchaseEnabled, ctaState } = useCampaignState();
  const blocked = !purchaseEnabled;
  const [offers, setOffers] = useState<OfferResponse[]>([]);

  useEffect(() => {
    let active = true;
    participations
      .offers(DEMO_CAMPAIGN_ID)
      .then((list) => {
        if (active) setOffers(list);
      })
      .catch(() => {
        if (active) setOffers([]);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="section bg-[var(--color-snow)] py-16 md:py-24">
      <div className="section-inner">
        <h2 className="text-center font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-ink)] md:text-3xl">
          Escolha seu combo e participe
        </h2>
        <p className="mt-3 max-w-md text-center text-sm text-[var(--color-ink-soft)]">
          Quanto maior o combo, menor o valor por número
        </p>

        <div className="mt-10 grid w-full grid-cols-1 gap-4 sm:grid-cols-2 md:mt-12 lg:grid-cols-5 lg:gap-4">
          {offers.map((offer, i) => {
            const vis = VISUAL[i] ?? VISUAL[0];
            const combo = COMBOS.find((c) => c.qty === offer.quantity);
            const label = combo?.label ?? `Combo ${offer.quantity.toLocaleString("pt-BR")}`;
            const badge = combo?.badge ?? null;
            const popular = combo?.popular ?? false;
            return (
              <a
                key={offer.id}
                href={blocked ? undefined : `/participar?combo=${offer.id}`}
                className={`group flex flex-col items-center rounded-2xl border-2 bg-white px-5 py-6 text-center transition-shadow ${
                  blocked
                    ? "cursor-not-allowed opacity-60"
                    : "hover:shadow-lg"
                } ${
                  popular
                    ? "shadow-[0_10px_30px_rgba(224,180,0,0.18)]"
                    : "shadow-[0_2px_10px_rgba(11,43,38,0.05)]"
                }`}
                style={{
                  borderColor: vis.accent,
                  backgroundImage: `linear-gradient(180deg, ${vis.tint} 0%, #ffffff 46%)`,
                }}
                aria-disabled={blocked || undefined}
              >
                <span
                  className="flex h-11 w-11 items-center justify-center rounded-full text-xs font-bold text-[var(--color-forest)] shadow-sm"
                  style={{ backgroundImage: vis.metal }}
                  aria-hidden="true"
                >
                  {label.slice(0, 1)}
                </span>

                <span className="mt-3 text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-ink-soft)]">
                  {label}
                </span>

                {badge ? (
                  <span
                    className="mt-2 rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wide"
                    style={{
                      backgroundColor: vis.tint,
                      color: popular
                        ? "var(--color-gold-ink)"
                        : "var(--color-forest)",
                      border: `1px solid ${vis.accent}`,
                    }}
                  >
                    {badge}
                  </span>
                ) : (
                  <span className="mt-2 h-[26px]" aria-hidden="true" />
                )}

                <p className="mt-2 font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-ink)]">
                  {offer.quantity.toLocaleString("pt-BR")} números
                </p>

                <p className="mt-3 font-[family-name:var(--font-heading)] text-2xl font-bold tabular-nums text-[var(--color-ink)]">
                  {formatBRL(offer.totalCents)}
                </p>
                <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                  {formatBRL(offer.unitCents)} / número
                </p>
                <p className="mt-1 h-4 text-xs font-semibold text-[var(--color-forest)]">
                  {offer.discountPct > 0 ? `${offer.discountPct}% de desconto` : ""}
                </p>

                <span
                  className={`mt-5 inline-flex w-full items-center justify-center rounded-xl px-4 py-3 text-sm font-bold transition-colors ${
                    blocked
                      ? "bg-[var(--color-ink-soft)]/30 text-white/60"
                      : "bg-[var(--color-orange)] text-white group-hover:bg-[#e85c28]"
                  }`}
                >
                  {blocked ? ctaLabel(ctaState) : "Quero este"}
                </span>
              </a>
            );
          })}
        </div>
      </div>
    </section>
  );
}

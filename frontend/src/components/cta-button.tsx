"use client";

import { isDemoMode, formatBRL } from "@/lib/campaign-config";
import { ctaLabel, useCampaignState } from "@/lib/use-campaign-state";

export function CTAButton({
  size = "lg",
  showPrice = false,
}: {
  size?: "lg" | "md" | "sm";
  showPrice?: boolean;
}) {
  const { purchaseEnabled, ctaState, state } = useCampaignState();
  const entryPriceCents = state?.entryPriceCents ?? null;
  const demo = isDemoMode();

  const cls = {
    lg: "px-10 py-5 text-lg md:text-xl gap-3 rounded-2xl",
    md: "px-8 py-4 text-base md:text-lg gap-2.5 rounded-xl",
    sm: "px-6 py-3.5 text-sm md:text-base gap-2 rounded-xl",
  }[size];

  if (!purchaseEnabled) {
    return (
      <span
        className={`inline-flex items-center justify-center bg-[var(--color-ink-soft)]/40 font-bold text-white/70 cursor-not-allowed ${cls}`}
        role="status"
      >
        {ctaLabel(ctaState)}
      </span>
    );
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <a
        href="/participar"
        className={`group inline-flex items-center justify-center bg-gradient-to-r from-[var(--color-orange)] to-[#FF8A65] font-bold text-white shadow-[0_10px_30px_rgba(255,107,53,0.25)] transition-transform hover:scale-[1.03] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-orange)] active:scale-[0.97] animate-cta-beacon ${cls}`}
      >
        {showPrice && entryPriceCents !== null ? (
          <>
            {ctaLabel("OPEN")}
            <span className="rounded-md bg-white/25 px-2.5 py-1 text-[11px] font-extrabold tabular-nums tracking-wide text-white">
              {formatBRL(entryPriceCents)}
            </span>
          </>
        ) : (
          ctaLabel("OPEN")
        )}
      </a>
      {demo && (
        <span className="text-[10px] text-[var(--color-iris)] font-medium">
          Modo demonstração
        </span>
      )}
    </div>
  );
}

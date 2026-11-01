"use client";

import { useState, useEffect } from "react";
import { Section } from "@/components/section";
import {
  BENEFICIARY_ENTITIES,
  BENEFICIARY_POOL_CENTS,
  formatBRL,
  isDemoMode,
  DEMO_CAMPAIGN_ID,
} from "@/lib/campaign-config";

interface BeneficiaryStats {
  entityId: string;
  name: string;
  choiceCount: number;
  percentage: number;
  amountCents: number;
}

export function BeneficiaryRanking() {
  const demoMode = isDemoMode();
  const [stats, setStats] = useState<BeneficiaryStats[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // In production, fetch from server: GET /api/campaigns/{id}/beneficiary-stats
    // For DEMO, generate simulated distribution
    if (demoMode) {
      const simulated: BeneficiaryStats[] = BENEFICIARY_ENTITIES.map((e, i) => {
        const counts = [342, 287, 256, 198, 167];
        const count = counts[i] ?? 100;
        return { entityId: e.id, name: e.name, choiceCount: count, percentage: 0, amountCents: 0 };
      });
      const total = simulated.reduce((s, e) => s + e.choiceCount, 0);
      for (const entry of simulated) {
        entry.percentage = total > 0 ? Math.round((entry.choiceCount / total) * 100) : 0;
        entry.amountCents = total > 0
          ? Math.round((entry.choiceCount / total) * BENEFICIARY_POOL_CENTS)
          : 0;
      }
      simulated.sort((a, b) => b.choiceCount - a.choiceCount);
      setStats(simulated);
    }
    setLoading(false);
  }, [demoMode]);

  const entityColors: Record<string, string> = {};
  for (const e of BENEFICIARY_ENTITIES) {
    entityColors[e.id] = e.color;
  }

  if (loading) return null;

  return (
    <Section tone="snow">
      <div className="mx-auto max-w-2xl">
        <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
          Para onde vai seu apoio
        </h2>
        <p className="mt-3 text-center text-base leading-relaxed text-[var(--color-ink-soft)]">
          {formatBRL(BENEFICIARY_POOL_CENTS)} distribuídos proporcionalmente às escolhas dos apoiadores
        </p>

        {demoMode && (
          <p className="mt-2 text-center text-xs text-[var(--color-iris)] font-medium">
            Dados simulados · modo demonstração
          </p>
        )}

        <div className="mt-8 space-y-4">
          {stats.map((entry, i) => {
            const color = entityColors[entry.entityId] ?? "var(--color-iris)";
            return (
              <div key={entry.entityId} className="rounded-2xl border border-black/[0.06] bg-white p-5">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <span className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white ${
                      i === 0 ? "ring-2 ring-offset-2" : ""
                    }`}
                    style={{ backgroundColor: color, ["--tw-ring-color" as string]: color }}
                    >
                      {i + 1}º
                    </span>
                    <span className="font-[family-name:var(--font-heading)] text-base font-semibold text-[var(--color-ink)]">
                      {entry.name}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="font-[family-name:var(--font-heading)] text-lg font-bold tabular-nums text-[var(--color-ink)]">
                      {formatBRL(entry.amountCents)}
                    </span>
                    <p className="text-xs text-[var(--color-ink-soft)] tabular-nums">
                      {entry.choiceCount.toLocaleString("pt-BR")} escolhas · {entry.percentage}%
                    </p>
                  </div>
                </div>
                <div className="h-2 w-full rounded-full bg-black/[0.04] overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-700"
                    style={{ width: `${entry.percentage}%`, backgroundColor: color }}
                  />
                </div>
              </div>
            );
          })}
        </div>

        <p className="mt-6 text-center text-xs text-[var(--color-ink-soft)]">
          Atualizado em tempo real conforme os apoiadores escolhem suas instituições
        </p>
      </div>
    </Section>
  );
}

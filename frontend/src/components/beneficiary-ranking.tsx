"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import { Section } from "@/components/section";
import {
  BENEFICIARY_ENTITIES,
  BENEFICIARY_POOL_CENTS,
  formatBRL,
  isDemoMode,
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
    if (demoMode) {
      const counts = [342, 287, 256, 198, 167, 145];
      const simulated: BeneficiaryStats[] = BENEFICIARY_ENTITIES.map((e, i) => {
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

  const entityMap = new Map<string, (typeof BENEFICIARY_ENTITIES)[number]>(
    BENEFICIARY_ENTITIES.map((e) => [e.id, e]),
  );

  if (loading) return null;

  return (
    <Section tone="snow">
      <div className="mx-auto max-w-2xl">
        <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
          Para onde vai seu apoio
        </h2>
        <p className="mt-3 text-center text-base leading-relaxed text-[var(--color-ink-soft)]">
          Instituições dedicadas ao tratamento do câncer infantojuvenil
        </p>
        <p className="mt-1 text-center text-sm text-[var(--color-ink-soft)]">
          {formatBRL(BENEFICIARY_POOL_CENTS)} distribuídos proporcionalmente às escolhas dos apoiadores
        </p>

        {demoMode && (
          <p className="mt-2 text-center text-xs font-medium text-[var(--color-iris)]">
            Dados simulados · modo demonstração
          </p>
        )}

        {/* Ranking cards */}
        <div className="mt-8 space-y-3">
          {stats.map((entry, i) => {
            const entity = entityMap.get(entry.entityId);
            const color = entity?.color ?? "var(--color-iris)";
            return (
              <div key={entry.entityId} className="rounded-2xl border border-black/[0.06] bg-white p-4 sm:p-5">
                <div className="mb-3 flex items-center justify-between">
                  <a
                    href={entity?.url ?? "#"}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-3 transition-opacity hover:opacity-80"
                  >
                    {entity && (
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white p-1.5">
                        <Image
                          src={entity.logo}
                          alt={entity.name}
                          width={32}
                          height={32}
                          className="h-full w-full object-contain"
                          unoptimized
                        />
                      </div>
                    )}
                    <div>
                      <span className="font-[family-name:var(--font-heading)] text-sm font-semibold text-[var(--color-ink)] sm:text-base">
                        {entry.name}
                      </span>
                      <p className="text-xs text-[var(--color-ink-soft)] tabular-nums">
                        {entry.choiceCount.toLocaleString("pt-BR")} escolhas · {entry.percentage}%
                      </p>
                    </div>
                  </a>
                  <span className="font-[family-name:var(--font-heading)] text-base font-bold tabular-nums text-[var(--color-ink)] sm:text-lg">
                    {formatBRL(entry.amountCents)}
                  </span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-black/[0.04]">
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
          Todas as instituições listadas são dedicadas ao combate ao câncer infantojuvenil
        </p>
      </div>
    </Section>
  );
}

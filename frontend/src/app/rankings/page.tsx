"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { rankings, type RankingSnapshot, type RankingPosition, ApiError } from "@/lib/api-client";
import { isDemoMode, RANKING_REWARDS, formatBRL } from "@/lib/campaign-config";

const DEMO_CAMPAIGN_ID = "demo";

const SCOPES = [
  { id: "BUYERS", title: "Maiores apoiadores", subtitle: "Quem mais comprou números · Desempate: data da primeira cota", accent: "text-[var(--color-gold)] bg-[var(--color-gold)]/10", metricLabel: "números" },
  { id: "INFLUENCERS", title: "Maiores divulgadores", subtitle: "Mais indicações convertidas pelo link · Desempate: data da primeira cota", accent: "text-[var(--color-ember)] bg-[var(--color-ember)]/10", metricLabel: "indicações" },
] as const;

const PRIZE_MAP: Record<string, Record<number, string>> = Object.fromEntries(
  RANKING_REWARDS.map((r) => [
    r.scope,
    Object.fromEntries(r.positions.map((p) => [p.rank, formatBRL(p.valueCents)])),
  ]),
);

function RankingTable({
  title,
  subtitle,
  entries,
  accent,
  metricLabel,
  prizes,
  loading,
  error,
}: {
  title: string;
  subtitle: string;
  entries: RankingPosition[];
  accent: string;
  metricLabel: string;
  prizes: Record<number, string>;
  loading: boolean;
  error: string | null;
}) {
  return (
    <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
      <div className="px-5 py-4 md:px-6 md:py-5 border-b border-black/[0.04]">
        <h2 className="font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-midnight)]">
          {title}
        </h2>
        <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">{subtitle}</p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-10">
          <div className="w-6 h-6 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
        </div>
      ) : error ? (
        <div className="px-5 py-8 text-center text-sm text-[var(--color-midnight)]/40">
          {error}
        </div>
      ) : entries.length === 0 ? (
        <div className="px-5 py-8 text-center text-sm text-[var(--color-midnight)]/40">
          Nenhum participante no ranking ainda.
        </div>
      ) : (
        <div className="divide-y divide-black/[0.03]">
          {entries.map((entry) => {
            const prize = prizes[entry.rank] ?? "";
            return (
              <div
                key={`${entry.rank}-${entry.pseudonym}`}
                className={`flex items-center gap-4 px-5 py-3.5 md:px-6 md:py-4 ${entry.rank <= 3 ? "bg-[var(--color-snow)]" : ""}`}
              >
                <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold shrink-0 ${
                  entry.rank === 1
                    ? "bg-[var(--color-gold)] text-amber-900 animate-gold-glow"
                    : entry.rank === 2
                    ? "bg-gray-300 text-gray-700"
                    : entry.rank === 3
                    ? "bg-orange-300 text-orange-800"
                    : "bg-[var(--color-midnight)]/[0.06] text-[var(--color-midnight)]/40"
                }`}>
                  {entry.rank}
                </span>
                <div className="flex-1 min-w-0">
                  <span className="text-sm font-medium text-[var(--color-midnight)] tabular-nums">
                    {entry.pseudonym}
                  </span>
                </div>
                <span className="text-sm text-[var(--color-midnight)]/45 tabular-nums shrink-0">
                  {entry.metricValue.toLocaleString("pt-BR")} {metricLabel}
                </span>
                {prize && (
                  <span className={`text-xs font-bold ${accent} px-2.5 py-1 rounded-full shrink-0`}>
                    {prize}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function RankingsPage() {
  const demoMode = isDemoMode();
  const [data, setData] = useState<Record<string, { positions: RankingPosition[]; loading: boolean; error: string | null }>>(
    Object.fromEntries(SCOPES.map((s) => [s.id, { positions: [], loading: true, error: null }])),
  );

  useEffect(() => {
    for (const scope of SCOPES) {
      rankings
        .getByScope(DEMO_CAMPAIGN_ID, scope.id)
        .then((snapshot: RankingSnapshot) => {
          setData((prev) => ({
            ...prev,
            [scope.id]: { positions: snapshot.positions, loading: false, error: null },
          }));
        })
        .catch((err: unknown) => {
          const message = err instanceof ApiError ? err.detail : "Não foi possível carregar";
          setData((prev) => ({
            ...prev,
            [scope.id]: { positions: [], loading: false, error: message },
          }));
        });
    }
  }, []);

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/participar" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Participar
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-4xl mx-auto">
          <div className="text-center mb-10 md:mb-14">
            <h1 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-midnight)] mb-2">
              Placar ao vivo
            </h1>
            <p className="text-sm text-[var(--color-midnight)]/40">
              Atualizado automaticamente
            </p>
            {demoMode && (
              <div className="inline-flex items-center gap-2 mt-3 bg-[var(--color-iris)]/10 text-[var(--color-iris)] text-xs font-medium px-3 py-1.5 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-iris)] animate-pulse-dot" />
                Modo demonstração
              </div>
            )}
          </div>

          <div className="space-y-6 md:space-y-8">
            {SCOPES.map((scope) => {
              const d = data[scope.id];
              return (
                <RankingTable
                  key={scope.id}
                  title={scope.title}
                  subtitle={scope.subtitle}
                  entries={d?.positions ?? []}
                  accent={scope.accent}
                  metricLabel={scope.metricLabel}
                  prizes={PRIZE_MAP[scope.id] ?? {}}
                  loading={d?.loading ?? true}
                  error={d?.error ?? null}
                />
              );
            })}
          </div>

          <div className="mt-12 md:mt-16 bg-[var(--color-midnight)] rounded-2xl p-6 md:p-8 text-center">
            <p className="text-white/40 text-sm mb-2">Quer subir no ranking?</p>
            <p className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-bold text-white mb-6">
              Compre mais números ou indique amigos
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <a href="/participar"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white animate-cta-beacon hover:scale-[1.05] active:scale-[0.97] transition-transform">
                Comprar números
              </a>
              <a href="/indicar"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-xl font-semibold text-base border border-white/10 text-white/60 hover:text-white hover:border-white/20 transition-all">
                Indicar amigos
              </a>
            </div>
          </div>

          <div className="mt-8 text-center">
            <p className="text-xs text-[var(--color-midnight)]/30 max-w-lg mx-auto leading-relaxed">
              Rankings identificados por iniciais e últimos 4 dígitos do telefone.
              Premiações de ranking são bonificações separadas dos prêmios do sorteio.
              <a href="/regulamento" className="text-[var(--color-iris)] hover:underline ml-1">Ver regulamento</a>
            </p>
          </div>
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-6">
        <div className="max-w-4xl mx-auto flex items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">Ação entre Amigos</span>
          <nav className="flex gap-4 text-xs">
            <a href="/regulamento" className="hover:text-white/60 transition-colors">Regulamento</a>
            <a href="/indicar" className="hover:text-white/60 transition-colors">Indicar</a>
            <a href="/faq" className="hover:text-white/60 transition-colors">FAQ</a>
          </nav>
        </div>
      </footer>
    </div>
  );
}

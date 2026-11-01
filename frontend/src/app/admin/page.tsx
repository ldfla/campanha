"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { campaigns, participations, type CampaignResponse, ApiError } from "@/lib/api-client";
import { isDemoMode, formatBRL, DEMO_CAMPAIGN_ID } from "@/lib/campaign-config";

export default function AdminDashboard() {
  const demoMode = isDemoMode();
  const [campaign, setCampaign] = useState<CampaignResponse | null>(null);
  const [availability, setAvailability] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      campaigns.getById(DEMO_CAMPAIGN_ID).catch(() => null),
      participations.availability(DEMO_CAMPAIGN_ID).catch(() => null),
    ])
      .then(([c, a]) => {
        setCampaign(c);
        setAvailability(a);
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.detail : "Falha ao carregar dados");
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="w-8 h-8 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
      </div>
    );
  }

  const totalNumbers = campaign?.totalNumbers ?? 100_000;
  const sold = availability !== null ? totalNumbers - availability : 0;
  const soldPct = totalNumbers > 0 ? Math.round((sold / totalNumbers) * 100) : 0;

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
          Dashboard
        </h1>
        <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
          Visão geral da campanha {demoMode ? "(demonstração)" : ""}
        </p>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          label="Status"
          value={campaign?.status ?? "—"}
          accent="text-[var(--color-iris)]"
        />
        <StatCard
          label="Modo"
          value={campaign?.mode ?? "DEMO"}
          accent="text-[var(--color-jade)]"
        />
        <StatCard
          label="Números vendidos"
          value={`${sold.toLocaleString("pt-BR")} / ${totalNumbers.toLocaleString("pt-BR")}`}
          sub={`${soldPct}% do total`}
          accent="text-[var(--color-ember)]"
        />
        <StatCard
          label="Disponíveis"
          value={(availability ?? 0).toLocaleString("pt-BR")}
          accent="text-[var(--color-midnight)]"
        />
      </div>

      {/* Progress bar */}
      <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-8">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-medium text-[var(--color-midnight)]">Progresso de vendas</span>
          <span className="text-sm font-bold text-[var(--color-ember)] tabular-nums">{soldPct}%</span>
        </div>
        <div className="w-full h-3 bg-[var(--color-midnight)]/[0.06] rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[var(--color-iris)] to-[var(--color-jade)] rounded-full transition-all duration-1000"
            style={{ width: `${soldPct}%` }}
          />
        </div>
      </div>

      {/* Quick actions */}
      <h2 className="font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-midnight)] mb-4">
        Ações rápidas
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <QuickAction href="/admin/ofertas" title="Gerenciar ofertas" desc="Criar e editar combos de números" />
        <QuickAction href="/admin/beneficiarios" title="Instituições" desc="Gerenciar entidades beneficiárias" />
        <QuickAction href="/admin/pagamentos" title="Pagamentos" desc="Simular liquidação (DEMO)" />
        <QuickAction href="/admin/compliance" title="Compliance" desc="Documentos e regulamento" />
        <QuickAction href="/admin/sorteio" title="Sorteio" desc="Regras e apuração" />
        <QuickAction href="/admin/relatorios" title="Relatórios" desc="Relatório financeiro" />
      </div>
    </div>
  );
}

function StatCard({ label, value, sub, accent }: { label: string; value: string; sub?: string; accent: string }) {
  return (
    <div className="bg-white rounded-xl border border-black/[0.06] p-4">
      <p className="text-xs text-[var(--color-midnight)]/40 mb-1">{label}</p>
      <p className={`font-[family-name:var(--font-heading)] text-lg font-bold ${accent} tabular-nums`}>
        {value}
      </p>
      {sub && <p className="text-[10px] text-[var(--color-midnight)]/30 mt-0.5">{sub}</p>}
    </div>
  );
}

function QuickAction({ href, title, desc }: { href: string; title: string; desc: string }) {
  return (
    <Link
      href={href}
      className="flex flex-col gap-1 bg-white rounded-xl border border-black/[0.06] p-4 hover:border-[var(--color-iris)]/20 hover:shadow-sm transition-all"
    >
      <span className="text-sm font-semibold text-[var(--color-midnight)]">{title}</span>
      <span className="text-xs text-[var(--color-midnight)]/40">{desc}</span>
    </Link>
  );
}

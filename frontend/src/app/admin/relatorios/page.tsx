"use client";

import { useState } from "react";
import { DEMO_CAMPAIGN_ID, formatBRL } from "@/lib/campaign-config";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

interface FinancialReport {
  totalOrdersCents: number;
  confirmedOrdersCents: number;
  pendingOrdersCents: number;
  totalOrders: number;
  confirmedOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
}

export default function RelatoriosAdmin() {
  const [report, setReport] = useState<FinancialReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadReport() {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/financial/campaigns/${DEMO_CAMPAIGN_ID}/reports`, {
        credentials: "include",
        headers: { Accept: "application/json" },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error(typeof body.detail === "string" ? body.detail : res.statusText);
      }
      setReport(await res.json() as FinancialReport);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar relatório");
    } finally {
      setLoading(false);
    }
  }

  function handleExportCsv() {
    window.open(`${API_BASE}/financial/campaigns/${DEMO_CAMPAIGN_ID}/reports/export`, "_blank");
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
            Relatórios financeiros
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
            Visão consolidada de pedidos e pagamentos
          </p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={loadReport}
            disabled={loading}
            className="px-4 py-2.5 rounded-xl font-semibold text-sm bg-[var(--color-iris)] text-white hover:bg-[var(--color-iris)]/90 transition-colors disabled:opacity-50"
          >
            {loading ? "Carregando..." : "Gerar relatório"}
          </button>
          <button
            onClick={handleExportCsv}
            className="px-4 py-2.5 rounded-xl font-semibold text-sm border border-black/[0.08] text-[var(--color-midnight)]/60 hover:bg-black/[0.02] transition-colors"
          >
            Exportar CSV
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {report ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <ReportCard label="Pedidos totais" value={report.totalOrders.toString()} sub={formatBRL(report.totalOrdersCents)} />
          <ReportCard label="Confirmados" value={report.confirmedOrders.toString()} sub={formatBRL(report.confirmedOrdersCents)} accent="text-[var(--color-jade)]" />
          <ReportCard label="Pendentes" value={report.pendingOrders.toString()} sub={formatBRL(report.pendingOrdersCents)} accent="text-amber-600" />
          <ReportCard label="Cancelados" value={report.cancelledOrders.toString()} accent="text-red-500" />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center">
          <p className="text-sm text-[var(--color-midnight)]/45">
            Clique em &quot;Gerar relatório&quot; para ver os dados consolidados.
          </p>
        </div>
      )}
    </div>
  );
}

function ReportCard({ label, value, sub, accent }: { label: string; value: string; sub?: string; accent?: string }) {
  return (
    <div className="bg-white rounded-xl border border-black/[0.06] p-4">
      <p className="text-xs text-[var(--color-midnight)]/40 mb-1">{label}</p>
      <p className={`font-[family-name:var(--font-heading)] text-2xl font-bold tabular-nums ${accent ?? "text-[var(--color-midnight)]"}`}>
        {value}
      </p>
      {sub && <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5 tabular-nums">{sub}</p>}
    </div>
  );
}

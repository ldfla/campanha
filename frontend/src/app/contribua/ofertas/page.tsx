"use client";

import { useState, useEffect } from "react";
import { participations, type OfferResponse, ApiError } from "@/lib/api-client";
import { formatBRL, DEMO_CAMPAIGN_ID } from "@/lib/campaign-config";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export default function OfertasAdmin() {
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [newQty, setNewQty] = useState(10);
  const [newTotal, setNewTotal] = useState(950);
  const [showForm, setShowForm] = useState(false);

  async function loadOffers() {
    setLoading(true);
    try {
      const list = await participations.offers(DEMO_CAMPAIGN_ID);
      setOffers(list);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Falha ao carregar ofertas");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadOffers(); }, []);

  async function handleCreate() {
    setCreating(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/participations/campaigns/${DEMO_CAMPAIGN_ID}/offers`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity: newQty, totalCents: newTotal }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error(typeof body.detail === "string" ? body.detail : res.statusText);
      }
      setShowForm(false);
      setNewQty(10);
      setNewTotal(950);
      await loadOffers();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao criar oferta");
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
            Ofertas
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
            Combos de números disponíveis para compra
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2.5 rounded-xl font-semibold text-sm bg-[var(--color-iris)] text-white hover:bg-[var(--color-iris)]/90 transition-colors"
        >
          {showForm ? "Cancelar" : "Nova oferta"}
        </button>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {showForm && (
        <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
          <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">Nova oferta</h3>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-medium text-[var(--color-midnight)]/60 mb-1">Quantidade</label>
              <input
                type="number"
                min={1}
                max={100}
                value={newQty}
                onChange={(e) => setNewQty(parseInt(e.target.value) || 1)}
                className="w-full px-3 py-2.5 rounded-lg border border-black/[0.08] text-sm text-[var(--color-midnight)] bg-[var(--color-snow)] outline-none focus:border-[var(--color-iris)]/40"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-[var(--color-midnight)]/60 mb-1">Total (centavos)</label>
              <input
                type="number"
                min={1}
                value={newTotal}
                onChange={(e) => setNewTotal(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2.5 rounded-lg border border-black/[0.08] text-sm text-[var(--color-midnight)] bg-[var(--color-snow)] outline-none focus:border-[var(--color-iris)]/40"
              />
              <p className="text-[10px] text-[var(--color-midnight)]/30 mt-1">{formatBRL(newTotal)}</p>
            </div>
          </div>
          <div className="flex items-center justify-between bg-[var(--color-snow-warm)] rounded-lg px-4 py-2.5 mb-4">
            <span className="text-xs text-[var(--color-midnight)]/50">Valor unitário</span>
            <span className="text-sm font-bold text-[var(--color-midnight)] tabular-nums">
              {formatBRL(newQty > 0 ? Math.round(newTotal / newQty) : 0)}
            </span>
          </div>
          <button
            onClick={handleCreate}
            disabled={creating || newQty < 1 || newTotal < 1}
            className="w-full py-3 rounded-xl font-semibold text-sm bg-[var(--color-jade)] text-white hover:bg-[var(--color-jade)]/90 transition-colors disabled:opacity-50"
          >
            {creating ? "Criando..." : "Criar oferta"}
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="w-6 h-6 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
        </div>
      ) : offers.length === 0 ? (
        <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center">
          <p className="text-sm text-[var(--color-midnight)]/45">Nenhuma oferta cadastrada.</p>
          <p className="text-xs text-[var(--color-midnight)]/30 mt-1">
            Crie ofertas para que os participantes possam comprar combos.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-black/[0.04]">
                  <th className="text-left px-5 py-3 text-xs font-semibold text-[var(--color-midnight)]/40">ID</th>
                  <th className="text-right px-5 py-3 text-xs font-semibold text-[var(--color-midnight)]/40">Quantidade</th>
                  <th className="text-right px-5 py-3 text-xs font-semibold text-[var(--color-midnight)]/40">Total</th>
                  <th className="text-right px-5 py-3 text-xs font-semibold text-[var(--color-midnight)]/40">Unitário</th>
                  <th className="text-right px-5 py-3 text-xs font-semibold text-[var(--color-midnight)]/40">Moeda</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/[0.03]">
                {offers.map((o) => (
                  <tr key={o.id} className="hover:bg-[var(--color-snow)]/50 transition-colors">
                    <td className="px-5 py-3 font-mono text-xs text-[var(--color-midnight)]/50">{o.id.slice(0, 8)}...</td>
                    <td className="px-5 py-3 text-right tabular-nums font-medium text-[var(--color-midnight)]">
                      {o.quantity.toLocaleString("pt-BR")}
                    </td>
                    <td className="px-5 py-3 text-right tabular-nums font-bold text-[var(--color-ember)]">
                      {formatBRL(o.totalCents)}
                    </td>
                    <td className="px-5 py-3 text-right tabular-nums text-[var(--color-midnight)]/60">
                      {formatBRL(o.quantity > 0 ? Math.round(o.totalCents / o.quantity) : 0)}
                    </td>
                    <td className="px-5 py-3 text-right text-[var(--color-midnight)]/40">{o.currency}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

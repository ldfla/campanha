"use client";

import { useState, useEffect } from "react";
import { participations, type BeneficiaryResponse, ApiError } from "@/lib/api-client";
import { DEMO_CAMPAIGN_ID } from "@/lib/campaign-config";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export default function BeneficiariosAdmin() {
  const [entities, setEntities] = useState<BeneficiaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [showForm, setShowForm] = useState(false);

  async function loadEntities() {
    try {
      const list = await participations.beneficiaries(DEMO_CAMPAIGN_ID);
      setEntities(list);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Falha ao carregar instituições");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    participations
      .beneficiaries(DEMO_CAMPAIGN_ID)
      .then(setEntities)
      .catch((err) => setError(err instanceof ApiError ? err.detail : "Falha ao carregar instituições"))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate() {
    if (!newName.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/participations/campaigns/${DEMO_CAMPAIGN_ID}/beneficiaries`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: newName.trim() }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error(typeof body.detail === "string" ? body.detail : res.statusText);
      }
      setShowForm(false);
      setNewName("");
      await loadEntities();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao criar instituição");
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
            Instituições beneficiárias
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
            Entidades que recebem parte da arrecadação
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2.5 rounded-xl font-semibold text-sm bg-[var(--color-iris)] text-white hover:bg-[var(--color-iris)]/90 transition-colors"
        >
          {showForm ? "Cancelar" : "Nova instituição"}
        </button>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {showForm && (
        <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
          <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">Nova instituição</h3>
          <div className="mb-4">
            <label className="block text-xs font-medium text-[var(--color-midnight)]/60 mb-1">Nome</label>
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="Nome da instituição"
              maxLength={150}
              className="w-full px-3 py-2.5 rounded-lg border border-black/[0.08] text-sm text-[var(--color-midnight)] bg-[var(--color-snow)] outline-none focus:border-[var(--color-iris)]/40"
            />
          </div>
          <button
            onClick={handleCreate}
            disabled={creating || !newName.trim()}
            className="w-full py-3 rounded-xl font-semibold text-sm bg-[var(--color-jade)] text-white hover:bg-[var(--color-jade)]/90 transition-colors disabled:opacity-50"
          >
            {creating ? "Criando..." : "Criar instituição"}
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="w-6 h-6 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
        </div>
      ) : entities.length === 0 ? (
        <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center">
          <p className="text-sm text-[var(--color-midnight)]/45">Nenhuma instituição cadastrada.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {entities.map((e) => (
            <div key={e.id} className="bg-white rounded-xl border border-black/[0.06] p-4 flex items-center gap-4">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-jade)]/10 flex items-center justify-center shrink-0">
                <span className="text-lg font-bold text-[var(--color-jade)]">{e.name.charAt(0)}</span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-[var(--color-midnight)] truncate">{e.name}</p>
                <p className="text-[10px] text-[var(--color-midnight)]/30 font-mono">{e.id.slice(0, 8)}...</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

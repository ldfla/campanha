"use client";

import { useState } from "react";
import { surprises, type SurpriseCommitmentResponse, ApiError } from "@/lib/api-client";
import { DEMO_CAMPAIGN_ID } from "@/lib/campaign-config";

export default function SorteioAdmin() {
  const [commitment, setCommitment] = useState<SurpriseCommitmentResponse | null>(null);
  const [loadingCommitment, setLoadingCommitment] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadCommitment() {
    setLoadingCommitment(true);
    setError(null);
    try {
      const c = await surprises.commitment(DEMO_CAMPAIGN_ID);
      setCommitment(c);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setCommitment(null);
        setError("Nenhum commitment de prêmios-surpresa registrado ainda.");
      } else {
        setError(err instanceof ApiError ? err.detail : "Falha ao carregar commitment");
      }
    } finally {
      setLoadingCommitment(false);
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
          Sorteio e apuração
        </h1>
        <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
          Regras, prêmios-surpresa e resultado da Loteria Federal
        </p>
      </div>

      {/* Drawing rules placeholder */}
      <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
        <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">Regras do sorteio</h3>
        <div className="space-y-2 text-xs text-[var(--color-midnight)]/50">
          <p>O sorteio é vinculado ao resultado da Loteria Federal de 20/12/2026.</p>
          <p>A fórmula de apuração será registrada e versionada antes da abertura da campanha.</p>
          <p>O motor de apuração é determinístico e produz snapshots verificáveis.</p>
        </div>
        <div className="mt-4 flex gap-3">
          <span className="text-[10px] font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-full">
            Regras ainda não registradas
          </span>
        </div>
      </div>

      {/* Surprise commitment */}
      <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-[var(--color-midnight)]">Prêmios-surpresa (commit-reveal)</h3>
          <button
            onClick={loadCommitment}
            disabled={loadingCommitment}
            className="px-3 py-1.5 rounded-lg text-xs font-semibold text-[var(--color-iris)] bg-[var(--color-iris)]/10 hover:bg-[var(--color-iris)]/15 transition-colors disabled:opacity-50"
          >
            {loadingCommitment ? "Carregando..." : "Consultar commitment"}
          </button>
        </div>

        {error && (
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700 mb-3">
            {error}
          </div>
        )}

        {commitment ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-[var(--color-midnight)]/40">Hash SHA-256</span>
              <span className="text-xs font-mono text-[var(--color-midnight)] break-all text-right max-w-[60%]">
                {commitment.commitment}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs text-[var(--color-midnight)]/40">Definido em</span>
              <span className="text-xs text-[var(--color-midnight)]">{new Date(commitment.definedAt).toLocaleString("pt-BR")}</span>
            </div>
          </div>
        ) : (
          <p className="text-xs text-[var(--color-midnight)]/30">
            O commitment garante que os números-surpresa são definidos antes do início da campanha e não podem ser alterados.
          </p>
        )}
      </div>

      {/* Federal result placeholder */}
      <div className="bg-white rounded-2xl border border-black/[0.06] p-5">
        <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">Resultado da Loteria Federal</h3>
        <p className="text-xs text-[var(--color-midnight)]/40 mb-3">
          O resultado oficial será importado após a extração de 20/12/2026 com dupla aprovação.
        </p>
        <span className="text-[10px] font-semibold text-[var(--color-midnight)]/30 bg-[var(--color-midnight)]/[0.04] px-2.5 py-1 rounded-full">
          Aguardando data do sorteio
        </span>
      </div>
    </div>
  );
}

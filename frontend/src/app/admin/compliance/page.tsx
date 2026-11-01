"use client";

import { useState } from "react";
import { DEMO_CAMPAIGN_ID } from "@/lib/campaign-config";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export default function ComplianceAdmin() {
  const [uploading, setUploading] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError(null);
    setResult(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(`${API_BASE}/admin/campaigns/${DEMO_CAMPAIGN_ID}/documents`, {
        method: "POST",
        credentials: "include",
        body: formData,
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error(typeof body.detail === "string" ? body.detail : res.statusText);
      }
      setResult("Documento enviado com sucesso.");
      setFile(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao enviar documento");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
          Compliance
        </h1>
        <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
          Upload de documentos, regulamento e autorizações
        </p>
      </div>

      {/* Gates status */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        <GateCard name="G-LEGAL" desc="Aprovação jurídica" status="pending" />
        <GateCard name="G-PSP" desc="Contrato PSP" status="pending" />
        <GateCard name="G-FINANCE" desc="Aprovação financeira" status="pending" />
        <GateCard name="G-SECURITY" desc="Revisão de segurança" status="pending" />
        <GateCard name="G-OPERATIONS" desc="Infra e monitoramento" status="pending" />
        <GateCard name="G-LAUNCH" desc="Habilitação da campanha" status="pending" />
      </div>

      {/* Upload document */}
      <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6 max-w-lg">
        <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">
          Enviar documento
        </h3>

        {error && (
          <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}
        {result && (
          <div className="mb-4 rounded-xl border border-[var(--color-jade)]/20 bg-[var(--color-jade)]/[0.06] px-4 py-3 text-sm text-[var(--color-jade)] font-medium">
            {result}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-xs font-medium text-[var(--color-midnight)]/60 mb-1">Arquivo</label>
          <input
            type="file"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className="w-full text-sm text-[var(--color-midnight)] file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-[var(--color-iris)]/10 file:text-[var(--color-iris)] hover:file:bg-[var(--color-iris)]/15"
          />
        </div>

        <button
          onClick={handleUpload}
          disabled={uploading || !file}
          className="w-full py-3 rounded-xl font-semibold text-sm bg-[var(--color-iris)] text-white hover:bg-[var(--color-iris)]/90 transition-colors disabled:opacity-50"
        >
          {uploading ? "Enviando..." : "Enviar documento"}
        </button>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 max-w-lg">
        <p className="text-xs font-semibold text-amber-800 mb-1">Importante</p>
        <p className="text-xs text-amber-700/70">
          Todos os gates devem ser aprovados antes de habilitar a campanha em modo produção.
          Em modo DEMO, a plataforma funciona sem aprovação dos gates para fins de teste.
        </p>
      </div>
    </div>
  );
}

function GateCard({ name, desc, status }: { name: string; desc: string; status: "pending" | "approved" | "rejected" }) {
  const colors = {
    pending: "border-amber-200 bg-amber-50 text-amber-700",
    approved: "border-[var(--color-jade)]/20 bg-[var(--color-jade)]/[0.06] text-[var(--color-jade)]",
    rejected: "border-red-200 bg-red-50 text-red-700",
  };
  const icons = {
    pending: "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z",
    approved: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
    rejected: "M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z",
  };

  return (
    <div className={`rounded-xl border p-4 ${colors[status]}`}>
      <div className="flex items-center gap-2 mb-1">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
          <path d={icons[status]} />
        </svg>
        <span className="text-xs font-bold">{name}</span>
      </div>
      <p className="text-xs opacity-70">{desc}</p>
    </div>
  );
}

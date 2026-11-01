"use client";

import { useState } from "react";
import Link from "next/link";
import { drawings, type DrawTransparencyResponse, ApiError } from "@/lib/api-client";
import { isDemoMode, DEMO_CAMPAIGN_ID, formatBRL, PRIZES } from "@/lib/campaign-config";

export default function ApuracaoPage() {
  const [data, setData] = useState<DrawTransparencyResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [drawId, setDrawId] = useState("");
  const demoMode = isDemoMode();

  async function handleLoad() {
    if (!drawId.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await drawings.transparency(DEMO_CAMPAIGN_ID, drawId.trim());
      setData(result);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError("Apuração não encontrada. Verifique o identificador.");
      } else {
        setError("Não foi possível carregar os dados da apuração.");
      }
      setData(null);
    } finally {
      setLoading(false);
    }
  }

  const statusLabels: Record<string, { text: string; color: string }> = {
    DRAFT: { text: "Rascunho", color: "bg-gray-100 text-gray-600" },
    FROZEN: { text: "Congelado", color: "bg-blue-50 text-blue-700" },
    PENDING_RESULT: { text: "Aguardando resultado", color: "bg-amber-50 text-amber-700" },
    PUBLISHED: { text: "Publicado", color: "bg-[var(--color-jade)]/10 text-[var(--color-jade)]" },
    CANCELLED: { text: "Cancelado", color: "bg-red-50 text-red-700" },
  };

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/transparencia" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Transparência
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-2xl mx-auto">
          <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
            Apuração do sorteio
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
            Resultado oficial baseado na Loteria Federal de 20 de dezembro de 2026.
            Concurso a confirmar pela Caixa.
          </p>

          {demoMode && (
            <div className="mb-6 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-5 py-4">
              <p className="text-sm font-semibold text-[var(--color-iris)]">Modo demonstração</p>
              <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                Esta página está preparada para exibir os resultados quando a apuração for publicada.
                Nenhum resultado real está disponível.
              </p>
            </div>
          )}

          {/* Prizes reference */}
          <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
            <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">
              Prêmios em disputa
            </h2>
            <div className="space-y-2">
              {PRIZES.map((p) => (
                <div key={p.tag} className="flex items-center justify-between py-2 border-b border-black/[0.03] last:border-0">
                  <div>
                    <span className="text-xs font-bold text-[var(--color-iris)]">{p.tag}</span>
                    <p className="text-sm font-medium text-[var(--color-midnight)]">{p.name}</p>
                  </div>
                  <span className="font-[family-name:var(--font-heading)] text-base font-bold text-[var(--color-midnight)] tabular-nums">
                    {formatBRL(p.valueCents)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Draw lookup */}
          <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
            <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">
              Consultar apuração
            </h2>
            <p className="text-xs text-[var(--color-midnight)]/40 mb-4">
              Insira o identificador da apuração para verificar os resultados e a auditoria.
            </p>
            <div className="flex gap-3">
              <input
                type="text"
                value={drawId}
                onChange={(e) => setDrawId(e.target.value)}
                placeholder="ID da apuração"
                onKeyDown={(e) => e.key === "Enter" && handleLoad()}
                className="flex-1 px-4 py-3 rounded-xl border border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all text-sm"
              />
              <button
                onClick={handleLoad}
                disabled={!drawId.trim() || loading}
                className="px-6 py-3 rounded-xl font-semibold text-sm bg-[var(--color-midnight)] text-white hover:bg-[var(--color-midnight)]/90 transition-colors disabled:opacity-40"
              >
                {loading ? "..." : "Consultar"}
              </button>
            </div>
          </div>

          {error && (
            <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {data && (
            <>
              {/* Status */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-sm font-semibold text-[var(--color-midnight)]">
                    Status da apuração
                  </h2>
                  <span className={`text-xs font-bold px-3 py-1 rounded-full ${statusLabels[data.status]?.color ?? "bg-gray-100 text-gray-600"}`}>
                    {statusLabels[data.status]?.text ?? data.status}
                  </span>
                </div>

                {data.status !== "PUBLISHED" && (
                  <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 mb-4">
                    <p className="text-xs text-amber-700">
                      Esta apuração ainda não foi publicada oficialmente. Os dados abaixo são preliminares.
                    </p>
                  </div>
                )}

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-[var(--color-midnight)]/40">Versão da regra</span>
                    <span className="font-mono text-[var(--color-midnight)]">{data.ruleVersion}</span>
                  </div>
                  {data.contestIdentifier && (
                    <div className="flex justify-between">
                      <span className="text-[var(--color-midnight)]/40">Concurso</span>
                      <span className="font-mono text-[var(--color-midnight)]">{data.contestIdentifier}</span>
                    </div>
                  )}
                  {data.sourceUrl && (
                    <div className="flex justify-between items-start gap-4">
                      <span className="text-[var(--color-midnight)]/40 shrink-0">Fonte oficial</span>
                      <span className="text-[var(--color-iris)] break-all text-right">{data.sourceUrl}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Audit hashes */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">
                  Auditoria
                </h2>
                <div className="space-y-3">
                  <div className="bg-[var(--color-snow)] rounded-xl p-3">
                    <p className="text-[10px] text-[var(--color-midnight)]/30 mb-1">Hash do congelamento (entradas)</p>
                    <p className="font-mono text-[10px] text-[var(--color-midnight)] break-all leading-relaxed">{data.entriesHash}</p>
                  </div>
                  {data.sourceDocumentHash && (
                    <div className="bg-[var(--color-snow)] rounded-xl p-3">
                      <p className="text-[10px] text-[var(--color-midnight)]/30 mb-1">Hash do documento fonte</p>
                      <p className="font-mono text-[10px] text-[var(--color-midnight)] break-all leading-relaxed">{data.sourceDocumentHash}</p>
                    </div>
                  )}
                  <div className="bg-[var(--color-snow)] rounded-xl p-3">
                    <p className="text-[10px] text-[var(--color-midnight)]/30 mb-1">Fórmula de apuração</p>
                    <p className="font-mono text-[10px] text-[var(--color-midnight)] break-all leading-relaxed">{data.formulaJson}</p>
                  </div>
                </div>
              </div>

              {/* Outcomes */}
              {data.outcomes.length > 0 && (
                <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden mb-6">
                  <div className="px-5 py-3.5 border-b border-black/[0.04]">
                    <h2 className="text-sm font-semibold text-[var(--color-midnight)]">Resultados</h2>
                  </div>
                  <div className="divide-y divide-black/[0.03]">
                    {data.outcomes.map((outcome, i) => (
                      <div key={i} className="flex items-center justify-between px-5 py-4">
                        <div>
                          <span className="text-xs font-bold text-[var(--color-iris)]">{outcome.prizeTag}</span>
                          <p className="font-mono text-lg font-bold text-[var(--color-midnight)] tabular-nums mt-0.5">
                            {String(outcome.winningNumber).padStart(5, "0")}
                          </p>
                        </div>
                        <span className={`text-[10px] font-semibold px-2.5 py-1 rounded-full ${
                          outcome.entryId
                            ? "bg-[var(--color-jade)]/10 text-[var(--color-jade)]"
                            : "bg-gray-100 text-gray-500"
                        }`}>
                          {outcome.decisionCode === "WINNER" ? "Premiado" : outcome.decisionCode}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          {/* Info */}
          <div className="rounded-xl border border-[var(--color-midnight)]/8 bg-[var(--color-midnight)]/[0.02] px-5 py-4">
            <p className="text-xs text-[var(--color-midnight)]/40 text-center">
              A apuração utiliza resultado oficial da Loteria Federal (Caixa Econômica Federal).
              O resultado é importado e verificado pelo sistema, nunca consultado diretamente pelo navegador.
              A fórmula e os hashes são publicados antes do encerramento para conferência independente.
            </p>
          </div>
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-6">
        <div className="max-w-3xl mx-auto flex items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">Ação entre Amigos</span>
          <nav className="flex gap-4 text-xs">
            <a href="/transparencia" className="hover:text-white/60 transition-colors">Transparência</a>
            <a href="/regulamento" className="hover:text-white/60 transition-colors">Regulamento</a>
            <a href="/faq" className="hover:text-white/60 transition-colors">FAQ</a>
          </nav>
        </div>
      </footer>
    </div>
  );
}

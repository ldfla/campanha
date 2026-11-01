"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import {
  prizeLoad,
  type PrizeLoadResponse,
  type PrizeLoadVerifyResponse,
} from "@/lib/api-client";
import { formatBRL, isDemoMode, DEMO_CAMPAIGN_ID, SURPRISE_PRIZES } from "@/lib/campaign-config";

export default function TransparenciaPage() {
  const [data, setData] = useState<PrizeLoadResponse | null>(null);
  const [verification, setVerification] = useState<PrizeLoadVerifyResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const demoMode = isDemoMode();

  useEffect(() => {
    prizeLoad
      .get(DEMO_CAMPAIGN_ID)
      .then(setData)
      .catch(() => setError("Dados não disponíveis no momento."))
      .finally(() => setLoading(false));
  }, []);

  async function handleVerify() {
    setVerifying(true);
    try {
      const result = await prizeLoad.verify(DEMO_CAMPAIGN_ID);
      setVerification(result);
    } catch {
      setVerification(null);
      setError("Não foi possível verificar a integridade.");
    } finally {
      setVerifying(false);
    }
  }

  const csvUrl = prizeLoad.exportCsvUrl(DEMO_CAMPAIGN_ID);

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/regulamento" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Regulamento
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-2xl mx-auto">
          <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
            Transparência
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
            Os prêmios-surpresa foram definidos antes da campanha, com compromisso criptográfico verificável.
          </p>

          {demoMode && (
            <div className="mb-6 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-5 py-4">
              <p className="text-sm font-semibold text-[var(--color-iris)]">Modo demonstração</p>
              <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                Os dados exibidos são simulados. Este painel não constitui compromisso real de premiação.
              </p>
            </div>
          )}

          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-8 h-8 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
            </div>
          ) : error && !data ? (
            <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center">
              <p className="text-sm text-[var(--color-midnight)]/45 mb-4">{error}</p>
              <p className="text-xs text-[var(--color-midnight)]/25">
                Os dados de transparência estarão disponíveis quando o servidor estiver ativo.
              </p>

              {/* Fallback: show from config */}
              <div className="mt-8 text-left">
                <h2 className="font-[family-name:var(--font-heading)] text-lg font-bold text-[var(--color-midnight)] mb-4">
                  Prêmios-surpresa previstos
                </h2>
                <div className="space-y-3">
                  {SURPRISE_PRIZES.map((tier, i) => (
                    <div key={i} className="bg-[var(--color-snow)] rounded-xl p-4 flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-[var(--color-midnight)]">{tier.label}</p>
                        <p className="text-xs text-[var(--color-midnight)]/40">{tier.unitLabel}</p>
                      </div>
                      <p className="font-[family-name:var(--font-heading)] text-lg font-bold text-[var(--color-jade)] tabular-nums">
                        {formatBRL(tier.count * tier.valueCentsEach)}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : data ? (
            <>
              {/* Tiers */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">
                  Prêmios-surpresa carregados
                </h2>

                <div className="space-y-3">
                  <div className="bg-[var(--color-snow)] rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold text-[var(--color-midnight)]">Tier 1</span>
                      <span className="text-xs font-semibold text-[var(--color-jade)]">
                        {formatBRL(data.tier1Cents)} cada
                      </span>
                    </div>
                    <p className="text-xs text-[var(--color-midnight)]/40">
                      {data.tier1Count} números premiados · Total: {formatBRL(data.tier1Count * data.tier1Cents)}
                    </p>
                  </div>

                  <div className="bg-[var(--color-snow)] rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold text-[var(--color-midnight)]">Tier 2</span>
                      <span className="text-xs font-semibold text-[var(--color-jade)]">
                        {formatBRL(data.tier2Cents)} cada
                      </span>
                    </div>
                    <p className="text-xs text-[var(--color-midnight)]/40">
                      {data.tier2Count} números premiados · Total: {formatBRL(data.tier2Count * data.tier2Cents)}
                    </p>
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-black/[0.04]">
                  <div className="flex items-center justify-between text-xs text-[var(--color-midnight)]/40">
                    <span>Intervalo de números</span>
                    <span className="font-mono tabular-nums">
                      {String(data.numberMin).padStart(5, "0")} – {String(data.numberMax).padStart(5, "0")}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-xs text-[var(--color-midnight)]/40 mt-1">
                    <span>Definido em</span>
                    <span>{new Date(data.definedAt).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" })}</span>
                  </div>
                  <div className="flex items-center justify-between text-xs text-[var(--color-midnight)]/40 mt-1">
                    <span>Gerado em</span>
                    <span>{new Date(data.generatedAt).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" })}</span>
                  </div>
                </div>
              </div>

              {/* SHA-256 */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">
                  Compromisso criptográfico
                </h2>
                <div className="bg-[var(--color-snow)] rounded-xl p-4 mb-4">
                  <p className="text-[10px] text-[var(--color-midnight)]/30 mb-1">SHA-256 do arquivo</p>
                  <p className="font-mono text-xs text-[var(--color-midnight)] break-all leading-relaxed tabular-nums">
                    {data.fileSha256}
                  </p>
                </div>

                {verification ? (
                  <div className={`rounded-xl p-4 ${verification.match ? "bg-[var(--color-jade)]/[0.08] border border-[var(--color-jade)]/20" : "bg-red-50 border border-red-200"}`}>
                    <div className="flex items-center gap-2 mb-2">
                      {verification.match ? (
                        <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 text-[var(--color-jade)]">
                          <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
                          <path d="M7 10l2 2 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      ) : (
                        <svg viewBox="0 0 20 20" fill="none" className="w-5 h-5 text-red-600">
                          <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.5" />
                          <path d="M7 7l6 6M13 7l-6 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                        </svg>
                      )}
                      <span className={`text-sm font-semibold ${verification.match ? "text-[var(--color-jade)]" : "text-red-700"}`}>
                        {verification.match ? "Integridade verificada" : "Falha na verificação"}
                      </span>
                    </div>
                    <div className="text-xs space-y-1">
                      <div className="flex gap-2">
                        <span className="text-[var(--color-midnight)]/30 shrink-0">Armazenado:</span>
                        <span className="font-mono text-[var(--color-midnight)]/60 break-all">{verification.storedSha256}</span>
                      </div>
                      <div className="flex gap-2">
                        <span className="text-[var(--color-midnight)]/30 shrink-0">Calculado:</span>
                        <span className="font-mono text-[var(--color-midnight)]/60 break-all">{verification.computedSha256}</span>
                      </div>
                    </div>
                  </div>
                ) : (
                  <button
                    onClick={handleVerify}
                    disabled={verifying}
                    className="w-full py-3 rounded-xl font-semibold text-sm border border-[var(--color-iris)]/20 text-[var(--color-iris)] hover:bg-[var(--color-iris)]/[0.04] transition-colors disabled:opacity-50"
                  >
                    {verifying ? "Verificando..." : "Verificar integridade"}
                  </button>
                )}
              </div>

              {/* CSV download */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <h2 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">
                  Exportar dados
                </h2>
                <p className="text-xs text-[var(--color-midnight)]/40 mb-4">
                  Baixe o arquivo CSV com todos os números premiados e seus valores para conferência independente.
                </p>
                <a
                  href={csvUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-5 py-3 rounded-xl font-semibold text-sm bg-[var(--color-midnight)] text-white hover:bg-[var(--color-midnight)]/90 transition-colors"
                >
                  <svg viewBox="0 0 20 20" fill="none" className="w-4 h-4" aria-hidden="true">
                    <path d="M10 3v10M6 9l4 4 4-4M4 15h12" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  Baixar CSV
                </a>
              </div>
            </>
          ) : null}
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-6">
        <div className="max-w-3xl mx-auto flex items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">Ação entre Amigos</span>
          <nav className="flex gap-4 text-xs">
            <a href="/regulamento" className="hover:text-white/60 transition-colors">Regulamento</a>
            <a href="/numeros" className="hover:text-white/60 transition-colors">Números</a>
            <a href="/faq" className="hover:text-white/60 transition-colors">FAQ</a>
          </nav>
        </div>
      </footer>
    </div>
  );
}

"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/use-auth";
import { QRCode } from "@/components/qr-code";
import {
  referrals,
  type ReferralMetrics,
} from "@/lib/api-client";
import {
  isDemoMode,
  formatBRL,
  PRICE_UNIT_CENTS,
  TOTAL_PRIZES_CENTS,
  RANKING_REWARDS,
  DEMO_CAMPAIGN_ID,
} from "@/lib/campaign-config";
import { CodeLoginForm } from "@/components/code-login-form";


export default function IndicarPage() {
  const { user, loading: authLoading } = useAuth();
  const [copied, setCopied] = useState(false);
  const [referralCode, setReferralCode] = useState<string | null>(null);
  const [metrics, setMetrics] = useState<ReferralMetrics | null>(null);
  const [loadingLink, setLoadingLink] = useState(true);
  const demoMode = isDemoMode();

  const referralUrl = referralCode
    ? `${typeof window !== "undefined" ? window.location.origin : ""}/participar?ref=${referralCode}`
    : null;

  useEffect(() => {
    if (!user) return;
    referrals
      .createLink(DEMO_CAMPAIGN_ID)
      .then((m) => setReferralCode(m.code))
      .catch(() => {})
      .finally(() => setLoadingLink(false));

    referrals
      .metrics(DEMO_CAMPAIGN_ID)
      .then(setMetrics)
      .catch(() => {});
  }, [user]);

  const [showQR, setShowQR] = useState(false);

  const handleCopy = useCallback(() => {
    if (!referralUrl) return;
    navigator.clipboard.writeText(referralUrl).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [referralUrl]);

  const shareTitle = "Ação entre Amigos";
  const shareText = referralUrl
    ? `Participe da Ação entre Amigos e concorra a ${formatBRL(TOTAL_PRIZES_CENTS)} em prêmios! Só ${formatBRL(PRICE_UNIT_CENTS)} por número. Sorteio pela Loteria Federal!`
    : "";

  const handleNativeShare = useCallback(async () => {
    if (!referralUrl || typeof navigator.share !== "function") return;
    try {
      await navigator.share({
        title: shareTitle,
        text: shareText,
        url: referralUrl,
      });
    } catch {
      // user cancelled or share failed — no-op
    }
  }, [referralUrl, shareText]);

  const canNativeShare = typeof navigator !== "undefined" && typeof navigator.share === "function";

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/rankings" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Rankings
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-lg mx-auto">
          {authLoading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-8 h-8 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
            </div>
          ) : !user ? (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Indicar amigos
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Compartilhe seu link e concorra a prêmios extras no ranking de indicações.
              </p>

              <CodeLoginForm submitLabel="Ver meu link de indicação" />
            </div>
          ) : (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Seu link de indicação
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Compartilhe para subir no ranking e ganhar prêmios.
              </p>

              {loadingLink ? (
                <div className="flex items-center justify-center py-12">
                  <div className="w-6 h-6 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
                </div>
              ) : referralUrl ? (
                <>
                  {/* Link + Copiar */}
                  <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-4">
                    <div className="flex items-center gap-3 bg-[var(--color-snow)] rounded-xl px-4 py-3 mb-4">
                      <span className="flex-1 text-sm text-[var(--color-midnight)] truncate font-mono tabular-nums">
                        {referralUrl}
                      </span>
                      <button
                        onClick={handleCopy}
                        className={`shrink-0 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
                          copied
                            ? "bg-[var(--color-jade)] text-white"
                            : "bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white"
                        }`}
                      >
                        {copied ? "Copiado!" : "Copiar"}
                      </button>
                    </div>

                    {/* Botões de compartilhamento */}
                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                      {/* WhatsApp */}
                      <a
                        href={`https://wa.me/?text=${encodeURIComponent(`${shareText}\n\n${referralUrl}`)}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center justify-center gap-2 py-3 rounded-xl bg-[#25D366]/10 text-[#25D366] font-semibold text-sm hover:bg-[#25D366]/15 transition-colors"
                      >
                        <svg viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5" aria-hidden="true">
                          <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
                          <path d="M12 2C6.477 2 2 6.477 2 12c0 1.89.525 3.66 1.438 5.168L2 22l4.832-1.438A9.955 9.955 0 0012 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18a8 8 0 01-4.243-1.214l-.257-.154-2.874.854.854-2.874-.154-.257A8 8 0 1112 20z" />
                        </svg>
                        WhatsApp
                      </a>

                      {/* Telegram */}
                      <a
                        href={`https://t.me/share/url?url=${encodeURIComponent(referralUrl)}&text=${encodeURIComponent(shareText)}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center justify-center gap-2 py-3 rounded-xl bg-[#0088cc]/10 text-[#0088cc] font-semibold text-sm hover:bg-[#0088cc]/15 transition-colors"
                      >
                        <svg viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5" aria-hidden="true">
                          <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.479.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z" />
                        </svg>
                        Telegram
                      </a>

                      {/* Copiar link */}
                      <button
                        onClick={handleCopy}
                        className="flex items-center justify-center gap-2 py-3 rounded-xl bg-[var(--color-midnight)]/[0.06] text-[var(--color-midnight)]/60 font-semibold text-sm hover:bg-[var(--color-midnight)]/10 transition-colors"
                      >
                        <svg viewBox="0 0 20 20" fill="none" className="w-4.5 h-4.5" aria-hidden="true">
                          <rect x="5" y="5" width="10" height="12" rx="2" stroke="currentColor" strokeWidth="1.5" />
                          <path d="M8 5V3.5A1.5 1.5 0 019.5 2h5A1.5 1.5 0 0116 3.5v9a1.5 1.5 0 01-1.5 1.5H13" stroke="currentColor" strokeWidth="1.5" />
                        </svg>
                        Copiar
                      </button>

                      {/* Compartilhar nativo (mobile) */}
                      {canNativeShare && (
                        <button
                          onClick={handleNativeShare}
                          className="flex items-center justify-center gap-2 py-3 rounded-xl bg-[var(--color-iris)]/10 text-[var(--color-iris)] font-semibold text-sm hover:bg-[var(--color-iris)]/15 transition-colors"
                        >
                          <svg viewBox="0 0 20 20" fill="none" className="w-4.5 h-4.5" aria-hidden="true">
                            <path d="M10 3v10M6 7l4-4 4 4M4 13v2a2 2 0 002 2h8a2 2 0 002-2v-2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                          Enviar
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Toggle QR Code */}
                  <button
                    onClick={() => setShowQR(!showQR)}
                    className="w-full flex items-center justify-center gap-2 py-3 rounded-xl border border-black/[0.06] text-[var(--color-midnight)]/50 font-semibold text-sm hover:bg-black/[0.02] transition-colors mb-4"
                  >
                    <svg viewBox="0 0 20 20" fill="none" className="w-4.5 h-4.5" aria-hidden="true">
                      <rect x="3" y="3" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.5" />
                      <rect x="11" y="3" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.5" />
                      <rect x="3" y="11" width="6" height="6" rx="1" stroke="currentColor" strokeWidth="1.5" />
                      <rect x="12" y="12" width="4" height="4" rx="0.5" stroke="currentColor" strokeWidth="1.5" />
                    </svg>
                    {showQR ? "Esconder QR Code" : "Mostrar QR Code"}
                  </button>

                  {/* QR Code expandível */}
                  {showQR && (
                    <div className="bg-white rounded-2xl border border-black/[0.06] p-6 mb-4 text-center">
                      <p className="text-sm font-semibold text-[var(--color-midnight)] mb-4">
                        Mostre este QR Code para indicar pessoalmente
                      </p>
                      <div className="inline-block rounded-xl bg-white p-3 shadow-sm border border-black/[0.04]">
                        <QRCode value={referralUrl} size={180} />
                      </div>
                      <p className="mt-3 text-xs text-[var(--color-midnight)]/30">
                        Aponte a câmera do celular para abrir o link
                      </p>
                    </div>
                  )}
                </>
              ) : (
                <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center mb-6">
                  <p className="text-sm text-[var(--color-midnight)]/45">
                    Não foi possível gerar o link. Tente novamente.
                  </p>
                </div>
              )}

              {metrics && (
                <div className="grid grid-cols-2 gap-3 mb-6">
                  <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                    <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)] tabular-nums">
                      {metrics.clicks.toLocaleString("pt-BR")}
                    </p>
                    <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Cliques</p>
                  </div>
                  <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                    <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-jade)] tabular-nums">
                      {metrics.participations.toLocaleString("pt-BR")}
                    </p>
                    <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Vendas</p>
                  </div>
                </div>
              )}

              <div className="bg-[var(--color-midnight)] rounded-2xl p-5 md:p-6">
                <h3 className="font-[family-name:var(--font-heading)] text-base font-semibold text-white mb-4">
                  Prêmios por indicação
                </h3>
                <div className="space-y-2.5">
                  {RANKING_REWARDS.find((r) => r.scope === "PROMOTER")?.positions.map((p) => (
                    <div key={p.rank} className="flex items-center justify-between text-sm">
                      <span className="text-white/45">{p.label}</span>
                      <span className="font-bold text-[var(--color-gold)]">{formatBRL(p.valueCents)}</span>
                    </div>
                  ))}
                </div>
                <a href="/rankings" className="inline-block mt-4 text-xs text-[var(--color-iris)] hover:text-[var(--color-iris)]/80 transition-colors">
                  Ver ranking completo
                </a>
              </div>

              {demoMode && (
                <div className="mt-6 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-4 py-3 text-center">
                  <p className="text-xs font-semibold text-[var(--color-iris)]">Modo demonstração</p>
                  <p className="mt-0.5 text-[10px] text-[var(--color-ink-soft)]">
                    Links e métricas são simulados. Este painel não constitui oferta de comissão real nem direito a bonificação.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-6">
        <div className="max-w-3xl mx-auto flex items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">Ação entre Amigos</span>
          <nav className="flex gap-4 text-xs">
            <a href="/regulamento" className="hover:text-white/60 transition-colors">Regulamento</a>
            <a href="/rankings" className="hover:text-white/60 transition-colors">Rankings</a>
            <a href="/faq" className="hover:text-white/60 transition-colors">FAQ</a>
          </nav>
        </div>
      </footer>
    </div>
  );
}

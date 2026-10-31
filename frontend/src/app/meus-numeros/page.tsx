"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/use-auth";
import {
  participations,
  type NumberAvailability,
  type PagedResponse,
  ApiError,
} from "@/lib/api-client";
import { isDemoMode, formatBRL, PRICE_UNIT_CENTS } from "@/lib/campaign-config";

const DEMO_CAMPAIGN_ID = "demo";

export default function MeusNumerosPage() {
  const { user, loading: authLoading, login, logout } = useAuth();
  const [phone, setPhone] = useState("");
  const [loginError, setLoginError] = useState<string | null>(null);
  const [loggingIn, setLoggingIn] = useState(false);
  const [numbers, setNumbers] = useState<NumberAvailability[]>([]);
  const [loadingNumbers, setLoadingNumbers] = useState(false);
  const demoMode = isDemoMode();

  const fetchNumbers = useCallback(async () => {
    if (!user) return;
    setLoadingNumbers(true);
    try {
      const res: PagedResponse<NumberAvailability> =
        await participations.numbers(DEMO_CAMPAIGN_ID);
      setNumbers(res.content);
    } catch {
      setNumbers([]);
    } finally {
      setLoadingNumbers(false);
    }
  }, [user]);

  useEffect(() => {
    if (user) fetchNumbers();
  }, [user, fetchNumbers]);

  async function handleLogin() {
    if (!phone.trim() || phone.replace(/\D/g, "").length < 10) return;
    setLoggingIn(true);
    setLoginError(null);
    try {
      const phoneFormatted = phone.replace(/\D/g, "");
      const phoneWithCountry = phoneFormatted.startsWith("55")
        ? phoneFormatted
        : `55${phoneFormatted}`;
      await login(phoneWithCountry);
    } catch (err) {
      if (err instanceof ApiError) {
        setLoginError(err.detail);
      } else {
        setLoginError("Não foi possível entrar. Verifique seus dados.");
      }
    } finally {
      setLoggingIn(false);
    }
  }

  const confirmed = numbers.filter((n) => n.status === "CONFIRMED").length;
  const pending = numbers.filter((n) => n.status === "PENDING" || n.status === "RESERVED").length;
  const totalPaidCents = confirmed * PRICE_UNIT_CENTS;

  const statusLabel: Record<string, { text: string; color: string }> = {
    CONFIRMED: { text: "Confirmado", color: "bg-[var(--color-jade)]/10 text-[var(--color-jade)]" },
    PENDING: { text: "Aguardando Pix", color: "bg-[var(--color-gold)]/10 text-amber-700" },
    RESERVED: { text: "Reservado", color: "bg-[var(--color-iris)]/10 text-[var(--color-iris)]" },
    AVAILABLE: { text: "Disponível", color: "bg-black/5 text-black/40" },
  };

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/participar" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Participar
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-2xl mx-auto">
          {authLoading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-8 h-8 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
            </div>
          ) : !user ? (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Meus números
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Informe seu WhatsApp para consultar seus números.
              </p>

              <div className="bg-white rounded-2xl border border-black/[0.06] p-6 md:p-8">
                {loginError && (
                  <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {loginError}
                  </div>
                )}
                <div className="space-y-4 mb-5">
                  <div>
                    <label htmlFor="phone" className="block text-sm font-medium text-[var(--color-midnight)] mb-2">
                      Número de WhatsApp
                    </label>
                    <input
                      id="phone"
                      type="tel"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="(11) 99999-9999"
                      autoComplete="tel"
                      onKeyDown={(e) => e.key === "Enter" && handleLogin()}
                      className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all text-lg"
                    />
                  </div>
                </div>
                <button
                  onClick={handleLogin}
                  disabled={phone.replace(/\D/g, "").length < 10 || loggingIn}
                  className="w-full py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white shadow-[0_4px_24px_rgba(255,99,71,0.3)] hover:shadow-[0_8px_32px_rgba(255,99,71,0.45)] active:scale-[0.98] transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {loggingIn ? "Entrando..." : "Consultar meus números"}
                </button>
                {demoMode && (
                  <p className="mt-3 text-xs text-[var(--color-midnight)]/30 text-center">
                    Modo demonstração — em produção, um código de verificação será enviado
                  </p>
                )}
              </div>

              <div className="mt-8 rounded-xl border border-[var(--color-midnight)]/8 bg-[var(--color-midnight)]/[0.02] px-5 py-4">
                <p className="text-xs text-[var(--color-midnight)]/40 text-center">
                  Seus dados só são exibidos após a autenticação. Nenhuma informação é acessível sem identificação.
                </p>
              </div>
            </div>
          ) : (
            <div className="animate-slide-in">
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)]">
                    Meus números
                  </h1>
                  <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
                    {user.firstName} {user.surname}
                  </p>
                </div>
                <button
                  onClick={() => {
                    logout();
                    setPhone("");
                    setNumbers([]);
                  }}
                  className="text-sm text-[var(--color-iris)] hover:underline"
                >
                  Sair
                </button>
              </div>

              <div className="grid grid-cols-3 gap-3 mb-8">
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-jade)] tabular-nums">{confirmed}</p>
                  <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Confirmados</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-gold)] tabular-nums">{pending}</p>
                  <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Pendentes</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-ember)] tabular-nums">{formatBRL(totalPaidCents)}</p>
                  <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Investido</p>
                </div>
              </div>

              {loadingNumbers ? (
                <div className="flex items-center justify-center py-12">
                  <div className="w-6 h-6 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
                </div>
              ) : numbers.length === 0 ? (
                <div className="bg-white rounded-2xl border border-black/[0.06] p-8 text-center">
                  <p className="text-sm text-[var(--color-midnight)]/45 mb-4">
                    Você ainda não tem números nesta campanha.
                  </p>
                  <a
                    href="/participar"
                    className="inline-flex px-6 py-3 rounded-xl font-bold text-sm bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white"
                  >
                    Participar agora
                  </a>
                </div>
              ) : (
                <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
                  <div className="px-5 py-3.5 border-b border-black/[0.04] flex items-center justify-between">
                    <h2 className="text-sm font-semibold text-[var(--color-midnight)]">Seus números</h2>
                    <span className="text-xs text-[var(--color-midnight)]/35">{numbers.length} total</span>
                  </div>
                  <div className="divide-y divide-black/[0.03]">
                    {numbers.map((n) => {
                      const status = statusLabel[n.status] ?? {
                        text: n.status,
                        color: "bg-black/5 text-black/40",
                      };
                      return (
                        <div key={n.numberValue} className="flex items-center justify-between px-5 py-3.5 hover:bg-black/[0.01] transition-colors">
                          <span className="font-mono text-lg font-bold text-[var(--color-midnight)] tabular-nums tracking-wider">
                            {String(n.numberValue).padStart(5, "0")}
                          </span>
                          <span className={`text-[10px] sm:text-xs font-semibold px-2.5 py-1 rounded-full ${status.color}`}>
                            {status.text}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              <div className="mt-8 flex flex-col sm:flex-row gap-3">
                <a
                  href="/participar"
                  className="flex-1 flex items-center justify-center gap-2 py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white animate-cta-beacon active:scale-[0.97] transition-transform"
                >
                  Comprar mais números
                </a>
                <a
                  href="/indicar"
                  className="flex-1 flex items-center justify-center gap-2 py-4 rounded-xl font-semibold text-base border border-black/[0.08] text-[var(--color-midnight)]/60 hover:text-[var(--color-midnight)] hover:bg-black/[0.02] transition-all"
                >
                  Indicar amigos
                </a>
              </div>

              <div className="mt-8 bg-[var(--color-midnight)] rounded-xl p-5 text-center">
                <p className="text-sm text-white/40 mb-1">Sorteio pela Loteria Federal</p>
                <p className="font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-gold)]">20 de dezembro de 2026</p>
                <p className="text-xs text-white/25 mt-2">
                  Resultado auditável · Fórmula publicada antes do encerramento
                </p>
              </div>

              {demoMode && (
                <div className="mt-6 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-4 py-3 text-center">
                  <p className="text-xs font-semibold text-[var(--color-iris)]">Modo demonstração</p>
                  <p className="mt-0.5 text-[10px] text-[var(--color-ink-soft)]">
                    Os números exibidos são simulados. Este painel não constitui comprovante de participação real nem direito a premiação.
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

"use client";

import { useState } from "react";

export default function IndicarPage() {
  const [phone, setPhone] = useState("");
  const [authenticated, setAuthenticated] = useState(false);
  const [copied, setCopied] = useState(false);

  const referralLink = "https://acao.exemplo.com/r/abc123";
  const stats = { clicks: 47, sales: 12, earnings: 0 };

  function handleLookup() {
    if (phone.trim().length >= 10) {
      setAuthenticated(true);
    }
  }

  function handleCopy() {
    navigator.clipboard.writeText(referralLink).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="min-h-dvh bg-[var(--color-cream)]">
      {/* Header */}
      <header className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <a href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-[var(--color-cream)] hover:text-[var(--color-lime)] transition-colors">
            &larr; Ação entre Amigos
          </a>
          <a href="/rankings" className="text-sm font-semibold text-[var(--color-lime)] hover:text-[var(--color-lime)]/80 transition-colors">
            Rankings &rarr;
          </a>
        </div>
      </header>

      <main className="px-4 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-lg mx-auto">
          {!authenticated ? (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Indicar amigos
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Compartilhe seu link e concorra a prêmios extras no ranking de indicações.
              </p>

              <div className="bg-white rounded-2xl border border-black/[0.06] p-6 md:p-8">
                <label htmlFor="phone" className="block text-sm font-medium text-[var(--color-charcoal)] mb-2">
                  Seu WhatsApp
                </label>
                <input
                  id="phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="(11) 99999-9999"
                  className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-[var(--color-cream)] text-[var(--color-charcoal)] placeholder:text-[var(--color-charcoal)]/30 outline-none focus:border-[var(--color-terracotta)]/40 focus:ring-2 focus:ring-[var(--color-terracotta)]/10 transition-all mb-4 text-lg"
                />
                <button
                  onClick={handleLookup}
                  disabled={phone.trim().length < 10}
                  className="w-full py-4 rounded-xl font-bold text-base bg-[var(--color-terracotta)] text-white hover:bg-[var(--color-terracotta-dark)] active:scale-[0.98] transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Ver meu link de indicação
                </button>
              </div>
            </div>
          ) : (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Seu link de indicação
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Compartilhe para subir no ranking e ganhar prêmios.
              </p>

              {/* Link to share */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 md:p-6 mb-6">
                <div className="flex items-center gap-3 bg-[var(--color-cream)] rounded-xl px-4 py-3 mb-4">
                  <span className="flex-1 text-sm text-[var(--color-charcoal)] truncate font-mono tabular-nums">
                    {referralLink}
                  </span>
                  <button
                    onClick={handleCopy}
                    className={`shrink-0 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
                      copied
                        ? "bg-emerald-500 text-white"
                        : "bg-[var(--color-terracotta)] text-white hover:bg-[var(--color-terracotta-dark)]"
                    }`}
                  >
                    {copied ? "Copiado!" : "Copiar"}
                  </button>
                </div>

                {/* Share buttons */}
                <div className="grid grid-cols-2 gap-3">
                  <a
                    href={`https://wa.me/?text=${encodeURIComponent("Participe da Ação entre Amigos! Concorra a R$ 18.500 em prêmios: " + referralLink)}`}
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
                  <button
                    onClick={handleCopy}
                    className="flex items-center justify-center gap-2 py-3 rounded-xl bg-[var(--color-charcoal)]/[0.06] text-[var(--color-charcoal)]/70 font-semibold text-sm hover:bg-[var(--color-charcoal)]/10 transition-colors"
                  >
                    <svg viewBox="0 0 20 20" fill="none" className="w-4.5 h-4.5" aria-hidden="true">
                      <rect x="5" y="5" width="10" height="12" rx="2" stroke="currentColor" strokeWidth="1.5" />
                      <path d="M8 5V3.5A1.5 1.5 0 019.5 2h5A1.5 1.5 0 0116 3.5v9a1.5 1.5 0 01-1.5 1.5H13" stroke="currentColor" strokeWidth="1.5" />
                    </svg>
                    Copiar link
                  </button>
                </div>
              </div>

              {/* Stats */}
              <div className="grid grid-cols-3 gap-3 mb-6">
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-charcoal)] tabular-nums">{stats.clicks}</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Cliques</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-emerald-600 tabular-nums">{stats.sales}</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Vendas</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-terracotta)] tabular-nums">12º</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Ranking</p>
                </div>
              </div>

              {/* Prizes info */}
              <div className="bg-[var(--color-charcoal)] rounded-2xl p-5 md:p-6">
                <h3 className="font-[family-name:var(--font-heading)] text-base font-semibold text-[var(--color-cream)] mb-4">
                  Prêmios por indicação
                </h3>
                <div className="space-y-2.5">
                  {[
                    { pos: "1º", prize: "R$ 1.000" },
                    { pos: "2º", prize: "R$ 750" },
                    { pos: "3º", prize: "R$ 500" },
                  ].map((p) => (
                    <div key={p.pos} className="flex items-center justify-between text-sm">
                      <span className="text-[var(--color-cream)]/55">{p.pos} lugar em vendas concluídas</span>
                      <span className="font-bold text-[var(--color-lime)]">{p.prize}</span>
                    </div>
                  ))}
                </div>
                <a href="/rankings" className="inline-block mt-4 text-xs text-[var(--color-lime)]/60 hover:text-[var(--color-lime)] transition-colors">
                  Ver ranking completo &rarr;
                </a>
              </div>

              <p className="mt-6 text-center text-xs text-[var(--color-charcoal)]/30">
                Modo demonstração · Link e dados simulados
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

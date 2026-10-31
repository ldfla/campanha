"use client";

import { useState } from "react";

function formatBRL(cents: number): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(cents / 100);
}

interface NumberEntry {
  value: string;
  purchasedAt: string;
  status: "confirmed" | "pending" | "surprise";
}

const MOCK_NUMBERS: NumberEntry[] = [
  { value: "04821", purchasedAt: "28/10/2026 14:32", status: "confirmed" },
  { value: "17390", purchasedAt: "28/10/2026 14:32", status: "confirmed" },
  { value: "52064", purchasedAt: "28/10/2026 14:32", status: "confirmed" },
  { value: "83712", purchasedAt: "28/10/2026 14:32", status: "confirmed" },
  { value: "09445", purchasedAt: "28/10/2026 14:32", status: "confirmed" },
  { value: "61238", purchasedAt: "29/10/2026 09:15", status: "confirmed" },
  { value: "33901", purchasedAt: "29/10/2026 09:15", status: "confirmed" },
  { value: "78566", purchasedAt: "29/10/2026 09:15", status: "confirmed" },
  { value: "25103", purchasedAt: "29/10/2026 09:15", status: "pending" },
  { value: "90847", purchasedAt: "29/10/2026 09:15", status: "pending" },
];

const statusLabel: Record<NumberEntry["status"], { text: string; color: string }> = {
  confirmed: { text: "Confirmado", color: "bg-emerald-500/10 text-emerald-700" },
  pending: { text: "Aguardando Pix", color: "bg-amber-500/10 text-amber-700" },
  surprise: { text: "Surpresa!", color: "bg-[var(--color-lime)]/20 text-[var(--color-charcoal)]" },
};

export default function MeusNumerosPage() {
  const [phone, setPhone] = useState("");
  const [authenticated, setAuthenticated] = useState(false);
  const [numbers] = useState<NumberEntry[]>(MOCK_NUMBERS);

  const confirmed = numbers.filter((n) => n.status === "confirmed").length;
  const pending = numbers.filter((n) => n.status === "pending").length;
  const totalPaid = confirmed * 56;

  function handleLookup() {
    if (phone.trim().length >= 10) {
      setAuthenticated(true);
    }
  }

  return (
    <div className="min-h-dvh bg-[var(--color-cream)]">
      {/* Header */}
      <header className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <a href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-[var(--color-cream)] hover:text-[var(--color-lime)] transition-colors">
            &larr; Ação entre Amigos
          </a>
          <a href="/participar" className="text-sm font-semibold text-[var(--color-lime)] hover:text-[var(--color-lime)]/80 transition-colors">
            Comprar mais &rarr;
          </a>
        </div>
      </header>

      <main className="px-4 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-2xl mx-auto">
          {!authenticated ? (
            /* Lookup form */
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Meus números
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Informe seu WhatsApp para ver seus números da sorte.
              </p>

              <div className="bg-white rounded-2xl border border-black/[0.06] p-6 md:p-8">
                <label htmlFor="phone" className="block text-sm font-medium text-[var(--color-charcoal)] mb-2">
                  Número de WhatsApp
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
                  Consultar meus números
                </button>
                <p className="mt-3 text-xs text-[var(--color-charcoal)]/35 text-center">
                  Modo demonstração — em produção, um código de verificação será enviado
                </p>
              </div>
            </div>
          ) : (
            /* Results */
            <div className="animate-slide-in">
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)]">
                    Meus números
                  </h1>
                  <p className="text-sm text-[var(--color-charcoal)]/50 mt-1">
                    WhatsApp: {phone}
                  </p>
                </div>
                <button
                  onClick={() => setAuthenticated(false)}
                  className="text-sm text-[var(--color-terracotta)] hover:underline"
                >
                  Trocar
                </button>
              </div>

              {/* Stats */}
              <div className="grid grid-cols-3 gap-3 mb-8">
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-charcoal)] tabular-nums">{confirmed}</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Confirmados</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-amber-600 tabular-nums">{pending}</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Pendentes</p>
                </div>
                <div className="bg-white rounded-xl border border-black/[0.04] p-4 text-center">
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-terracotta)] tabular-nums">{formatBRL(totalPaid)}</p>
                  <p className="text-xs text-[var(--color-charcoal)]/50 mt-0.5">Investido</p>
                </div>
              </div>

              {/* Numbers grid */}
              <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
                <div className="px-5 py-3.5 border-b border-black/[0.04] flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-[var(--color-charcoal)]">Seus números</h2>
                  <span className="text-xs text-[var(--color-charcoal)]/40">{numbers.length} total</span>
                </div>
                <div className="divide-y divide-black/[0.03]">
                  {numbers.map((n) => {
                    const status = statusLabel[n.status];
                    return (
                      <div key={n.value} className="flex items-center justify-between px-5 py-3.5 hover:bg-black/[0.01] transition-colors">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-lg font-bold text-[var(--color-charcoal)] tabular-nums tracking-wider">
                            {n.value}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-[var(--color-charcoal)]/35 hidden sm:block tabular-nums">
                            {n.purchasedAt}
                          </span>
                          <span className={`text-[10px] sm:text-xs font-semibold px-2.5 py-1 rounded-full ${status.color}`}>
                            {status.text}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Actions */}
              <div className="mt-8 flex flex-col sm:flex-row gap-3">
                <a
                  href="/participar"
                  className="flex-1 flex items-center justify-center gap-2 py-4 rounded-xl font-bold text-base bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.3)] hover:brightness-105 active:scale-[0.98] transition-all"
                >
                  Comprar mais números &rarr;
                </a>
                <a
                  href="/indicar"
                  className="flex-1 flex items-center justify-center gap-2 py-4 rounded-xl font-semibold text-base border border-black/[0.08] text-[var(--color-charcoal)] hover:bg-black/[0.02] transition-colors"
                >
                  Indicar amigos
                </a>
              </div>

              {/* Sorteio info */}
              <div className="mt-8 bg-[var(--color-charcoal)] rounded-xl p-5 text-center">
                <p className="text-sm text-[var(--color-cream)]/50 mb-1">Sorteio pela Loteria Federal</p>
                <p className="font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-lime)]">20 de dezembro de 2026</p>
                <p className="text-xs text-[var(--color-cream)]/30 mt-2">
                  Resultado auditável · Fórmula publicada antes do encerramento
                </p>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

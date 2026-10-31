"use client";

import { useState } from "react";
import Link from "next/link";

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
  confirmed: { text: "Confirmado", color: "bg-[var(--color-jade)]/10 text-[var(--color-jade)]" },
  pending: { text: "Aguardando Pix", color: "bg-[var(--color-gold)]/10 text-amber-700" },
  surprise: { text: "Surpresa!", color: "bg-[var(--color-iris)]/10 text-[var(--color-iris)]" },
};

export default function MeusNumerosPage() {
  const [surname, setSurname] = useState("");
  const [phone, setPhone] = useState("");
  const [authenticated, setAuthenticated] = useState(false);
  const [numbers] = useState<NumberEntry[]>(MOCK_NUMBERS);

  const confirmed = numbers.filter((n) => n.status === "confirmed").length;
  const pending = numbers.filter((n) => n.status === "pending").length;
  const totalPaid = confirmed * 56;

  function handleLookup() {
    if (surname.trim().length >= 2 && phone.trim().length >= 10) {
      setAuthenticated(true);
    }
  }

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
            Ação entre Amigos
          </Link>
          <a href="/participar" className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors">
            Comprar mais
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-2xl mx-auto">
          {!authenticated ? (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Meus números
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Informe seu sobrenome e WhatsApp para ver seus números da sorte.
              </p>

              <div className="bg-white rounded-2xl border border-black/[0.06] p-6 md:p-8">
                <div className="space-y-4 mb-5">
                  <div>
                    <label htmlFor="surname" className="block text-sm font-medium text-[var(--color-midnight)] mb-2">
                      Sobrenome
                    </label>
                    <input
                      id="surname"
                      type="text"
                      value={surname}
                      onChange={(e) => setSurname(e.target.value)}
                      placeholder="Seu sobrenome"
                      className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all text-lg"
                    />
                  </div>
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
                      className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all text-lg"
                    />
                  </div>
                </div>
                <button
                  onClick={handleLookup}
                  disabled={surname.trim().length < 2 || phone.trim().length < 10}
                  className="w-full py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white shadow-[0_4px_24px_rgba(255,99,71,0.3)] hover:shadow-[0_8px_32px_rgba(255,99,71,0.45)] active:scale-[0.98] transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Consultar meus números
                </button>
                <p className="mt-3 text-xs text-[var(--color-midnight)]/30 text-center">
                  Modo demonstração — em produção, um código de verificação será enviado
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
                    {surname} · WhatsApp: {phone}
                  </p>
                </div>
                <button
                  onClick={() => setAuthenticated(false)}
                  className="text-sm text-[var(--color-iris)] hover:underline"
                >
                  Trocar
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
                  <p className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-ember)] tabular-nums">{formatBRL(totalPaid)}</p>
                  <p className="text-xs text-[var(--color-midnight)]/40 mt-0.5">Investido</p>
                </div>
              </div>

              <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
                <div className="px-5 py-3.5 border-b border-black/[0.04] flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-[var(--color-midnight)]">Seus números</h2>
                  <span className="text-xs text-[var(--color-midnight)]/35">{numbers.length} total</span>
                </div>
                <div className="divide-y divide-black/[0.03]">
                  {numbers.map((n) => {
                    const status = statusLabel[n.status];
                    return (
                      <div key={n.value} className="flex items-center justify-between px-5 py-3.5 hover:bg-black/[0.01] transition-colors">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-lg font-bold text-[var(--color-midnight)] tabular-nums tracking-wider">
                            {n.value}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-[var(--color-midnight)]/30 hidden sm:block tabular-nums">
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

              <p className="mt-6 text-center text-xs text-[var(--color-midnight)]/25">
                Modo demonstração · Dados simulados
              </p>
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
          </nav>
        </div>
      </footer>
    </div>
  );
}

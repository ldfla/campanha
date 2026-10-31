"use client";

import { useState } from "react";
import { UrgencyBar } from "@/components/urgency-bar";

const PRICE_CENTS = 56;
const QUICK_OPTIONS = [1, 5, 10, 25, 50, 100];

function formatBRL(cents: number): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(cents / 100);
}

export default function ParticiparPage() {
  const [qty, setQty] = useState(10);
  const [name, setName] = useState("");
  const [surname, setSurname] = useState("");
  const [whatsapp, setWhatsapp] = useState("");
  const [step, setStep] = useState<"choose" | "register" | "payment">("choose");

  const total = qty * PRICE_CENTS;

  function handleNext() {
    if (step === "choose" && qty > 0) {
      setStep("register");
    } else if (step === "register" && name.trim() && surname.trim() && whatsapp.trim()) {
      setStep("payment");
    }
  }

  function handleBack() {
    if (step === "register") setStep("choose");
    if (step === "payment") setStep("register");
  }

  return (
    <>
      <div className="sticky top-0 z-50">
        <UrgencyBar />
      </div>

      <div className="min-h-[calc(100dvh-40px)] bg-[var(--color-cream)] px-4 py-8 md:py-16">
        <div className="max-w-lg mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <a href="/" className="font-[family-name:var(--font-heading)] text-lg font-bold text-[var(--color-charcoal)] hover:text-[var(--color-terracotta)] transition-colors">
              &larr; Ação entre Amigos
            </a>
          </div>

          {/* Progress */}
          <div className="flex items-center gap-2 mb-8 px-2">
            {["Números", "Cadastro", "Pagamento"].map((label, i) => {
              const stepIdx = ["choose", "register", "payment"].indexOf(step);
              const isActive = i <= stepIdx;
              return (
                <div key={label} className="flex-1 flex flex-col items-center gap-1.5">
                  <div className={`w-full h-1.5 rounded-full transition-colors ${isActive ? "bg-[var(--color-terracotta)]" : "bg-[var(--color-charcoal)]/10"}`} />
                  <span className={`text-[11px] font-medium transition-colors ${isActive ? "text-[var(--color-terracotta)]" : "text-[var(--color-charcoal)]/30"}`}>
                    {label}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Step 1: Escolha de números */}
          {step === "choose" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Quantos números quer?
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Cada número custa {formatBRL(PRICE_CENTS)}. Escolha quantos quiser.
              </p>

              {/* Quick options */}
              <div className="grid grid-cols-3 gap-2.5 mb-6">
                {QUICK_OPTIONS.map((n) => (
                  <button
                    key={n}
                    onClick={() => setQty(n)}
                    className={`py-3.5 rounded-xl text-center font-semibold text-base transition-all ${
                      qty === n
                        ? "bg-[var(--color-terracotta)] text-white shadow-md"
                        : "bg-white border border-black/[0.06] text-[var(--color-charcoal)] hover:border-[var(--color-terracotta)]/30 hover:bg-[var(--color-terracotta)]/[0.03]"
                    }`}
                  >
                    {n} {n === 1 ? "nº" : "nºs"}
                  </button>
                ))}
              </div>

              {/* Custom input */}
              <div className="flex items-center gap-3 bg-white rounded-xl border border-black/[0.06] px-4 py-3 mb-8">
                <label htmlFor="qty" className="text-sm text-[var(--color-charcoal)]/60 shrink-0">Outro valor:</label>
                <input
                  id="qty"
                  type="number"
                  min={1}
                  max={1000}
                  value={qty}
                  onChange={(e) => setQty(Math.max(1, Math.min(1000, parseInt(e.target.value) || 1)))}
                  className="flex-1 text-right text-lg font-bold text-[var(--color-charcoal)] bg-transparent outline-none tabular-nums [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                />
                <span className="text-sm text-[var(--color-charcoal)]/40">números</span>
              </div>

              {/* Total */}
              <div className="bg-[var(--color-charcoal)] rounded-2xl p-5 mb-6 text-center">
                <p className="text-sm text-[var(--color-cream)]/50 mb-1">Total</p>
                <p className="font-[family-name:var(--font-heading)] text-3xl md:text-4xl font-bold text-[var(--color-lime)] tabular-nums">
                  {formatBRL(total)}
                </p>
                <p className="text-xs text-[var(--color-cream)]/35 mt-1">
                  {qty} {qty === 1 ? "número" : "números"} &times; {formatBRL(PRICE_CENTS)}
                </p>
              </div>

              <button
                onClick={handleNext}
                className="w-full py-4 rounded-xl font-bold text-lg bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.3)] hover:brightness-105 active:scale-[0.98] transition-all"
              >
                Continuar &rarr;
              </button>
            </div>
          )}

          {/* Step 2: Cadastro */}
          {step === "register" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Seus dados
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Precisamos do seu nome e WhatsApp para confirmar a participação.
              </p>

              <div className="space-y-4 mb-8">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-[var(--color-charcoal)] mb-1.5">Nome</label>
                  <input
                    id="name"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Seu primeiro nome"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-charcoal)] placeholder:text-[var(--color-charcoal)]/30 outline-none focus:border-[var(--color-terracotta)]/40 focus:ring-2 focus:ring-[var(--color-terracotta)]/10 transition-all"
                  />
                </div>
                <div>
                  <label htmlFor="surname" className="block text-sm font-medium text-[var(--color-charcoal)] mb-1.5">Sobrenome</label>
                  <input
                    id="surname"
                    type="text"
                    value={surname}
                    onChange={(e) => setSurname(e.target.value)}
                    placeholder="Seu sobrenome"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-charcoal)] placeholder:text-[var(--color-charcoal)]/30 outline-none focus:border-[var(--color-terracotta)]/40 focus:ring-2 focus:ring-[var(--color-terracotta)]/10 transition-all"
                  />
                </div>
                <div>
                  <label htmlFor="whatsapp" className="block text-sm font-medium text-[var(--color-charcoal)] mb-1.5">WhatsApp</label>
                  <input
                    id="whatsapp"
                    type="tel"
                    value={whatsapp}
                    onChange={(e) => setWhatsapp(e.target.value)}
                    placeholder="(11) 99999-9999"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-charcoal)] placeholder:text-[var(--color-charcoal)]/30 outline-none focus:border-[var(--color-terracotta)]/40 focus:ring-2 focus:ring-[var(--color-terracotta)]/10 transition-all"
                  />
                  <p className="mt-1.5 text-xs text-[var(--color-charcoal)]/40">
                    Usado para confirmar seus números e enviar resultados
                  </p>
                </div>
              </div>

              {/* Resumo */}
              <div className="flex items-center justify-between bg-[var(--color-cream-warm)] rounded-xl px-4 py-3 mb-6 text-sm">
                <span className="text-[var(--color-charcoal)]/60">
                  {qty} {qty === 1 ? "número" : "números"}
                </span>
                <span className="font-bold text-[var(--color-charcoal)] tabular-nums">{formatBRL(total)}</span>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={handleBack}
                  className="px-6 py-4 rounded-xl font-semibold text-base border border-black/[0.08] text-[var(--color-charcoal)]/70 hover:bg-black/[0.02] transition-colors"
                >
                  &larr; Voltar
                </button>
                <button
                  onClick={handleNext}
                  disabled={!name.trim() || !surname.trim() || !whatsapp.trim()}
                  className="flex-1 py-4 rounded-xl font-bold text-lg bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.3)] hover:brightness-105 active:scale-[0.98] transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:brightness-100"
                >
                  Ir para pagamento &rarr;
                </button>
              </div>
            </div>
          )}

          {/* Step 3: Pagamento (DEMO) */}
          {step === "payment" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-charcoal)] mb-2">
                Pagamento via Pix
              </h1>
              <p className="text-sm text-[var(--color-charcoal)]/55 mb-8">
                Escaneie o QR Code ou copie o código para pagar.
              </p>

              {/* Resumo do pedido */}
              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
                <div className="flex items-center justify-between mb-3 pb-3 border-b border-black/[0.04]">
                  <span className="text-sm text-[var(--color-charcoal)]/60">Participante</span>
                  <span className="text-sm font-medium text-[var(--color-charcoal)]">{name} {surname}</span>
                </div>
                <div className="flex items-center justify-between mb-3 pb-3 border-b border-black/[0.04]">
                  <span className="text-sm text-[var(--color-charcoal)]/60">Números</span>
                  <span className="text-sm font-medium text-[var(--color-charcoal)]">{qty}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-[var(--color-charcoal)]">Total</span>
                  <span className="font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-terracotta)] tabular-nums">{formatBRL(total)}</span>
                </div>
              </div>

              {/* QR Code placeholder (DEMO) */}
              <div className="bg-[var(--color-charcoal)] rounded-2xl p-8 mb-6 text-center">
                <div className="inline-block bg-white rounded-xl p-4 mb-4">
                  <div className="w-40 h-40 bg-[var(--color-charcoal)]/5 rounded-lg flex items-center justify-center">
                    <span className="text-4xl" aria-hidden="true">📱</span>
                  </div>
                </div>
                <p className="text-[var(--color-cream)]/50 text-sm mb-3">
                  QR Code Pix
                </p>
                <div className="inline-flex items-center gap-2 bg-[var(--color-lime)]/10 text-[var(--color-lime)] text-xs font-bold uppercase tracking-wider px-3 py-1.5 rounded-full">
                  <span className="w-2 h-2 rounded-full bg-[var(--color-lime)] animate-pulse-dot" />
                  Modo demonstração
                </div>
              </div>

              {/* Copy code */}
              <button
                className="w-full py-3.5 rounded-xl font-semibold text-sm bg-white border border-black/[0.08] text-[var(--color-charcoal)] hover:bg-black/[0.01] active:scale-[0.99] transition-all mb-4"
              >
                Copiar código Pix
              </button>

              <button
                onClick={handleBack}
                className="w-full py-3 text-center text-sm text-[var(--color-charcoal)]/50 hover:text-[var(--color-charcoal)]/80 transition-colors"
              >
                &larr; Voltar para cadastro
              </button>

              <p className="mt-6 text-center text-xs text-[var(--color-charcoal)]/30">
                Após o pagamento, seus números serão confirmados via WhatsApp.
                <br />
                Pagamento processado com segurança.
              </p>
            </div>
          )}

          {/* Footer links */}
          <div className="mt-12 pt-6 border-t border-[var(--color-charcoal)]/8 flex items-center justify-center gap-4 text-xs text-[var(--color-charcoal)]/35">
            <a href="/regulamento" className="hover:text-[var(--color-charcoal)]/60 transition-colors">Regulamento</a>
            <span aria-hidden="true">·</span>
            <a href="/meus-numeros" className="hover:text-[var(--color-charcoal)]/60 transition-colors">Meus números</a>
            <span aria-hidden="true">·</span>
            <a href="/indicar" className="hover:text-[var(--color-charcoal)]/60 transition-colors">Indicar amigos</a>
          </div>
        </div>
      </div>
    </>
  );
}

"use client";

import { useState } from "react";
import Link from "next/link";
import { UrgencyBar } from "@/components/urgency-bar";
import { Countdown } from "@/components/countdown";
import { useAuth } from "@/lib/use-auth";
import {
  participations,
  payments,
  type PaymentOrderResponse,
  ApiError,
} from "@/lib/api-client";
import {
  COMBOS,
  PRICE_UNIT_CENTS,
  isSoldOut,
  isDemoMode,
  formatBRL,
} from "@/lib/campaign-config";

const DEMO_CAMPAIGN_ID = "demo";

export default function ParticiparPage() {
  const { user, register } = useAuth();
  const [selectedCombo, setSelectedCombo] = useState<string>("torcida");
  const soldOut = isSoldOut();
  const demoMode = isDemoMode();
  const [customQty, setCustomQty] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [surname, setSurname] = useState("");
  const [whatsapp, setWhatsapp] = useState("");
  const [step, setStep] = useState<"choose" | "register" | "payment">("choose");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paymentOrder, setPaymentOrder] = useState<PaymentOrderResponse | null>(null);
  const [assignedNumbers, setAssignedNumbers] = useState<number[]>([]);

  const combo = COMBOS.find((c) => c.id === selectedCombo);
  const isCustom = customQty !== null;
  const qty = isCustom ? customQty : (combo?.qty ?? 1);
  const total = isCustom ? customQty * PRICE_UNIT_CENTS : (combo?.totalCents ?? PRICE_UNIT_CENTS);
  const unitPrice = isCustom ? PRICE_UNIT_CENTS : (combo?.unitCents ?? PRICE_UNIT_CENTS);

  function selectCombo(id: string) {
    setSelectedCombo(id);
    setCustomQty(null);
  }

  async function handleNext() {
    if (step === "choose" && qty > 0 && !soldOut) {
      setStep("register");
    } else if (step === "register" && name.trim() && surname.trim() && whatsapp.trim()) {
      setSubmitting(true);
      setError(null);
      try {
        const phoneFormatted = whatsapp.replace(/\D/g, "");
        const phoneWithCountry = phoneFormatted.startsWith("55")
          ? phoneFormatted
          : `55${phoneFormatted}`;

        if (!user) {
          await register(name.trim(), surname.trim(), phoneWithCountry);
        }

        const idempotencyKey = crypto.randomUUID();
        const allocation = await participations.request(
          DEMO_CAMPAIGN_ID,
          idempotencyKey,
          qty,
        );
        setAssignedNumbers(allocation.numbersAssigned);

        const order = await payments.createOrder(total, allocation.entryId);
        setPaymentOrder(order);
        setStep("payment");
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.detail);
        } else {
          setError("Ocorreu um erro. Tente novamente.");
        }
      } finally {
        setSubmitting(false);
      }
    }
  }

  function handleBack() {
    setError(null);
    if (step === "register") setStep("choose");
    if (step === "payment") setStep("register");
  }

  return (
    <>
      <div className="sticky top-0 z-50">
        <UrgencyBar />
      </div>

      <div className="min-h-[calc(100dvh-40px)] bg-[var(--color-snow)] px-5 py-8 md:py-16">
        <div className="max-w-lg mx-auto">
          <div className="text-center mb-8">
            <Link href="/" className="font-[family-name:var(--font-heading)] text-lg font-bold text-[var(--color-midnight)] hover:text-[var(--color-ember)] transition-colors">
              Ação entre Amigos
            </Link>
          </div>

          {/* Progress */}
          <div className="flex items-center gap-2 mb-8 px-2">
            {["Números", "Cadastro", "Pagamento"].map((label, i) => {
              const stepIdx = ["choose", "register", "payment"].indexOf(step);
              const isActive = i <= stepIdx;
              return (
                <div key={label} className="flex-1 flex flex-col items-center gap-1.5">
                  <div className={`w-full h-1.5 rounded-full transition-colors ${isActive ? "bg-gradient-to-r from-[var(--color-iris)] to-[var(--color-jade)]" : "bg-[var(--color-midnight)]/10"}`} />
                  <span className={`text-[11px] font-medium transition-colors ${isActive ? "text-[var(--color-iris)]" : "text-[var(--color-midnight)]/30"}`}>
                    {label}
                  </span>
                </div>
              );
            })}
          </div>

          {error && (
            <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {step === "choose" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Escolha seu combo
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Quanto maior o combo, menor o valor por número.
              </p>

              {/* Combo cards */}
              <div className="space-y-3 mb-6">
                {COMBOS.map((c) => {
                  const isSelected = selectedCombo === c.id && !isCustom;
                  const saving = c.qty > 1 ? Math.round((1 - c.unitCents / PRICE_UNIT_CENTS) * 100) : 0;
                  return (
                    <button
                      key={c.id}
                      onClick={() => selectCombo(c.id)}
                      disabled={soldOut}
                      className={`relative w-full text-left rounded-2xl p-5 transition-all ${
                        isSelected
                          ? c.popular
                            ? "bg-gradient-to-br from-[var(--color-iris)]/[0.08] to-[var(--color-jade)]/[0.06] border-2 border-[var(--color-iris)]/30 shadow-[0_4px_20px_rgba(124,92,252,0.12)]"
                            : "bg-white border-2 border-[var(--color-iris)]/25 shadow-md"
                          : "bg-white border border-black/[0.06] hover:border-black/[0.12] hover:shadow-sm"
                      } ${soldOut ? "opacity-60 cursor-not-allowed" : ""}`}
                    >
                      {c.badge && (
                        <span className={`absolute -top-2.5 left-5 text-[10px] font-bold px-3 py-1 rounded-full ${
                          c.popular
                            ? "bg-[var(--color-iris)] text-white"
                            : "bg-[var(--color-gold)] text-amber-900"
                        }`}>
                          {c.badge}
                        </span>
                      )}
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="font-[family-name:var(--font-heading)] text-base font-semibold text-[var(--color-midnight)] mb-0.5">
                            {c.label}
                          </h3>
                          <p className="text-sm text-[var(--color-midnight)]/40">
                            {c.qty.toLocaleString("pt-BR")} números · {formatBRL(c.unitCents)}/cada
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-midnight)] tabular-nums">
                            {formatBRL(c.totalCents)}
                          </p>
                          {saving > 0 && (
                            <span className="text-[10px] font-semibold text-[var(--color-jade)]">
                              -{saving}% por número
                            </span>
                          )}
                        </div>
                      </div>
                      {isSelected && (
                        <div className="absolute top-1/2 -translate-y-1/2 right-4">
                          <div className="w-5 h-5 rounded-full bg-[var(--color-iris)] flex items-center justify-center">
                            <svg viewBox="0 0 16 16" fill="none" className="w-3 h-3">
                              <path d="M3.5 8.5L6.5 11.5L12.5 5" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </div>
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>

              {/* Custom qty */}
              {!soldOut && (
                <div className="mb-6">
                  <button
                    onClick={() => setCustomQty(customQty === null ? 1 : null)}
                    className="text-sm text-[var(--color-iris)] font-medium hover:underline"
                  >
                    {isCustom ? "Voltar para combos" : "Escolher quantidade personalizada"}
                  </button>
                  {isCustom && (
                    <div className="flex items-center gap-3 bg-white rounded-xl border border-black/[0.06] px-4 py-3 mt-3">
                      <label htmlFor="qty" className="text-sm text-[var(--color-midnight)]/50 shrink-0">Quantidade:</label>
                      <input
                        id="qty"
                        type="number"
                        min={1}
                        max={10000}
                        value={customQty}
                        onChange={(e) => setCustomQty(Math.max(1, Math.min(10000, parseInt(e.target.value) || 1)))}
                        className="flex-1 text-right text-lg font-bold text-[var(--color-midnight)] bg-transparent outline-none tabular-nums [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                      />
                      <span className="text-sm text-[var(--color-midnight)]/40">nºs</span>
                    </div>
                  )}
                </div>
              )}

              {/* Total */}
              <div className="bg-[var(--color-midnight)] rounded-2xl p-5 mb-6 text-center">
                <p className="text-sm text-white/40 mb-1">Total</p>
                <p className="font-[family-name:var(--font-heading)] text-3xl md:text-4xl font-bold text-[var(--color-gold)] tabular-nums">
                  {formatBRL(total)}
                </p>
                <p className="text-xs text-white/25 mt-1 tabular-nums">
                  {qty.toLocaleString("pt-BR")} {qty === 1 ? "número" : "números"} &times; {formatBRL(unitPrice)}
                </p>
              </div>

              {soldOut ? (
                <span className="block w-full py-4 rounded-xl font-bold text-lg text-center bg-[var(--color-ink-soft)]/40 text-white/60 cursor-not-allowed">
                  Esgotado
                </span>
              ) : (
                <button
                  onClick={handleNext}
                  className="w-full py-4 rounded-xl font-bold text-lg bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white animate-cta-beacon hover:scale-[1.02] active:scale-[0.97] transition-transform"
                >
                  Garantir meus números
                </button>
              )}

              <div className="mt-8">
                <Countdown />
              </div>
            </div>
          )}

          {step === "register" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Seus dados
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Precisamos do seu nome e WhatsApp para confirmar a participação.
              </p>

              <div className="space-y-4 mb-8">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-[var(--color-midnight)] mb-1.5">Nome</label>
                  <input id="name" type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="Seu primeiro nome" autoComplete="given-name"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all" />
                </div>
                <div>
                  <label htmlFor="surname" className="block text-sm font-medium text-[var(--color-midnight)] mb-1.5">Sobrenome</label>
                  <input id="surname" type="text" value={surname} onChange={(e) => setSurname(e.target.value)} placeholder="Seu sobrenome" autoComplete="family-name"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all" />
                </div>
                <div>
                  <label htmlFor="whatsapp" className="block text-sm font-medium text-[var(--color-midnight)] mb-1.5">WhatsApp</label>
                  <input id="whatsapp" type="tel" value={whatsapp} onChange={(e) => setWhatsapp(e.target.value)} placeholder="(11) 99999-9999" autoComplete="tel"
                    className="w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-white text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all" />
                  <p className="mt-1.5 text-xs text-[var(--color-midnight)]/35">Usado para confirmar seus números e enviar resultados</p>
                </div>
              </div>

              <div className="flex items-center justify-between bg-[var(--color-snow-warm)] rounded-xl px-4 py-3 mb-6 text-sm">
                <span className="text-[var(--color-midnight)]/50">{qty.toLocaleString("pt-BR")} {qty === 1 ? "número" : "números"}</span>
                <span className="font-bold text-[var(--color-midnight)] tabular-nums">{formatBRL(total)}</span>
              </div>

              <div className="flex gap-3">
                <button onClick={handleBack}
                  className="px-6 py-4 rounded-xl font-semibold text-base border border-black/[0.08] text-[var(--color-midnight)]/60 hover:bg-black/[0.02] transition-colors">
                  Voltar
                </button>
                <button onClick={handleNext} disabled={!name.trim() || !surname.trim() || !whatsapp.trim() || submitting}
                  className="flex-1 py-4 rounded-xl font-bold text-lg bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white animate-cta-beacon hover:scale-[1.02] active:scale-[0.97] transition-transform disabled:opacity-40 disabled:cursor-not-allowed disabled:animate-none">
                  {submitting ? "Processando..." : "Ir para pagamento"}
                </button>
              </div>
            </div>
          )}

          {step === "payment" && (
            <div className="animate-slide-in">
              <h1 className="font-[family-name:var(--font-heading)] text-2xl md:text-3xl font-bold text-[var(--color-midnight)] mb-2">
                Pagamento via Pix
              </h1>
              <p className="text-sm text-[var(--color-midnight)]/45 mb-8">
                Escaneie o QR Code ou copie o código para pagar.
              </p>

              <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
                <div className="flex items-center justify-between mb-3 pb-3 border-b border-black/[0.04]">
                  <span className="text-sm text-[var(--color-midnight)]/50">Participante</span>
                  <span className="text-sm font-medium text-[var(--color-midnight)]">{name} {surname}</span>
                </div>
                <div className="flex items-center justify-between mb-3 pb-3 border-b border-black/[0.04]">
                  <span className="text-sm text-[var(--color-midnight)]/50">Números</span>
                  <span className="text-sm font-medium text-[var(--color-midnight)] tabular-nums">{qty.toLocaleString("pt-BR")}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-[var(--color-midnight)]">Total</span>
                  <span className="font-[family-name:var(--font-heading)] text-xl font-bold text-[var(--color-ember)] tabular-nums">{formatBRL(total)}</span>
                </div>
              </div>

              {assignedNumbers.length > 0 && (
                <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6">
                  <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-3">Seus números reservados</h3>
                  <div className="flex flex-wrap gap-2">
                    {assignedNumbers.map((n) => (
                      <span key={n} className="font-mono text-sm font-bold text-[var(--color-midnight)] bg-[var(--color-snow)] rounded-lg px-2.5 py-1 tabular-nums">
                        {String(n).padStart(5, "0")}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <div className="bg-[var(--color-midnight)] rounded-2xl p-8 mb-6 text-center">
                {paymentOrder?.pixQrCodeBase64 ? (
                  <div className="inline-block bg-white rounded-xl p-4 mb-4">
                    <img
                      src={`data:image/png;base64,${paymentOrder.pixQrCodeBase64}`}
                      alt="QR Code Pix"
                      className="w-40 h-40"
                    />
                  </div>
                ) : (
                  <div className="inline-block bg-white rounded-xl p-4 mb-4">
                    <div className="w-40 h-40 bg-[var(--color-midnight)]/5 rounded-lg flex items-center justify-center">
                      <svg viewBox="0 0 48 48" fill="none" className="w-16 h-16 text-[var(--color-midnight)]/20">
                        <rect x="8" y="8" width="12" height="12" rx="2" stroke="currentColor" strokeWidth="2" />
                        <rect x="28" y="8" width="12" height="12" rx="2" stroke="currentColor" strokeWidth="2" />
                        <rect x="8" y="28" width="12" height="12" rx="2" stroke="currentColor" strokeWidth="2" />
                        <rect x="30" y="30" width="8" height="8" rx="1" stroke="currentColor" strokeWidth="2" />
                      </svg>
                    </div>
                  </div>
                )}
                <p className="text-white/40 text-sm mb-3">QR Code Pix</p>
                {demoMode && (
                  <div className="inline-flex items-center gap-2 bg-[var(--color-iris)]/15 text-[var(--color-iris)] text-xs font-semibold px-3 py-1.5 rounded-full">
                    <span className="w-2 h-2 rounded-full bg-[var(--color-iris)] animate-pulse" />
                    Modo demonstração
                  </div>
                )}
              </div>

              {paymentOrder?.pixCode && (
                <button
                  onClick={() => navigator.clipboard.writeText(paymentOrder.pixCode)}
                  className="w-full py-3.5 rounded-xl font-semibold text-sm bg-white border border-black/[0.08] text-[var(--color-midnight)] hover:bg-black/[0.01] active:scale-[0.99] transition-all mb-4"
                >
                  Copiar código Pix
                </button>
              )}

              {!paymentOrder?.pixCode && (
                <button className="w-full py-3.5 rounded-xl font-semibold text-sm bg-white border border-black/[0.08] text-[var(--color-midnight)]/40 cursor-not-allowed mb-4">
                  Copiar código Pix
                </button>
              )}

              <button onClick={handleBack} className="w-full py-3 text-center text-sm text-[var(--color-midnight)]/40 hover:text-[var(--color-midnight)]/70 transition-colors">
                Voltar para cadastro
              </button>

              <p className="mt-6 text-center text-xs text-[var(--color-midnight)]/25">
                Após o pagamento, seus números serão confirmados via WhatsApp.
              </p>
            </div>
          )}

          <div className="mt-12 pt-6 border-t border-[var(--color-midnight)]/8 flex items-center justify-center gap-4 text-xs text-[var(--color-midnight)]/30">
            <a href="/regulamento" className="hover:text-[var(--color-midnight)]/50 transition-colors">Regulamento</a>
            <a href="/meus-numeros" className="hover:text-[var(--color-midnight)]/50 transition-colors">Meus números</a>
            <a href="/indicar" className="hover:text-[var(--color-midnight)]/50 transition-colors">Indicar amigos</a>
          </div>
        </div>
      </div>
    </>
  );
}

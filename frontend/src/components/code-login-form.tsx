"use client";

import { useState } from "react";
import { useAuth } from "@/lib/use-auth";
import { ApiError } from "@/lib/api-client";
import { isDemoMode, toE164 } from "@/lib/campaign-config";

type Step = "phone" | "code";

/**
 * Login em duas etapas: o WhatsApp só identifica; a sessão nasce após o código recebido.
 * A resposta da solicitação é a mesma para números cadastrados ou não (sem enumeração).
 */
export function CodeLoginForm({ submitLabel }: { submitLabel: string }) {
  const { requestCode, verifyCode } = useAuth();
  const [step, setStep] = useState<Step>("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const demoMode = isDemoMode();
  const phoneValid = phone.replace(/\D/g, "").length >= 10;

  async function handleRequest() {
    if (!phoneValid || busy) return;
    setBusy(true);
    setError(null);
    try {
      const challenge = await requestCode(toE164(phone));
      setNotice(challenge.detail);
      setStep("code");
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Não foi possível enviar o código.");
    } finally {
      setBusy(false);
    }
  }

  async function handleVerify() {
    if (!/^\d{6}$/.test(code) || busy) return;
    setBusy(true);
    setError(null);
    try {
      await verifyCode(toE164(phone), code);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Código inválido ou expirado.");
    } finally {
      setBusy(false);
    }
  }

  const inputClass =
    "w-full px-4 py-3.5 rounded-xl border border-black/[0.08] bg-[var(--color-snow)] text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all text-lg";
  const buttonClass =
    "w-full py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white shadow-[0_4px_24px_rgba(255,99,71,0.3)] hover:shadow-[0_8px_32px_rgba(255,99,71,0.45)] active:scale-[0.98] transition-all disabled:opacity-40 disabled:cursor-not-allowed";

  return (
    <div className="bg-white rounded-2xl border border-black/[0.06] p-6 md:p-8">
      {error && (
        <div role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {step === "phone" ? (
        <>
          <div className="mb-5">
            <label htmlFor="login-phone" className="block text-sm font-medium text-[var(--color-midnight)] mb-2">
              Número de WhatsApp
            </label>
            <input
              id="login-phone"
              type="tel"
              inputMode="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="(11) 99999-9999"
              autoComplete="tel"
              onKeyDown={(e) => e.key === "Enter" && handleRequest()}
              className={inputClass}
            />
          </div>
          <button type="button" onClick={handleRequest} disabled={!phoneValid || busy} className={buttonClass}>
            {busy ? "Enviando código..." : "Receber código"}
          </button>
        </>
      ) : (
        <>
          <p role="status" className="mb-4 text-sm text-[var(--color-midnight)]/60">
            {notice}
          </p>
          <div className="mb-5">
            <label htmlFor="login-code" className="block text-sm font-medium text-[var(--color-midnight)] mb-2">
              Código de verificação (6 dígitos)
            </label>
            <input
              id="login-code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
              onKeyDown={(e) => e.key === "Enter" && handleVerify()}
              className={`${inputClass} tracking-[0.4em] text-center font-mono`}
            />
          </div>
          <button type="button" onClick={handleVerify} disabled={code.length !== 6 || busy} className={buttonClass}>
            {busy ? "Verificando..." : submitLabel}
          </button>
          <button
            type="button"
            onClick={() => { setStep("phone"); setCode(""); setError(null); }}
            className="mt-3 w-full text-sm text-[var(--color-iris)] hover:underline"
          >
            Trocar número
          </button>
        </>
      )}
      {demoMode && (
        <p className="mt-3 text-xs text-[var(--color-midnight)]/40 text-center">
          Modo demonstração — nenhuma mensagem é enviada; use o código de teste 000000.
        </p>
      )}
    </div>
  );
}

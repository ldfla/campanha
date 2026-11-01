"use client";

import { useState } from "react";
import { mockPayments, ApiError } from "@/lib/api-client";

export default function PagamentosAdmin() {
  const [orderId, setOrderId] = useState("");
  const [settling, setSettling] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSettle() {
    if (!orderId.trim()) return;
    setSettling(true);
    setError(null);
    setResult(null);
    try {
      await mockPayments.settle(orderId.trim());
      setResult(`Pedido ${orderId.slice(0, 8)}... liquidado com sucesso.`);
      setOrderId("");
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Falha ao liquidar pedido");
    } finally {
      setSettling(false);
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)]">
          Simulação de pagamento
        </h1>
        <p className="text-sm text-[var(--color-midnight)]/40 mt-1">
          Liquidar pedidos mock para fins de demonstração (profile local)
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-black/[0.06] p-5 mb-6 max-w-lg">
        <h3 className="text-sm font-semibold text-[var(--color-midnight)] mb-4">Liquidar pedido mock</h3>
        <p className="text-xs text-[var(--color-midnight)]/40 mb-4">
          Informe o ID público do pedido (orderPublicId) gerado no checkout para simular a confirmação de pagamento pelo PSP.
        </p>

        {error && (
          <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {result && (
          <div className="mb-4 rounded-xl border border-[var(--color-jade)]/20 bg-[var(--color-jade)]/[0.06] px-4 py-3 text-sm text-[var(--color-jade)] font-medium">
            {result}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-xs font-medium text-[var(--color-midnight)]/60 mb-1">
            Order Public ID
          </label>
          <input
            type="text"
            value={orderId}
            onChange={(e) => setOrderId(e.target.value)}
            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            className="w-full px-3 py-2.5 rounded-lg border border-black/[0.08] text-sm text-[var(--color-midnight)] bg-[var(--color-snow)] outline-none focus:border-[var(--color-iris)]/40 font-mono"
          />
        </div>

        <button
          onClick={handleSettle}
          disabled={settling || !orderId.trim()}
          className="w-full py-3 rounded-xl font-semibold text-sm bg-[var(--color-ember)] text-white hover:bg-[var(--color-ember)]/90 transition-colors disabled:opacity-50"
        >
          {settling ? "Liquidando..." : "Liquidar pagamento (mock)"}
        </button>
      </div>

      <div className="bg-[var(--color-iris)]/[0.06] border border-[var(--color-iris)]/15 rounded-xl p-4 max-w-lg">
        <p className="text-xs font-semibold text-[var(--color-iris)] mb-1">Como funciona</p>
        <ol className="text-xs text-[var(--color-ink-soft)] space-y-1 list-decimal list-inside">
          <li>Participante faz checkout e recebe um orderPublicId</li>
          <li>Cole o orderPublicId acima e clique em Liquidar</li>
          <li>O PSP mock marca como PAID e o pedido é conciliado</li>
          <li>Os números do participante são confirmados</li>
        </ol>
      </div>
    </div>
  );
}

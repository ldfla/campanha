"use client";

import { useState, useMemo, useCallback } from "react";
import Link from "next/link";
import {
  TOTAL_NUMBERS,
  isDemoMode,
  isSoldOut,
  isSuspended,
} from "@/lib/campaign-config";

const RANGE_SIZE = 1000;
const TOTAL_RANGES = Math.ceil(TOTAL_NUMBERS / RANGE_SIZE);
const COLS = 25;

type NumberStatus = "AVAILABLE" | "CONFIRMED" | "PENDING" | "RESERVED";

interface NumberCell {
  value: number;
  status: NumberStatus;
}

const STATUS_COLORS: Record<NumberStatus, { bg: string; label: string }> = {
  AVAILABLE: { bg: "bg-[var(--color-midnight)]/[0.06]", label: "Disponível" },
  CONFIRMED: { bg: "bg-[var(--color-jade)]", label: "Confirmado" },
  PENDING: { bg: "bg-[var(--color-gold)]", label: "Aguardando" },
  RESERVED: { bg: "bg-[var(--color-iris)]", label: "Reservado" },
};

function generateDemoData(rangeStart: number, rangeEnd: number): NumberCell[] {
  const cells: NumberCell[] = [];
  for (let i = rangeStart; i < rangeEnd && i < TOTAL_NUMBERS; i++) {
    const seed = ((i * 2654435761) >>> 0) % 100;
    let status: NumberStatus = "AVAILABLE";
    if (seed < 35) status = "CONFIRMED";
    else if (seed < 38) status = "PENDING";
    else if (seed < 40) status = "RESERVED";
    cells.push({ value: i, status });
  }
  return cells;
}

function computeStats(cells: NumberCell[]) {
  let available = 0;
  let confirmed = 0;
  let pending = 0;
  let reserved = 0;
  for (const c of cells) {
    if (c.status === "AVAILABLE") available++;
    else if (c.status === "CONFIRMED") confirmed++;
    else if (c.status === "PENDING") pending++;
    else reserved++;
  }
  return { available, confirmed, pending, reserved };
}

export default function NumerosPage() {
  const demoMode = isDemoMode();
  const soldOut = isSoldOut();
  const suspended = isSuspended();
  const [rangeIdx, setRangeIdx] = useState(0);
  const [searchValue, setSearchValue] = useState("");
  const [selectedNumber, setSelectedNumber] = useState<NumberCell | null>(null);

  const rangeStart = rangeIdx * RANGE_SIZE;
  const rangeEnd = Math.min(rangeStart + RANGE_SIZE, TOTAL_NUMBERS);

  const cells = useMemo(
    () => generateDemoData(rangeStart, rangeEnd),
    [rangeStart, rangeEnd],
  );

  const stats = useMemo(() => computeStats(cells), [cells]);

  const globalStats = useMemo(() => {
    let confirmed = 0;
    for (let i = 0; i < TOTAL_NUMBERS; i++) {
      const seed = ((i * 2654435761) >>> 0) % 100;
      if (seed < 35) confirmed++;
      else if (seed < 40) confirmed++;
    }
    return {
      total: TOTAL_NUMBERS,
      sold: confirmed,
      available: TOTAL_NUMBERS - confirmed,
    };
  }, []);

  const handleSearch = useCallback(() => {
    const cleaned = searchValue.replace(/\D/g, "");
    if (cleaned.length === 0 || cleaned.length > 5) return;
    const num = parseInt(cleaned, 10);
    if (num < 0 || num >= TOTAL_NUMBERS) return;
    const targetRange = Math.floor(num / RANGE_SIZE);
    setRangeIdx(targetRange);
    const seed = ((num * 2654435761) >>> 0) % 100;
    let status: NumberStatus = "AVAILABLE";
    if (seed < 35) status = "CONFIRMED";
    else if (seed < 38) status = "PENDING";
    else if (seed < 40) status = "RESERVED";
    setSelectedNumber({ value: num, status });
  }, [searchValue]);

  const rangeLabel = `${String(rangeStart).padStart(5, "0")} – ${String(rangeEnd - 1).padStart(5, "0")}`;

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-4">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <Link
            href="/"
            className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors"
          >
            Ação entre Amigos
          </Link>
          <nav className="flex gap-4 text-sm">
            <a
              href="/meus-numeros"
              className="font-semibold text-white/50 hover:text-white/80 transition-colors"
            >
              Meus números
            </a>
            <a
              href="/participar"
              className="font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors"
            >
              Participar
            </a>
          </nav>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-8 md:py-12">
        <div className="mx-auto max-w-4xl">
          <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)] md:text-3xl">
            Painel de números
          </h1>
          <p className="mt-1 text-sm text-[var(--color-midnight)]/40">
            Acompanhe a disponibilidade dos 100.000 números da campanha.
          </p>

          {demoMode && (
            <div className="mt-4 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-4 py-3">
              <p className="text-sm font-semibold text-[var(--color-iris)]">
                Modo demonstração
              </p>
              <p className="mt-0.5 text-xs text-[var(--color-ink-soft)]">
                Os dados exibidos são simulados. Em produção, os números refletirão participações reais confirmadas.
                Este painel não constitui oferta de venda ou promessa de premiação.
              </p>
            </div>
          )}

          {(soldOut || suspended) && (
            <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
              <p className="text-sm font-semibold text-amber-800">
                {soldOut ? "Todos os números foram vendidos" : "Campanha temporariamente suspensa"}
              </p>
            </div>
          )}

          {/* Stats globais */}
          <div className="mt-6 grid grid-cols-3 gap-3">
            <div className="rounded-xl border border-black/[0.04] bg-white p-4 text-center">
              <p className="font-[family-name:var(--font-heading)] text-xl font-bold tabular-nums text-[var(--color-midnight)] md:text-2xl">
                {globalStats.total.toLocaleString("pt-BR")}
              </p>
              <p className="mt-0.5 text-xs text-[var(--color-midnight)]/40">
                Total
              </p>
            </div>
            <div className="rounded-xl border border-black/[0.04] bg-white p-4 text-center">
              <p className="font-[family-name:var(--font-heading)] text-xl font-bold tabular-nums text-[var(--color-jade)] md:text-2xl">
                {globalStats.sold.toLocaleString("pt-BR")}
              </p>
              <p className="mt-0.5 text-xs text-[var(--color-midnight)]/40">
                Vendidos
              </p>
            </div>
            <div className="rounded-xl border border-black/[0.04] bg-white p-4 text-center">
              <p className="font-[family-name:var(--font-heading)] text-xl font-bold tabular-nums text-[var(--color-ember)] md:text-2xl">
                {globalStats.available.toLocaleString("pt-BR")}
              </p>
              <p className="mt-0.5 text-xs text-[var(--color-midnight)]/40">
                Disponíveis
              </p>
            </div>
          </div>

          {/* Busca */}
          <div className="mt-6 flex gap-2">
            <input
              type="text"
              inputMode="numeric"
              maxLength={5}
              value={searchValue}
              onChange={(e) => {
                setSearchValue(e.target.value.replace(/\D/g, "").slice(0, 5));
                setSelectedNumber(null);
              }}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="Buscar número (ex: 00042)"
              className="flex-1 rounded-xl border border-black/[0.08] bg-white px-4 py-3 text-[var(--color-midnight)] placeholder:text-[var(--color-midnight)]/25 outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all tabular-nums font-mono"
            />
            <button
              onClick={handleSearch}
              disabled={searchValue.length === 0}
              className="shrink-0 rounded-xl bg-[var(--color-midnight)] px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-[var(--color-midnight)]/80 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Buscar
            </button>
          </div>

          {selectedNumber && (
            <div className="mt-3 flex items-center justify-between rounded-xl border border-black/[0.06] bg-white px-4 py-3">
              <div className="flex items-center gap-3">
                <span className="font-mono text-lg font-bold tabular-nums text-[var(--color-midnight)]">
                  {String(selectedNumber.value).padStart(5, "0")}
                </span>
                <span
                  className={`rounded-full px-2.5 py-1 text-[10px] font-semibold text-white ${STATUS_COLORS[selectedNumber.status].bg}`}
                >
                  {STATUS_COLORS[selectedNumber.status].label}
                </span>
              </div>
              <button
                onClick={() => setSelectedNumber(null)}
                className="text-xs text-[var(--color-midnight)]/30 hover:text-[var(--color-midnight)]/60"
              >
                Fechar
              </button>
            </div>
          )}

          {/* Legenda */}
          <div className="mt-6 flex flex-wrap gap-4 text-xs text-[var(--color-midnight)]/60">
            {(Object.entries(STATUS_COLORS) as [NumberStatus, { bg: string; label: string }][]).map(
              ([key, { bg, label }]) => (
                <span key={key} className="flex items-center gap-1.5">
                  <span className={`inline-block h-3 w-3 rounded-sm ${bg}`} />
                  {label}
                </span>
              ),
            )}
          </div>

          {/* Navegação de faixas */}
          <div className="mt-6 flex items-center justify-between">
            <button
              onClick={() => setRangeIdx(Math.max(0, rangeIdx - 1))}
              disabled={rangeIdx === 0}
              className="rounded-lg border border-black/[0.08] px-3 py-2 text-sm font-medium text-[var(--color-midnight)] transition-colors hover:bg-black/[0.02] disabled:opacity-25 disabled:cursor-not-allowed"
            >
              ← Anterior
            </button>
            <div className="text-center">
              <p className="font-mono text-sm font-semibold tabular-nums text-[var(--color-midnight)]">
                {rangeLabel}
              </p>
              <p className="text-[10px] text-[var(--color-midnight)]/30">
                Faixa {rangeIdx + 1} de {TOTAL_RANGES}
              </p>
            </div>
            <button
              onClick={() =>
                setRangeIdx(Math.min(TOTAL_RANGES - 1, rangeIdx + 1))
              }
              disabled={rangeIdx >= TOTAL_RANGES - 1}
              className="rounded-lg border border-black/[0.08] px-3 py-2 text-sm font-medium text-[var(--color-midnight)] transition-colors hover:bg-black/[0.02] disabled:opacity-25 disabled:cursor-not-allowed"
            >
              Próxima →
            </button>
          </div>

          {/* Seletor rápido de faixa */}
          <div className="mt-3">
            <select
              value={rangeIdx}
              onChange={(e) => setRangeIdx(parseInt(e.target.value, 10))}
              className="w-full rounded-xl border border-black/[0.08] bg-white px-4 py-2.5 text-sm text-[var(--color-midnight)] outline-none focus:border-[var(--color-iris)]/40 focus:ring-2 focus:ring-[var(--color-iris)]/10 transition-all"
            >
              {Array.from({ length: TOTAL_RANGES }, (_, i) => {
                const start = String(i * RANGE_SIZE).padStart(5, "0");
                const end = String(
                  Math.min((i + 1) * RANGE_SIZE - 1, TOTAL_NUMBERS - 1),
                ).padStart(5, "0");
                return (
                  <option key={i} value={i}>
                    {start} – {end}
                  </option>
                );
              })}
            </select>
          </div>

          {/* Stats da faixa */}
          <div className="mt-4 flex gap-3 text-xs">
            <span className="text-[var(--color-midnight)]/40">
              Nesta faixa:
            </span>
            <span className="font-medium text-[var(--color-jade)]">
              {stats.confirmed} vendidos
            </span>
            <span className="font-medium text-[var(--color-midnight)]/50">
              {stats.available} disponíveis
            </span>
          </div>

          {/* Grid de números */}
          <div className="mt-4 overflow-x-auto rounded-2xl border border-black/[0.06] bg-white p-3 md:p-4">
            <div
              className="grid gap-[3px] md:gap-1"
              style={{
                gridTemplateColumns: `repeat(${COLS}, minmax(0, 1fr))`,
              }}
            >
              {cells.map((cell) => {
                const isHighlighted =
                  selectedNumber?.value === cell.value;
                return (
                  <button
                    key={cell.value}
                    onClick={() =>
                      setSelectedNumber(
                        selectedNumber?.value === cell.value ? null : cell,
                      )
                    }
                    title={`${String(cell.value).padStart(5, "0")} — ${STATUS_COLORS[cell.status].label}`}
                    className={`aspect-square rounded-[2px] transition-all md:rounded-[3px] ${
                      STATUS_COLORS[cell.status].bg
                    } ${
                      isHighlighted
                        ? "ring-2 ring-[var(--color-ember)] ring-offset-1 scale-150 z-10"
                        : "hover:ring-1 hover:ring-[var(--color-midnight)]/20"
                    }`}
                    aria-label={`Número ${String(cell.value).padStart(5, "0")}: ${STATUS_COLORS[cell.status].label}`}
                  />
                );
              })}
            </div>
          </div>

          {/* Info de privacidade */}
          <div className="mt-6 rounded-xl border border-[var(--color-midnight)]/8 bg-[var(--color-midnight)]/[0.02] px-5 py-4">
            <p className="text-xs text-[var(--color-midnight)]/40 text-center">
              Este painel mostra apenas o status de cada número (disponível, vendido, pendente).
              Nenhuma informação pessoal dos participantes é exibida.
              Para consultar seus próprios números, acesse{" "}
              <a
                href="/meus-numeros"
                className="text-[var(--color-iris)] hover:underline"
              >
                Meus números
              </a>
              .
            </p>
          </div>
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 py-6 md:px-8 lg:px-16">
        <div className="mx-auto flex max-w-4xl items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">
            Ação entre Amigos
          </span>
          <nav className="flex gap-4 text-xs">
            <Link
              href="/"
              className="transition-colors hover:text-white/60"
            >
              Início
            </Link>
            <Link
              href="/regulamento"
              className="transition-colors hover:text-white/60"
            >
              Regulamento
            </Link>
            <Link
              href="/faq"
              className="transition-colors hover:text-white/60"
            >
              FAQ
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}

"use client";

import { useSyncExternalStore } from "react";

const DRAW_DATE = new Date("2026-12-20T15:00:00-03:00");

interface TimeLeft {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

function subscribe(callback: () => void): () => void {
  const interval = setInterval(callback, 1000);
  return () => clearInterval(interval);
}

function getSnapshot(): number {
  return Math.floor(Date.now() / 1000);
}

/** No servidor não há relógio: renderiza nada até a primeira leitura no cliente. */
function getServerSnapshot(): number | null {
  return null;
}

function getTimeLeft(nowMillis: number): TimeLeft {
  const diff = Math.max(0, DRAW_DATE.getTime() - nowMillis);
  return {
    days: Math.floor(diff / (1000 * 60 * 60 * 24)),
    hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
    minutes: Math.floor((diff / (1000 * 60)) % 60),
    seconds: Math.floor((diff / 1000) % 60),
  };
}

function Digit({ value, label }: { value: number; label: string }) {
  return (
    <div className="flex flex-col items-center">
      <span className="font-[family-name:var(--font-heading)] text-2xl font-bold leading-none tabular-nums text-white md:text-3xl">
        {String(value).padStart(2, "0")}
      </span>
      <span className="mt-1.5 text-[10px] text-white/60">{label}</span>
    </div>
  );
}

export function Countdown() {
  const nowSeconds = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  if (nowSeconds === null) {
    return null;
  }

  const time = getTimeLeft(nowSeconds * 1000);

  return (
    <div className="rounded-2xl bg-[var(--color-forest-light)] p-5 md:p-6">
      <p className="mb-4 text-center text-sm text-white/70">Sorteio em</p>
      <div className="flex items-center justify-center gap-4 md:gap-6">
        <Digit value={time.days} label="dias" />
        <span className="text-lg font-light text-white/30">:</span>
        <Digit value={time.hours} label="horas" />
        <span className="text-lg font-light text-white/30">:</span>
        <Digit value={time.minutes} label="min" />
        <span className="text-lg font-light text-white/30">:</span>
        <Digit value={time.seconds} label="seg" />
      </div>
      <p className="mt-3 text-center text-xs text-white/50">
        Loteria Federal · 20 de dezembro de 2026
      </p>
    </div>
  );
}

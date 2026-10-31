"use client";

import { useEffect, useState } from "react";
import { Section } from "@/components/section";

const NAMES = [
  "Ana M.", "Carlos S.", "Julia R.", "Pedro L.", "Fernanda C.",
  "Lucas O.", "Mariana T.", "Gustavo P.", "Camila D.", "Rafael B.",
  "Beatriz N.", "Diego F.", "Larissa K.", "Thiago V.", "Amanda G.",
];

/** Gradientes da paleta: verde, laranja e variações escuras para contraste AA. */
const COLORS = [
  ["#2ECC71", "#1E9E6A"],
  ["#FF6B35", "#D94A15"],
  ["#153B33", "#2ECC71"],
  ["#E0B400", "#B58A00"],
  ["#1E9E6A", "#0B2B26"],
  ["#FF8A65", "#FF6B35"],
] as const;

const QUANTITIES = [1, 1, 2, 1, 3, 5, 1, 2, 10, 1];
const CITIES = [
  "São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba",
  "Salvador", "Brasília", "Fortaleza", "Recife", "Porto Alegre",
];

/** PRNG determinístico: a lista inicial é idêntica no servidor e no cliente. */
function seededRandom(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function pick<T>(arr: readonly T[], random: () => number): T {
  const item = arr[Math.floor(random() * arr.length)];
  if (item === undefined) {
    throw new Error("Lista de sorteio vazia");
  }
  return item;
}

interface Activity {
  id: number;
  name: string;
  qty: number;
  city: string;
  minutes: number;
  color: readonly [string, string];
  fresh: boolean;
}

function initialActivities(): Activity[] {
  const random = seededRandom(20261220);
  return Array.from({ length: 6 }, (_, i) => ({
    id: i,
    name: pick(NAMES, random),
    qty: pick(QUANTITIES, random),
    city: pick(CITIES, random),
    minutes: Math.floor(random() * 15) + 1,
    color: pick(COLORS, random),
    fresh: false,
  }));
}

export function LiveFeed() {
  const [activities, setActivities] = useState<Activity[]>(initialActivities);

  useEffect(() => {
    const interval = setInterval(() => {
      setActivities((prev) => {
        const newItem: Activity = {
          id: Date.now(),
          name: pick(NAMES, Math.random),
          qty: pick(QUANTITIES, Math.random),
          city: pick(CITIES, Math.random),
          minutes: 1,
          color: pick(COLORS, Math.random),
          fresh: true,
        };
        return [newItem, ...prev.slice(0, 5)];
      });
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Section tone="snow">
      <div className="flex w-full items-center justify-center gap-2.5">
        <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
        <h2 className="font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-ink)] md:text-xl">
          Atividade da comunidade
        </h2>
      </div>

      <div className="mx-auto mt-8 w-full max-w-xl space-y-2.5">
        {activities.map((activity) => (
          <div
            key={activity.id}
            className={`flex items-center gap-3 rounded-2xl border border-black/[0.04] bg-white px-3.5 py-3 shadow-sm ${
              activity.fresh ? "animate-slide-in" : ""
            }`}
          >
            <div
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-[13px] font-bold text-white shadow-sm"
              style={{
                backgroundImage: `linear-gradient(135deg, ${activity.color[0]} 0%, ${activity.color[1]} 100%)`,
              }}
              aria-hidden="true"
            >
              {activity.name
                .split(" ")
                .map((part) => part[0])
                .join("")}
            </div>

            <div className="min-w-0 flex-1 text-left">
              <p className="truncate text-sm font-bold text-[var(--color-ink)]">
                {activity.name}
              </p>
              <p className="text-[13px] text-[var(--color-ink-soft)]">
                comprou{" "}
                <strong className="font-semibold text-[var(--color-iris)]">
                  {activity.qty} {activity.qty === 1 ? "número" : "números"}
                </strong>
              </p>
              <p className="text-[11px] tabular-nums text-[var(--color-ink-soft)]/70">
                {activity.city} · {activity.minutes}min atrás
              </p>
            </div>
          </div>
        ))}
      </div>

      <p className="mt-6 text-center text-xs text-[var(--color-ink-soft)]/70">
        Modo demonstração, dados simulados
      </p>
    </Section>
  );
}

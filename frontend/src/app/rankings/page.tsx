import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Rankings — Ação entre Amigos",
  description: "Placar em tempo real: quem mais comprou, quem mais indicou, e os divulgadores mais ativos.",
};

interface RankEntry {
  position: number;
  label: string;
  metric: string;
  prize: string;
}

const buyersRanking: RankEntry[] = [
  { position: 1, label: "Mar... *4821", metric: "247 números", prize: "R$ 2.000" },
  { position: 2, label: "Car... *9033", metric: "189 números", prize: "R$ 1.000" },
  { position: 3, label: "Ana... *5517", metric: "143 números", prize: "R$ 750" },
  { position: 4, label: "Ped... *7702", metric: "98 números", prize: "" },
  { position: 5, label: "Fer... *3388", metric: "72 números", prize: "" },
];

const referrersRanking: RankEntry[] = [
  { position: 1, label: "Jul... *2290", metric: "34 vendas", prize: "R$ 1.000" },
  { position: 2, label: "Luc... *8845", metric: "28 vendas", prize: "R$ 750" },
  { position: 3, label: "Bea... *1166", metric: "19 vendas", prize: "R$ 500" },
  { position: 4, label: "Raf... *6634", metric: "12 vendas", prize: "" },
  { position: 5, label: "Isa... *4477", metric: "9 vendas", prize: "" },
];

const influencersRanking: RankEntry[] = [
  { position: 1, label: "@foto.maria", metric: "1.247 cliques", prize: "R$ 2.000" },
  { position: 2, label: "@lucas.tech", metric: "892 cliques", prize: "R$ 1.000" },
  { position: 3, label: "@camila.art", metric: "634 cliques", prize: "R$ 500" },
];

function RankingTable({ title, subtitle, entries, icon }: { title: string; subtitle: string; entries: RankEntry[]; icon: string }) {
  return (
    <div className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
      <div className="px-5 py-4 md:px-6 md:py-5 border-b border-black/[0.04]">
        <div className="flex items-center gap-3">
          <span className="text-2xl" aria-hidden="true">{icon}</span>
          <div>
            <h2 className="font-[family-name:var(--font-heading)] text-lg font-semibold text-[var(--color-charcoal)]">
              {title}
            </h2>
            <p className="text-xs text-[var(--color-charcoal)]/45 mt-0.5">{subtitle}</p>
          </div>
        </div>
      </div>
      <div className="divide-y divide-black/[0.03]">
        {entries.map((entry) => (
          <div
            key={entry.position + entry.label}
            className={`flex items-center gap-4 px-5 py-3.5 md:px-6 md:py-4 ${entry.position <= 3 ? "bg-[var(--color-cream)]/40" : ""}`}
          >
            <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold shrink-0 ${
              entry.position === 1
                ? "bg-amber-400 text-amber-900"
                : entry.position === 2
                ? "bg-gray-300 text-gray-700"
                : entry.position === 3
                ? "bg-orange-300 text-orange-800"
                : "bg-[var(--color-charcoal)]/[0.06] text-[var(--color-charcoal)]/40"
            }`}>
              {entry.position}
            </span>

            <div className="flex-1 min-w-0">
              <span className="text-sm font-medium text-[var(--color-charcoal)] tabular-nums">
                {entry.label}
              </span>
            </div>

            <span className="text-sm text-[var(--color-charcoal)]/55 tabular-nums shrink-0">
              {entry.metric}
            </span>

            {entry.prize && (
              <span className="text-xs font-bold text-[var(--color-terracotta)] bg-[var(--color-terracotta)]/8 px-2.5 py-1 rounded-full shrink-0">
                {entry.prize}
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

export default function RankingsPage() {
  return (
    <div className="min-h-dvh bg-[var(--color-cream)]">
      {/* Header */}
      <header className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-4">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <a href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-[var(--color-cream)] hover:text-[var(--color-lime)] transition-colors">
            &larr; Ação entre Amigos
          </a>
          <a href="/participar" className="text-sm font-semibold text-[var(--color-lime)] hover:text-[var(--color-lime)]/80 transition-colors">
            Participar &rarr;
          </a>
        </div>
      </header>

      <main className="px-4 md:px-8 lg:px-16 py-10 md:py-16">
        <div className="max-w-4xl mx-auto">
          <div className="text-center mb-10 md:mb-14">
            <h1 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-charcoal)] mb-2">
              Rankings
            </h1>
            <p className="text-sm text-[var(--color-charcoal)]/50">
              Placar em tempo real · Atualizado automaticamente
            </p>
            <div className="inline-flex items-center gap-2 mt-3 bg-[var(--color-lime)]/10 text-[var(--color-charcoal)]/60 text-xs font-medium px-3 py-1.5 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse-dot" />
              Dados simulados (modo demonstração)
            </div>
          </div>

          <div className="space-y-6 md:space-y-8">
            <RankingTable
              title="Quem mais comprou"
              subtitle="Premiação para os 3 primeiros"
              entries={buyersRanking}
              icon="🛒"
            />

            <RankingTable
              title="Quem mais indicou"
              subtitle="Vendas concluídas entre amigos"
              entries={referrersRanking}
              icon="🤝"
            />

            <RankingTable
              title="Divulgadores"
              subtitle="Links mais ativos (Instagram/TikTok)"
              entries={influencersRanking}
              icon="📢"
            />
          </div>

          {/* CTA */}
          <div className="mt-12 md:mt-16 bg-[var(--color-charcoal)] rounded-2xl p-6 md:p-8 text-center">
            <p className="text-[var(--color-cream)]/55 text-sm mb-2">Quer subir no ranking?</p>
            <p className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-bold text-[var(--color-cream)] mb-6">
              Compre mais números ou indique amigos
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <a
                href="/participar"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-xl font-bold text-base bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.3)] hover:brightness-105 active:scale-[0.98] transition-all"
              >
                Comprar números &rarr;
              </a>
              <a
                href="/indicar"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-xl font-semibold text-base border border-white/10 text-[var(--color-cream)]/70 hover:text-[var(--color-cream)] hover:border-white/20 transition-all"
              >
                Indicar amigos
              </a>
            </div>
          </div>

          {/* Rules */}
          <div className="mt-8 text-center">
            <p className="text-xs text-[var(--color-charcoal)]/35 max-w-lg mx-auto leading-relaxed">
              Rankings identificados por iniciais e últimos 4 dígitos do telefone.
              Premiações de ranking são bonificações separadas dos prêmios do sorteio.
              <a href="/regulamento" className="text-[var(--color-terracotta)] hover:underline ml-1">Ver regulamento</a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}

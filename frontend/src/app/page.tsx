import { CTAButton } from "@/components/cta-button";
import { SocialProof } from "@/components/social-proof";
import { HowItWorks } from "@/components/how-it-works";
import { Prizes } from "@/components/prizes";
import { UrgencyBar } from "@/components/urgency-bar";
import { LiveTicker } from "@/components/live-ticker";
import { FloatingCTA } from "@/components/floating-cta";

export default function Home() {
  return (
    <>
      {/* ── Barra de urgência (sticky) ── */}
      <div className="sticky top-0 z-50">
        <UrgencyBar />
      </div>

      {/* ── Hero — Produto primeiro ── */}
      <section className="px-4 md:px-8 lg:px-16 pt-10 pb-16 md:pt-16 md:pb-24 bg-[var(--color-cream)]">
        <div className="max-w-5xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-14 items-start">
            {/* Lado esquerdo — Copy de venda */}
            <div className="md:pt-4">
              <div className="inline-flex items-center gap-2 bg-[var(--color-terracotta)]/10 text-[var(--color-terracotta)] text-xs font-bold uppercase tracking-widest px-3 py-1.5 rounded-full mb-5">
                <span className="inline-block w-1.5 h-1.5 rounded-full bg-[var(--color-terracotta)]" />
                Ação entre Amigos
              </div>

              <h1 className="font-[family-name:var(--font-heading)] text-[clamp(2rem,5vw,3.5rem)] leading-[1.1] font-bold text-[var(--color-charcoal)] tracking-tight">
                Seu número da sorte
                <br />
                <span className="text-[var(--color-terracotta)]">por apenas R$&nbsp;0,56</span>
              </h1>

              <p className="mt-5 text-base md:text-lg text-[var(--color-charcoal)]/65 leading-relaxed max-w-md">
                Concorra a <strong className="text-[var(--color-charcoal)] font-semibold">R$&nbsp;18.500 em prêmios</strong> — câmeras profissionais,
                MacBook, dinheiro e 10 surpresas. Resultado pela Loteria Federal.
              </p>

              <div className="mt-8 flex flex-col sm:flex-row items-start gap-4">
                <CTAButton showPrice />
              </div>

              <div className="mt-6 flex flex-wrap items-center gap-x-5 gap-y-2 text-[13px] text-[var(--color-charcoal)]/55">
                <span className="flex items-center gap-1.5">
                  <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4 text-[var(--color-terracotta)]/60" aria-hidden="true">
                    <path d="M8 1.5a6.5 6.5 0 100 13 6.5 6.5 0 000-13z" stroke="currentColor" strokeWidth="1.2" />
                    <path d="M5.5 8l2 2 3-3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  Pix instantâneo
                </span>
                <span className="flex items-center gap-1.5">
                  <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4 text-[var(--color-terracotta)]/60" aria-hidden="true">
                    <path d="M8 1.5a6.5 6.5 0 100 13 6.5 6.5 0 000-13z" stroke="currentColor" strokeWidth="1.2" />
                    <path d="M5.5 8l2 2 3-3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  Sem cadastro longo
                </span>
                <span className="flex items-center gap-1.5">
                  <svg viewBox="0 0 16 16" fill="none" className="w-4 h-4 text-[var(--color-terracotta)]/60" aria-hidden="true">
                    <path d="M8 1.5a6.5 6.5 0 100 13 6.5 6.5 0 000-13z" stroke="currentColor" strokeWidth="1.2" />
                    <path d="M5.5 8l2 2 3-3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  Sorteio auditável
                </span>
              </div>
            </div>

            {/* Lado direito — Vitrine de prêmios */}
            <div className="relative pb-6">
              <div className="bg-white rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.08)] border border-black/[0.04] overflow-hidden">
                {/* Prêmio destaque */}
                <div className="bg-gradient-to-br from-[var(--color-charcoal)] via-[#352620] to-[#2B1D16] px-6 py-8 md:px-8 md:py-10 text-center relative overflow-hidden">
                  <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(198,242,78,0.06),transparent_60%)]" />
                  <div className="relative">
                    <span className="inline-block bg-[var(--color-lime)] text-[var(--color-charcoal)] text-[10px] font-bold uppercase tracking-widest px-3 py-1 rounded-full mb-4">
                      1º Prêmio
                    </span>
                    <div className="text-5xl md:text-6xl mb-3" aria-hidden="true">📸</div>
                    <h2 className="font-[family-name:var(--font-heading)] text-xl md:text-2xl font-bold text-[var(--color-cream)] mb-1">
                      Kit Canon Profissional
                    </h2>
                    <p className="text-sm text-[var(--color-cream)]/50 mb-4">
                      Canon D70 + Lente 24-70 f/4 + Flash 430EX
                    </p>
                    <span className="font-[family-name:var(--font-heading)] text-3xl md:text-4xl font-bold text-[var(--color-lime)]">
                      R$ 10.000
                    </span>
                  </div>
                </div>

                {/* Mini lista de outros prêmios */}
                <div className="divide-y divide-black/[0.04]">
                  {[
                    { emoji: "💻", name: "MacBook Air M1", value: "R$ 4.000" },
                    { emoji: "📷", name: "Kit Canon T1i Rebel", value: "R$ 2.000" },
                    { emoji: "💰", name: "Dinheiro via Pix", value: "R$ 1.000" },
                    { emoji: "💵", name: "Dinheiro via Pix", value: "R$ 500" },
                  ].map((p) => (
                    <div key={p.name + p.value} className="flex items-center gap-3 px-5 py-3.5 md:px-7 hover:bg-black/[0.01] transition-colors">
                      <span className="text-xl" aria-hidden="true">{p.emoji}</span>
                      <span className="flex-1 text-sm font-medium text-[var(--color-charcoal)]">{p.name}</span>
                      <span className="text-sm font-bold text-[var(--color-terracotta)] tabular-nums">{p.value}</span>
                    </div>
                  ))}
                </div>

                {/* Surpresas */}
                <div className="bg-[var(--color-lime)]/8 px-6 py-3.5 md:px-8 text-center border-t border-[var(--color-lime)]/10">
                  <span className="text-sm text-[var(--color-charcoal)]/70">
                    🎁 <strong className="font-semibold">+ 10 prêmios-surpresa</strong> de R$ 100
                  </span>
                </div>
              </div>

              {/* Badge de total */}
              <div className="absolute -bottom-0 left-1/2 -translate-x-1/2 bg-[var(--color-terracotta)] text-white px-6 py-2.5 rounded-full shadow-[0_4px_16px_rgba(181,80,47,0.3)] whitespace-nowrap">
                <span className="text-sm font-bold tracking-wide">Total: R$ 18.500 em prêmios</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Prova social ── */}
      <SocialProof />

      {/* ── Como funciona ── */}
      <HowItWorks />

      {/* ── Todos os prêmios ── */}
      <Prizes />

      {/* ── CTA Final ── */}
      <section className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-20 md:py-28">
        <div className="max-w-2xl mx-auto text-center">
          <p className="text-5xl md:text-6xl mb-5" aria-hidden="true">🍀</p>
          <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.75rem)] font-bold text-[var(--color-cream)] mb-5">
            Não deixe pra depois
          </h2>
          <p className="text-base md:text-lg text-[var(--color-cream)]/55 mb-10 max-w-md mx-auto leading-relaxed">
            Cada número vendido é um a menos disponível. Garanta o seu agora e concorra a R$&nbsp;18.500 em prêmios.
          </p>

          <CTAButton showPrice />

          <p className="mt-6 text-sm text-[var(--color-cream)]/30">
            Pagamento seguro via Pix · Resultado transparente
          </p>
        </div>
      </section>

      {/* ── Rodapé mínimo ── */}
      <footer className="bg-[var(--color-charcoal)] border-t border-white/5 px-4 md:px-8 lg:px-16 py-8 pb-24 md:pb-8">
        <div className="max-w-5xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-[var(--color-cream)]/40">
          <span className="font-[family-name:var(--font-heading)] text-base text-[var(--color-cream)]/60">
            Ação entre Amigos
          </span>
          <nav className="flex gap-6">
            <a href="/regulamento" className="hover:text-[var(--color-cream)]/70 transition-colors">Regulamento</a>
            <a href="/rankings" className="hover:text-[var(--color-cream)]/70 transition-colors">Rankings</a>
            <a href="/meus-numeros" className="hover:text-[var(--color-cream)]/70 transition-colors">Meus números</a>
          </nav>
        </div>
      </footer>

      {/* ── Ticker de compras em tempo real ── */}
      <LiveTicker />

      {/* ── CTA flutuante mobile ── */}
      <FloatingCTA />
    </>
  );
}

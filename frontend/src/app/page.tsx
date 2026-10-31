import { CommunityRing } from "@/components/community-ring";
import { ComboCards } from "@/components/combo-cards";
import { Countdown } from "@/components/countdown";
import { CTAButton } from "@/components/cta-button";
import { FloatingCTA } from "@/components/floating-cta";
import { HowItWorks } from "@/components/how-it-works";
import { LiveFeed } from "@/components/live-feed";
import { Prizes } from "@/components/prizes";
import { Section } from "@/components/section";
import { UrgencyBar } from "@/components/urgency-bar";

export default function Home() {
  return (
    <>
      <div className="sticky top-0 z-50">
        <UrgencyBar />
      </div>

      {/* 1. Hero — foto dos prêmios como background com overlay escuro */}
      <section className="section relative overflow-hidden py-20 text-white md:py-28">
        {/* Background: foto dos produtos com transparência natural */}
        <div
          className="absolute inset-0 z-0 bg-[url('/hero-mobile.png')] bg-cover bg-center bg-no-repeat md:bg-[url('/hero-produtos.jpeg')]"
          aria-hidden="true"
        />

        {/* Overlay: escurece a foto para garantir contraste do texto (AA) */}
        <div
          className="absolute inset-0 z-[1]"
          style={{
            background:
              "linear-gradient(180deg, rgba(7, 26, 22, 0.82) 0%, rgba(10, 36, 30, 0.72) 50%, rgba(11, 43, 38, 0.88) 100%)",
          }}
          aria-hidden="true"
        />

        {/* Conteúdo acima do overlay */}
        <div className="section-inner relative z-10">
          <p className="mb-6 text-sm font-medium text-[var(--color-green)]">
            Ação entre Amigos
          </p>

          <h1 className="mx-auto max-w-2xl text-center font-[family-name:var(--font-heading)] text-[clamp(1.75rem,5vw,3rem)] font-bold leading-[1.15] tracking-tight">
            Cada tijolinho importa
          </h1>

          <p className="mx-auto mt-4 max-w-md text-center text-base text-white/75">
            Ajude a construir esse sonho e concorra a prêmios.
            R$&nbsp;18.500 em premiações, sorteio pela Loteria Federal.
          </p>

          <div className="my-10 flex w-full justify-center md:my-12">
            <CommunityRing />
          </div>

          <div className="flex w-full flex-col items-center gap-8">
            <CTAButton showPrice />

            <div className="flex flex-wrap justify-center gap-x-6 gap-y-2 text-sm text-white/70">
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
                Pix instantâneo
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
                Loteria Federal
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-green)]" aria-hidden="true" />
                Sorteio auditável
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Combos */}
      <ComboCards />

      {/* 3. Prêmios */}
      <Prizes />

      {/* 4. Como participar */}
      <HowItWorks />

      {/* 5. Feed da comunidade */}
      <LiveFeed />

      {/* 6. CTA final + contagem regressiva */}
      <Section tone="forest" spacing="tall">
        <div className="mx-auto flex max-w-lg flex-col items-center">
          <h2 className="font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.5rem)] font-bold leading-tight text-white">
            Falta pouco pra fechar
          </h2>
          <p className="mt-4 max-w-sm text-base text-white/70">
            Cada número que sai é um a menos disponível. Entre agora e faça
            parte dessa comunidade.
          </p>

          <div className="my-10 w-full">
            <Countdown />
          </div>

          <CTAButton showPrice />

          <p className="mt-6 text-sm text-white/50">
            Pagamento seguro via Pix
          </p>
        </div>
      </Section>

      {/* 7. Rodapé */}
      <footer className="bg-[var(--color-forest-deep)] px-5 pb-safe pt-8 md:px-8 md:pb-8">
        <div className="mx-auto flex max-w-[1140px] flex-col items-center justify-between gap-4 text-sm text-white/50 md:flex-row">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/70">
            Ação entre Amigos
          </span>
          <nav className="flex gap-6 text-xs" aria-label="Rodapé">
            <a href="/regulamento" className="transition-colors hover:text-white/80">
              Regulamento
            </a>
            <a href="/faq" className="transition-colors hover:text-white/80">
              FAQ
            </a>
            <a href="/rankings" className="transition-colors hover:text-white/80">
              Rankings
            </a>
            <a href="/meus-numeros" className="transition-colors hover:text-white/80">
              Meus números
            </a>
          </nav>
        </div>
      </footer>

      <FloatingCTA />
    </>
  );
}

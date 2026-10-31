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
      <section className="section relative overflow-hidden py-12 text-white md:py-16">
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

          <p className="mx-auto mt-4 max-w-md text-center text-base leading-relaxed text-white/75">
            Eu não estou pedindo um favor.
            Estou dividindo um sonho com quem quer construir junto.
          </p>

          <div className="my-6 flex w-full justify-center md:my-8">
            <CommunityRing />
          </div>

          <div className="flex w-full flex-col items-center gap-5">
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

      {/* 2. A história */}
      <section className="bg-gradient-to-b from-amber-50/80 to-orange-50/40 px-5 py-12 md:px-8 md:py-14">
        <div className="mx-auto flex max-w-xl flex-col items-center">
          <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
            Todo negócio começa com alguém acreditando antes de existir
          </h2>
          <p className="mt-2 text-center text-base text-[var(--color-ink-soft)]">
            Hoje, esse alguém pode ser você.
          </p>

          <div className="mt-6 space-y-4 text-base leading-relaxed text-[var(--color-ink-soft)]">
            <p>
              Estou vendendo o que tenho, abrindo o que posso e devolvendo o que consigo.
              Porque sonho bom é sonho dividido.
            </p>
            <p>
              Cada contribuição vira um tijolo real: o computador que vai rodar o projeto.
              O site que vai me apresentar ao mundo. O designer que vai dar rosto à ideia.
              O jurídico que vai me proteger. A marca que vai me tornar real.
            </p>
          </div>
        </div>
      </section>

      {/* 3. A troca */}
      <section className="bg-gradient-to-b from-rose-50/60 to-amber-50/30 px-5 py-12 md:px-8 md:py-14">
        <div className="mx-auto flex max-w-xl flex-col items-center">
          <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
            Eu não venho de mãos vazias
          </h2>

          <div className="mt-6 space-y-4 text-base leading-relaxed text-[var(--color-ink-soft)]">
            <p>
              Devolvo com o que tenho: minhas câmeras, meu computador atual, retribuições
              em dinheiro e participação no que vamos construir juntos.
            </p>
            <p>
              São 120 chances de ser retribuído de verdade. Não é esmola.
              É troca justa, com o coração no meio.
            </p>
          </div>
        </div>
      </section>

      {/* 4. Combos */}
      <ComboCards />

      {/* 3. Prêmios */}
      <Prizes />

      {/* 5. Quem você deseja ajudar */}
      <Section tone="snow">
        <div className="mx-auto max-w-2xl">
          <h2 className="text-center font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,2.25rem)] font-bold leading-tight text-[var(--color-ink)]">
            Quem você deseja ajudar
          </h2>
          <p className="mt-3 text-center text-base leading-relaxed text-[var(--color-ink-soft)]">
            Parte da sua contribuição você escolhe para onde vai.
            Seu apoio ao meu sonho também vira apoio a quem mais precisa.
          </p>

          <div className="mt-10 grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-6">
            {[
              { name: "GRAAC", color: "#E4002B", initials: "G" },
              { name: "Casa Hope", color: "#00A651", initials: "CH" },
              { name: "AACD", color: "#0072BC", initials: "A" },
              { name: "TUCCA", color: "#8B5CF6", initials: "T" },
              { name: "ITACI", color: "#F59E0B", initials: "I" },
              { name: "AACC", color: "#EC4899", initials: "A" },
            ].map((org) => (
              <div key={org.name} className="flex flex-col items-center gap-2.5">
                <div
                  className="flex h-16 w-16 items-center justify-center rounded-2xl text-xl font-bold text-white shadow-sm"
                  style={{ backgroundColor: org.color }}
                  aria-hidden="true"
                >
                  {org.initials}
                </div>
                <span className="text-center text-xs font-semibold text-[var(--color-ink)]">
                  {org.name}
                </span>
              </div>
            ))}
          </div>
        </div>
      </Section>

      {/* 6. Como participar */}
      <HowItWorks />

      {/* 7. Feed da comunidade */}
      <LiveFeed />

      {/* 8. CTA final + contagem regressiva */}
      <Section tone="forest" spacing="tall">
        <div className="mx-auto flex max-w-lg flex-col items-center">
          <p className="max-w-sm text-center text-base leading-relaxed text-white/70">
            Eu queria pedir mais. Mas estou sendo honesto com o que preciso agora:
            o suficiente para começar, e justo o bastante para que muita gente ganhe junto.
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
            <a href="/numeros" className="transition-colors hover:text-white/80">
              Números
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

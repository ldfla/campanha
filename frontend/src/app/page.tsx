import { CTAButton } from "@/components/cta-button";
import { SocialProof } from "@/components/social-proof";
import { HowItWorks } from "@/components/how-it-works";
import { Prizes } from "@/components/prizes";

export default function Home() {
  return (
    <>
      {/* ── Hero ── */}
      <section className="relative min-h-[100dvh] flex flex-col justify-center px-6 md:px-16 lg:px-24 xl:px-32 py-24 bg-[var(--color-cream)]">
        <div className="max-w-4xl">
          <h1
            className="animate-mask-reveal font-[family-name:var(--font-heading)] text-[clamp(2.5rem,7vw,5.5rem)] leading-[1.05] font-semibold text-[var(--color-charcoal)] tracking-tight"
          >
            Eu tenho um sonho.
            <br />
            E um número da sorte pode ser o primeiro tijolo.
          </h1>

          <p className="mt-10 md:mt-12 max-w-xl text-lg md:text-xl leading-relaxed text-[var(--color-charcoal)] opacity-75">
            Compre seu número na Ação entre Amigos. Concorra a prêmios de
            verdade&nbsp;&mdash; e ajude a empresa a nascer.
          </p>

          <div className="mt-12 md:mt-14 animate-cta-glow">
            <CTAButton />
          </div>

          <p className="mt-5 text-sm text-[var(--color-charcoal)] opacity-45 tracking-wide">
            Leva 1 minuto. Prêmios a qualquer momento.
          </p>
        </div>
      </section>

      {/* ── Prova social ── */}
      <SocialProof />

      {/* ── Como funciona ── */}
      <HowItWorks />

      {/* ── Prêmios ── */}
      <Prizes />

      {/* ── Fechamento emocional ── */}
      <section className="bg-[var(--color-terracotta)] px-6 md:px-16 lg:px-24 xl:px-32 py-24 md:py-36">
        <div className="max-w-3xl mx-auto text-center">
          <p className="font-[family-name:var(--font-heading)] text-[clamp(1.5rem,4vw,3rem)] leading-tight font-medium text-[var(--color-cream)]">
            Não é doação. Não é investimento.
            <br />
            É aposta no meu sonho&nbsp;&mdash; e no seu prêmio.
          </p>

          <div className="mt-12 flex flex-col items-center gap-4">
            <CTAButton size="md" />
            <p className="text-sm text-[var(--color-cream)] opacity-60">
              Sem cadastro complicado. Seus prêmios, divulgados abertos.
            </p>
          </div>
        </div>
      </section>

      {/* ── Rodapé ── */}
      <footer className="bg-[var(--color-charcoal)] px-6 md:px-16 lg:px-24 xl:px-32 py-12">
        <div className="max-w-5xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6 text-sm text-[var(--color-cream)] opacity-60">
          <span className="font-[family-name:var(--font-heading)] text-base opacity-100 text-[var(--color-cream)]">
            Ação entre Amigos
          </span>

          <nav className="flex gap-6">
            <a href="#regulamento" className="hover:opacity-100 transition-opacity">
              Regulamento
            </a>
            <a href="#contato" className="hover:opacity-100 transition-opacity">
              Fale comigo
            </a>
          </nav>
        </div>
      </footer>
    </>
  );
}

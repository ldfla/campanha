import { CTAButton } from "@/components/cta-button";
import { SocialProof } from "@/components/social-proof";
import { HowItWorks } from "@/components/how-it-works";
import { Prizes } from "@/components/prizes";

export default function Home() {
  return (
    <>
      {/* ── Hero ── */}
      <section className="relative min-h-[100dvh] flex flex-col justify-center px-6 md:px-16 lg:px-24 xl:px-32 py-24 bg-[var(--color-cream)] overflow-hidden">
        {/* Decorative terracotta accent */}
        <div
          className="absolute top-0 right-0 w-[60%] h-[40%] opacity-[0.04] pointer-events-none"
          style={{
            background: "radial-gradient(ellipse at top right, var(--color-terracotta), transparent 70%)",
          }}
        />

        <div className="relative max-w-5xl">
          <span className="inline-block font-[family-name:var(--font-heading)] text-sm md:text-base font-medium tracking-[0.2em] uppercase text-[var(--color-terracotta)] mb-6 md:mb-8">
            Ação entre Amigos
          </span>

          <h1 className="animate-mask-reveal font-[family-name:var(--font-heading)] text-[clamp(2.75rem,8vw,6.5rem)] leading-[0.95] font-bold text-[var(--color-charcoal)] tracking-tight">
            Eu tenho
            <br />
            um sonho.
          </h1>

          <p className="mt-6 md:mt-8 font-[family-name:var(--font-heading)] text-[clamp(1.25rem,3vw,2.25rem)] leading-[1.2] font-normal text-[var(--color-terracotta)] max-w-2xl">
            E um número da sorte pode ser
            <br className="hidden md:block" />{" "}
            o primeiro tijolo.
          </p>

          <p className="mt-8 md:mt-10 max-w-lg text-base md:text-lg leading-relaxed text-[var(--color-charcoal)] opacity-65">
            Compre seu número na Ação entre Amigos. Concorra a mais de R$&nbsp;18.000 em
            prêmios&nbsp;&mdash; câmeras, MacBook, dinheiro e surpresas.
          </p>

          <div className="mt-10 md:mt-14 animate-cta-glow">
            <CTAButton />
          </div>

          <p className="mt-5 text-sm text-[var(--color-charcoal)] opacity-40 tracking-wide">
            Leva 1 minuto &middot; Sem cadastro complicado
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
            Cada número comprado
            <br />
            é um tijolo nessa história.
          </p>
          <p className="mt-6 text-lg text-[var(--color-cream)] opacity-70 max-w-md mx-auto">
            Você concorre, eu construo. Se der sorte, a gente comemora junto.
          </p>

          <div className="mt-12 flex flex-col items-center gap-4">
            <CTAButton size="md" />
            <p className="text-sm text-[var(--color-cream)] opacity-50">
              Seus prêmios, divulgados abertos. Sem surpresas escondidas.
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

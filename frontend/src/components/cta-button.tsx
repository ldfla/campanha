export function CTAButton({ size = "lg", showPrice = false }: { size?: "lg" | "md" | "sm"; showPrice?: boolean }) {
  const cls = {
    lg: "px-10 py-5 text-lg md:text-xl gap-3 rounded-2xl",
    md: "px-8 py-4 text-base md:text-lg gap-2.5 rounded-xl",
    sm: "px-6 py-3 text-sm md:text-base gap-2 rounded-lg",
  }[size];

  return (
    <a
      href="/participar"
      className={`group inline-flex items-center font-bold bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.35)] transition-all duration-150 hover:brightness-110 hover:shadow-[0_6px_32px_rgba(198,242,78,0.5)] hover:scale-[1.02] active:scale-[0.98] ${cls}`}
    >
      {showPrice ? (
        <>
          Garantir meu número
          <span className="bg-[var(--color-charcoal)] text-[var(--color-lime)] text-xs font-bold px-2.5 py-1 rounded-lg">
            R$ 0,56
          </span>
        </>
      ) : (
        <>
          Quero meu número
          <span aria-hidden="true" className="transition-transform group-hover:translate-x-0.5">&rarr;</span>
        </>
      )}
    </a>
  );
}

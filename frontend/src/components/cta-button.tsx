export function CTAButton({ size = "lg" }: { size?: "lg" | "md" | "sm" }) {
  const cls = {
    lg: "px-14 py-6 text-xl md:text-2xl gap-4 rounded-[20px]",
    md: "px-10 py-5 text-lg md:text-xl gap-3 rounded-2xl",
    sm: "px-8 py-4 text-base md:text-lg gap-3 rounded-xl",
  }[size];

  return (
    <a
      href="/participar"
      className={`inline-flex items-center font-bold bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_6px_32px_rgba(198,242,78,0.4)] transition-all duration-200 hover:brightness-110 hover:shadow-[0_8px_40px_rgba(198,242,78,0.55)] hover:scale-[1.03] active:scale-[0.98] ${cls}`}
    >
      Quero meu número
      <span aria-hidden="true" className="text-2xl">&rarr;</span>
    </a>
  );
}

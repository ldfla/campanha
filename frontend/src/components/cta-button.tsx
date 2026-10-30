export function CTAButton() {
  return (
    <a
      href="/participar"
      className="inline-flex items-center gap-2 px-10 py-5 bg-[var(--color-lime)] text-[var(--color-charcoal)] font-semibold text-lg rounded-2xl transition-all duration-200 hover:brightness-105 hover:scale-[1.02] active:scale-[0.98]"
    >
      Quero meu número
      <span aria-hidden="true">&rarr;</span>
    </a>
  );
}

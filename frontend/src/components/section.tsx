import type { ReactNode } from "react";

type Tone = "snow" | "white" | "forest";

const tones: Record<Tone, string> = {
  snow: "bg-[var(--color-snow)]",
  white: "bg-white",
  forest:
    "bg-[linear-gradient(180deg,var(--color-forest)_0%,var(--color-forest-deep)_100%)] text-white",
};

const spacings = {
  normal: "py-16 md:py-24",
  tall: "py-20 md:py-28",
} as const;

interface SectionProps {
  id?: string;
  tone?: Tone;
  spacing?: keyof typeof spacings;
  children: ReactNode;
  className?: string;
  innerClassName?: string;
}

/**
 * Contêiner de seção da landing: sempre centralizado, limitado a 1140px,
 * com o conteúdo empilhado (flex column) para eliminar sobra lateral.
 */
export function Section({
  id,
  tone = "snow",
  spacing = "normal",
  children,
  className = "",
  innerClassName = "",
}: SectionProps) {
  return (
    <section
      id={id}
      className={`section ${spacings[spacing]} ${tones[tone]} ${className}`}
    >
      <div className={`section-inner ${innerClassName}`}>{children}</div>
    </section>
  );
}

"use client";

import { useEffect, useState } from "react";

export function FloatingCTA() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    function onScroll() {
      setShow(window.scrollY > 500);
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (!show) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-white/95 backdrop-blur-md border-t border-black/[0.06] px-4 py-3 safe-area-inset-bottom animate-slide-in">
      <a
        href="/participar"
        className="flex items-center justify-center gap-2 w-full py-3.5 rounded-xl font-bold text-[15px] bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_2px_16px_rgba(198,242,78,0.35)] active:scale-[0.97] transition-transform"
      >
        Garantir meu número
        <span className="bg-[var(--color-charcoal)] text-[var(--color-lime)] text-[10px] font-extrabold px-2 py-0.5 rounded tracking-wide">
          R$&nbsp;0,56
        </span>
      </a>
    </div>
  );
}

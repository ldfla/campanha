"use client";

import { useEffect, useState } from "react";

export function FloatingCTA() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    function onScroll() {
      setShow(window.scrollY > 600);
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (!show) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-white/95 backdrop-blur-md border-t border-black/5 px-4 py-3 animate-slide-in">
      <a
        href="/participar"
        className="flex items-center justify-center gap-2 w-full py-3.5 rounded-xl font-bold text-base bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.4)] active:scale-[0.97] transition-transform"
      >
        Garantir meu número — R$&nbsp;0,56
        <span aria-hidden="true">&rarr;</span>
      </a>
    </div>
  );
}

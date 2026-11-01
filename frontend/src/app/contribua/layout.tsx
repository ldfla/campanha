"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/use-auth";
import { isDemoMode } from "@/lib/campaign-config";

const NAV_ITEMS = [
  { href: "/contribua", label: "Dashboard", icon: "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" },
  { href: "/contribua/ofertas", label: "Ofertas", icon: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" },
  { href: "/contribua/beneficiarios", label: "Instituições", icon: "M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" },
  { href: "/contribua/pagamentos", label: "Pagamentos", icon: "M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" },
  { href: "/contribua/compliance", label: "Compliance", icon: "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" },
  { href: "/contribua/sorteio", label: "Sorteio", icon: "M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  { href: "/contribua/relatorios", label: "Relatórios", icon: "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" },
] as const;

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, loading } = useAuth();
  const demoMode = isDemoMode();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-dvh bg-[var(--color-snow)]">
        <div className="w-8 h-8 border-2 border-[var(--color-iris)]/20 border-t-[var(--color-iris)] rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-dvh bg-[var(--color-snow)]">
        <div className="max-w-md mx-auto text-center px-6">
          <h1 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-[var(--color-midnight)] mb-4">
            Acesso restrito
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/50 mb-6">
            Faça login para acessar o painel administrativo.
          </p>
          <Link href="/participar" className="inline-flex px-6 py-3 rounded-xl font-semibold text-sm bg-[var(--color-iris)] text-white hover:bg-[var(--color-iris)]/90 transition-colors">
            Ir para o início
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors">
              Ação entre Amigos
            </Link>
            <span className="text-xs text-white/30 border-l border-white/10 pl-4">Painel Admin</span>
          </div>
          <div className="flex items-center gap-3">
            {demoMode && (
              <span className="text-[10px] font-semibold text-[var(--color-iris)] bg-[var(--color-iris)]/15 px-2.5 py-1 rounded-full">
                DEMO
              </span>
            )}
            <span className="text-xs text-white/40">{user.firstName}</span>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto flex">
        <nav className="hidden md:block w-56 shrink-0 border-r border-black/[0.04] bg-white min-h-[calc(100dvh-56px)]">
          <div className="py-4 px-3 space-y-0.5">
            {NAV_ITEMS.map((item) => {
              const isActive = pathname === item.href || (item.href !== "/contribua" && pathname.startsWith(item.href));
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-[var(--color-iris)]/10 text-[var(--color-iris)]"
                      : "text-[var(--color-midnight)]/50 hover:text-[var(--color-midnight)] hover:bg-black/[0.03]"
                  }`}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5 shrink-0">
                    <path d={item.icon} />
                  </svg>
                  {item.label}
                </Link>
              );
            })}
          </div>
        </nav>

        {/* Mobile nav */}
        <div className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-black/[0.06] z-50 px-2 py-2">
          <div className="flex items-center justify-around">
            {NAV_ITEMS.slice(0, 5).map((item) => {
              const isActive = pathname === item.href || (item.href !== "/contribua" && pathname.startsWith(item.href));
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-lg text-[10px] font-medium transition-colors ${
                    isActive ? "text-[var(--color-iris)]" : "text-[var(--color-midnight)]/40"
                  }`}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
                    <path d={item.icon} />
                  </svg>
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>

        <main className="flex-1 p-5 md:p-8 pb-24 md:pb-8">
          {children}
        </main>
      </div>
    </div>
  );
}

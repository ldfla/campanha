import type { Metadata } from "next";
import { Fraunces, Overlock } from "next/font/google";
import { AuthProvider } from "@/components/auth-provider";
import "./globals.css";

const fraunces = Fraunces({
  subsets: ["latin"],
  variable: "--font-fraunces",
  display: "swap",
});

const overlock = Overlock({
  subsets: ["latin"],
  weight: ["400", "700", "900"],
  style: ["normal", "italic"],
  variable: "--font-overlock",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Ação entre Amigos — Cada tijolinho importa",
  description:
    "Cada tijolinho importa, ajude a construir esse sonho e concorra a prêmios. R$ 25.300 em prêmios, sorteio pela Loteria Federal em 20/12/2026.",
  keywords: [
    "ação entre amigos",
    "sorteio",
    "prêmios",
    "loteria federal",
    "concorra",
  ],
  openGraph: {
    title: "Ação entre Amigos — Cada tijolinho importa",
    description:
      "Cada tijolinho importa, ajude a construir esse sonho e concorra a prêmios. R$ 25.300 em premiações.",
    locale: "pt_BR",
    type: "website",
    siteName: "Ação entre Amigos",
  },
  twitter: {
    card: "summary_large_image",
    title: "Ação entre Amigos — Cada tijolinho importa",
    description:
      "Ajude a construir esse sonho e concorra a R$ 25.300 em prêmios.",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="pt-BR"
      className={`${fraunces.variable} ${overlock.variable}`}
    >
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}

import type { Metadata } from "next";
import { Fraunces, Overlock } from "next/font/google";
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
  title: "Ação entre Amigos — Ajude um sonho a nascer",
  description:
    "Compre seu número na Ação entre Amigos. Concorra a prêmios de verdade e ajude a empresa a nascer.",
  openGraph: {
    title: "Ação entre Amigos — Ajude um sonho a nascer",
    description:
      "Compre seu número, concorra a prêmios e faça parte dessa história.",
    locale: "pt_BR",
    type: "website",
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="pt-BR"
      className={`${fraunces.variable} ${overlock.variable}`}
    >
      <body>{children}</body>
    </html>
  );
}

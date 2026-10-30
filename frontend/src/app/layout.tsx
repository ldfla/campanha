import type { Metadata } from "next";
import { Fraunces, DM_Sans } from "next/font/google";
import "./globals.css";

const fraunces = Fraunces({
  subsets: ["latin"],
  variable: "--font-fraunces",
  display: "swap",
});

const dmSans = DM_Sans({
  subsets: ["latin"],
  variable: "--font-dm-sans",
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
      className={`${fraunces.variable} ${dmSans.variable}`}
    >
      <body>{children}</body>
    </html>
  );
}

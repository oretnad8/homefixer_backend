import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "HomeFixer — Servicios Técnicos a Domicilio",
  description:
    "Conecta con técnicos calificados para reparaciones y mantenimiento en tu hogar.",
  icons: {
    icon: "/favicon.ico",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="es" className={inter.variable} data-scroll-behavior="smooth">
      {/* inter.className aplica la fuente cargada por next/font (nombre con hash) */}
      <body className={inter.className}>{children}</body>
    </html>
  );
}

import type { Metadata } from "next";
import "@fontsource/barlow-condensed/600.css";
import "@fontsource/barlow-condensed/700.css";
import "@fontsource/source-sans-3/400.css";
import "@fontsource/source-sans-3/500.css";
import "@fontsource/source-sans-3/600.css";
import "@fontsource/source-sans-3/700.css";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { Shell } from "@/components/shell";

export const metadata: Metadata = {
  title: {
    default: "Projetos Sociais — Toda mudança começa perto",
    template: "%s | Projetos Sociais",
  },
  description:
    "Encontre iniciativas comunitárias, conheça as organizações responsáveis e acompanhe cada novo passo.",
};
export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" data-scroll-behavior="smooth">
      <body>
        <AuthProvider>
          <Shell>{children}</Shell>
        </AuthProvider>
      </body>
    </html>
  );
}

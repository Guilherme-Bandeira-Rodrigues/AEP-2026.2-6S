"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { ArrowUpRight, LogOut } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { errorMessage } from "@/lib/errors";
import { Feedback } from "./ui";

export function Brand() {
  return (
    <Link href="/" className="brand" aria-label="Projetos Sociais — início">
      <span className="brand-mark" aria-hidden="true">
        <i />
        <i />
        <i />
        <i />
      </span>
      <span>
        PROJETOS
        <br />
        SOCIAIS<span className="brand-dot">.</span>
      </span>
    </Link>
  );
}
export function Shell({ children }: { children: ReactNode }) {
  const { user, api, setUser, handleError } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirmReset, setConfirmReset] = useState(false);
  async function logout() {
    setBusy(true);
    setError("");
    try {
      await api.auth.logout();
      setUser(null);
      router.push("/");
    } catch (e) {
      handleError(e);
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function reset() {
    try {
      await api.resetDemo?.();
      window.location.reload();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <>
      <a href="#conteudo" className="skip-link">
        Pular para o conteúdo
      </a>
      {api.mode === "demo" && (
        <div className="demo-bar">
          <span>Modo demonstração · dados fictícios</span>
          {confirmReset ? (
            <span>
              Apagar alterações locais?{" "}
              <button onClick={reset}>Restaurar</button>
              <button onClick={() => setConfirmReset(false)}>Cancelar</button>
            </span>
          ) : (
            <button onClick={() => setConfirmReset(true)}>
              Restaurar demonstração
            </button>
          )}
        </div>
      )}
      <header className="site-header">
        <div className="container header-inner">
          <Brand />
          <nav aria-label="Navegação principal">
            <Link
              href="/"
              className={`nav-link explore-link ${pathname === "/" ? "active" : ""}`}
              aria-current={pathname === "/" ? "page" : undefined}
            >
              Explorar projetos
            </Link>
            {user ? (
              <>
                <Link href="/gestao" className="button button-primary">
                  Minha gestão <ArrowUpRight size={17} />
                </Link>
                <button
                  onClick={logout}
                  disabled={busy}
                  className="logout-button"
                  aria-label="Sair da conta"
                >
                  <LogOut size={18} />
                  <span>Sair</span>
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="nav-link">
                  Entrar
                </Link>
                <Link href="/cadastro" className="button button-primary">
                  Criar conta <ArrowUpRight size={17} aria-hidden="true" />
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      {error && (
        <div className="container mt-4">
          <Feedback>
            {error}{" "}
            <button className="text-link" onClick={() => setError("")}>
              Fechar
            </button>
          </Feedback>
        </div>
      )}
      <main id="conteudo">{children}</main>
      <footer className="site-footer">
        <div className="container footer-inner">
          <div>
            <Brand />
            <p>Iniciativas locais. Possibilidades coletivas.</p>
          </div>
          <div className="footer-note">
            <strong>Informação que aproxima.</strong>
            <p>
              Uma iniciativa acadêmica de Engenharia de Software.
              <br />
              Alinhada ao ODS 16: transparência e acesso à informação.
            </p>
            <a
              href="https://brasil.un.org/pt-br/sdgs/16"
              target="_blank"
              rel="noreferrer"
            >
              Conheça o ODS 16 <ArrowUpRight size={15} aria-hidden="true" />
            </a>
          </div>
        </div>
      </footer>
    </>
  );
}

"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { ArrowRight, Eye, EyeOff, Sprout } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useForm } from "@/hooks/use-form";
import { Mosaic } from "./mosaic";
import { Button, Feedback, Field } from "./ui";

export function AuthForm({ mode }: { mode: "login" | "cadastro" }) {
  const { api, user, setUser } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const register = mode === "cadastro";
  const [show, setShow] = useState(false);
  const form = useForm(mode, async (values) => {
    if (register) {
      await api.auth.register({
        nome: values.nome.trim(),
        email: values.email.trim(),
        senha: values.senha,
      });
      router.push(
        `/login?cadastro=sucesso&email=${encodeURIComponent(values.email.trim())}`,
      );
    } else {
      const account = await api.auth.login({
        email: values.email.trim(),
        senha: values.senha,
      });
      setUser(account);
      router.push("/gestao");
    }
  });
  return (
    <section className="container auth-page">
      <div className="auth-story">
        <span className="eyebrow">
          <Sprout size={17} />
          CADA PESSOA FAZ PARTE
        </span>
        <h1>
          {register ? (
            <>
              Grandes mudanças.
              <br />
              Começos simples.
            </>
          ) : (
            <>
              Que bom ter
              <br />
              você por perto.
            </>
          )}
        </h1>
        <p>
          Um lugar para dar visibilidade a iniciativas,
          <br className="hidden md:block" /> compartilhar caminhos e fortalecer
          a comunidade.
        </p>
        <Mosaic compact />
      </div>
      <div className="auth-panel">
        <span className="eyebrow">PROJETOS SOCIAIS</span>
        <h2>{register ? "Vamos começar?" : "Entre na sua conta"}</h2>
        <p>
          {register
            ? "Crie sua conta para organizar e compartilhar suas iniciativas."
            : "Acompanhe suas organizações e dê o próximo passo nos seus projetos."}
        </p>
        {user ? (
          <Feedback kind="info">
            Você já está conectado como {user.nome}.{" "}
            <Link href="/gestao" className="text-link">
              Ir para minha gestão
            </Link>
          </Feedback>
        ) : (
          <>
            {!register && params.get("cadastro") === "sucesso" && (
              <Feedback kind="success">
                Conta criada. Entre para publicar sua primeira iniciativa.
              </Feedback>
            )}
            {api.mode === "demo" && (
              <Feedback kind="info">
                Use dados fictícios. O login é simulado, sem verificar ou
                guardar senhas. Experimente <strong>ana@example.org</strong> com
                uma senha fictícia de 8 caracteres.
              </Feedback>
            )}
            <form onSubmit={form.submit} noValidate className="form-stack">
              {register && (
                <Field name="nome" label="Seu nome" error={form.fields.nome}>
                  <input
                    {...form.attrs("nome")}
                    autoComplete="name"
                    maxLength={100}
                    placeholder="Como podemos chamar você?"
                  />
                </Field>
              )}
              <Field name="email" label="E-mail" error={form.fields.email}>
                <input
                  {...form.attrs("email")}
                  type="email"
                  autoComplete="email"
                  maxLength={254}
                  defaultValue={register ? "" : params.get("email") || ""}
                  placeholder="voce@exemplo.com"
                />
              </Field>
              <Field
                name="senha"
                label="Senha"
                error={form.fields.senha}
                hint={
                  register
                    ? "Pelo menos 8 caracteres. Máximo de 72 bytes."
                    : undefined
                }
              >
                <div className="password-input">
                  <input
                    {...form.attrs("senha")}
                    type={show ? "text" : "password"}
                    autoComplete={
                      register ? "new-password" : "current-password"
                    }
                    maxLength={72}
                    placeholder={register ? "Crie uma senha" : "Sua senha"}
                  />
                  <button
                    type="button"
                    onClick={() => setShow(!show)}
                    aria-label={show ? "Ocultar senha" : "Mostrar senha"}
                  >
                    {show ? <EyeOff size={19} /> : <Eye size={19} />}
                  </button>
                </div>
              </Field>
              {register && (
                <Field
                  name="confirmacao"
                  label="Confirme a senha"
                  error={form.fields.confirmacao}
                >
                  <input
                    {...form.attrs("confirmacao")}
                    type={show ? "text" : "password"}
                    autoComplete="new-password"
                    maxLength={72}
                    placeholder="Digite a senha novamente"
                  />
                </Field>
              )}
              {form.error && <Feedback>{form.error}</Feedback>}
              <Button type="submit" busy={form.busy} className="w-full">
                {register ? "Criar minha conta" : "Entrar"}
                <ArrowRight size={18} />
              </Button>
            </form>
            <p className="auth-switch">
              {register ? "Já faz parte?" : "Ainda não tem uma conta?"}{" "}
              <Link href={register ? "/login" : "/cadastro"}>
                {register ? "Entre na sua conta" : "Comece por aqui"}
              </Link>
            </p>
          </>
        )}
      </div>
    </section>
  );
}

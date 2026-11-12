import { describe, expect, it, vi } from "vitest";
import { createHttpServices } from "./http";
import { ApiError } from "@/lib/errors";

function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
const token = { headerName: "X-CSRF-TOKEN", token: "token-original" };
const user = { id: "1", nome: "Ana", email: "ana@example.org" };

describe("integração HTTP", () => {
  it("rotaciona CSRF depois do login e compartilha a sessão por cookie", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(json(user))
      .mockResolvedValueOnce(json({ ...token, token: "novo" }))
      .mockResolvedValueOnce(json({ id: "org" }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(json(user));
    const api = createHttpServices("http://localhost:8080/api/", fetcher);
    await api.auth.login({ email: user.email, senha: "Senha123!" });
    await api.organizations.create({
      nome: "Org",
      descricao: "Descrição",
      emailContato: user.email,
    });
    expect(fetcher.mock.calls[1][1]).toMatchObject({
      credentials: "include",
      method: "POST",
      headers: { "X-CSRF-TOKEN": "token-original" },
    });
    expect(fetcher.mock.calls[3][1]).toMatchObject({
      headers: { "X-CSRF-TOKEN": "novo" },
    });
    await expect(api.auth.logout()).resolves.toBeUndefined();
    await api.auth.register({
      nome: "Ana",
      email: user.email,
      senha: "Senha123!",
    });
    expect(fetcher.mock.calls[5][0]).toBe(
      "http://localhost:8080/api/auth/csrf",
    );
  });

  it("envia filtros e trata a publicação como resposta do projeto completo", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(json([]))
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(
        json({ id: "p", atualizacoes: [{ descricao: "Oficina" }] }, 201),
      );
    const api = createHttpServices("/api", fetcher);
    await api.projects.list({ nome: "  Edu.*  ", status: "EM_ANDAMENTO" });
    expect(fetcher.mock.calls[0][0]).toBe(
      "/api/projetos?nome=Edu.*&status=EM_ANDAMENTO",
    );
    expect(await api.projects.publish("p", "Oficina")).toMatchObject({
      id: "p",
      atualizacoes: [{ descricao: "Oficina" }],
    });
    expect(fetcher.mock.calls[2][1]?.body).toBe('{"descricao":"Oficina"}');
  });

  it("encaminha CRUD e listas privadas para seus endpoints reais", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockImplementation(async (url) =>
        String(url).endsWith("/auth/csrf") ? json(token) : json({}),
      );
    const api = createHttpServices("/api", fetcher);
    const org = {
      nome: "Org",
      descricao: "Descrição",
      emailContato: user.email,
    };
    const project = {
      ...org,
      organizacaoId: "org",
      dataInicio: "2026-11-01",
      dataFim: null,
      status: "PLANEJADO" as const,
    };
    await api.auth.me();
    await api.organizations.mine();
    await api.organizations.update("o", org);
    await api.organizations.remove("o");
    await api.projects.get("a/b");
    await api.projects.mine();
    await api.projects.create(project);
    await api.projects.update("p", project);
    await api.projects.remove("p");
    expect(fetcher.mock.calls.map((call) => call[0])).toEqual(
      expect.arrayContaining([
        "/api/me/organizacoes",
        "/api/me/projetos",
        "/api/projetos/a%2Fb",
        "/api/organizacoes/o",
        "/api/projetos/p",
      ]),
    );
  });

  it("apresenta erros de campo, sessão e rede sem reenviar escritas", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(json({}, 401))
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(
        json({ detail: "Confira", erros: { nome: ["Obrigatório"] } }, 400),
      );
    const api = createHttpServices("/api", fetcher);
    expect(await api.auth.me()).toBeNull();
    await expect(
      api.organizations.create({ nome: "", descricao: "", emailContato: "" }),
    ).rejects.toMatchObject({
      status: 400,
      message: "Confira",
      fields: { nome: ["Obrigatório"] },
    });
    expect(fetcher).toHaveBeenCalledTimes(3);
    fetcher.mockRejectedValueOnce(new TypeError("fetch failed"));
    await expect(api.projects.list()).rejects.toMatchObject({ status: 0 });
    const abort = new DOMException("Aborted", "AbortError");
    fetcher.mockRejectedValueOnce(abort);
    await expect(api.projects.list()).rejects.toBe(abort);
  });

  it("renova token após 403 sem repetir a operação e distingue falha na sessão", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(new Response("Denied", { status: 403 }))
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(json({}, 500));
    const api = createHttpServices("/api", fetcher);
    await expect(api.projects.remove("p")).rejects.toBeInstanceOf(ApiError);
    expect(fetcher).toHaveBeenCalledTimes(2);
    await api.projects.remove("p");
    expect(fetcher.mock.calls[2][0]).toBe("/api/auth/csrf");
    await expect(api.auth.me()).rejects.toMatchObject({ status: 500 });
  });

  it("permite tentar novamente quando a obtenção do CSRF falha", async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce(json(token))
      .mockResolvedValueOnce(json(user));
    const api = createHttpServices("/api", fetcher);
    await expect(
      api.auth.login({ email: user.email, senha: "teste" }),
    ).rejects.toBeInstanceOf(ApiError);
    await expect(
      api.auth.login({ email: user.email, senha: "teste" }),
    ).resolves.toEqual(user);
  });
});

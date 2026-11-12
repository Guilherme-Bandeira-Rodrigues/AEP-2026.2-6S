import { describe, expect, it } from "vitest";
import { createDemoServices } from "./demo";

describe("demonstração isolada", () => {
  it("mantém alterações, vínculos e histórico, sem persistir senhas", async () => {
    const api = createDemoServices();
    expect(await api.auth.me()).toBeNull();
    await expect(api.projects.mine()).rejects.toMatchObject({ status: 401 });
    const user = await api.auth.register({
      nome: " Pessoa ",
      email: " TESTE@example.org ",
      senha: "NaoPersistir123!",
    });
    expect(user.email).toBe("teste@example.org");
    await api.auth.login({ email: user.email, senha: "simulada" });
    const org = await api.organizations.create({
      nome: "Org",
      descricao: "Coletivo",
      emailContato: user.email,
    });
    const p = await api.projects.create({
      nome: "Projeto de teste",
      descricao: "Atividades na comunidade",
      emailContato: user.email,
      organizacaoId: org.id,
      dataInicio: "2026-11-01",
      dataFim: null,
      status: "PLANEJADO",
    });
    await api.projects.publish(p.id, "  Novo encontro  ");
    await api.projects.update(p.id, {
      ...p,
      organizacaoId: org.id,
      status: "EM_ANDAMENTO",
    });
    await api.organizations.update(org.id, {
      ...org,
      nome: "Organização atualizada",
    });
    const next = createDemoServices();
    expect((await next.projects.get(p.id)).organizacao?.nome).toBe(
      "Organização atualizada",
    );
    expect((await next.projects.get(p.id)).atualizacoes[0].descricao).toBe(
      "Novo encontro",
    );
    expect(
      await next.projects.list({ nome: " de teste ", status: "EM_ANDAMENTO" }),
    ).toHaveLength(1);
    expect(await next.projects.mine()).toHaveLength(1);
    expect(await next.organizations.mine()).toHaveLength(1);
    expect(localStorage.getItem("projetos-sociais.demo.v1")).not.toContain(
      "NaoPersistir123!",
    );
    await expect(api.organizations.remove(org.id)).rejects.toMatchObject({
      status: 409,
    });
    await api.projects.remove(p.id);
    await api.organizations.remove(org.id);
    expect(await api.projects.mine()).toEqual([]);
    await api.auth.logout();
    expect(await api.auth.me()).toBeNull();
  });

  it("não permite apropriação de registros de outra conta", async () => {
    const api = createDemoServices();
    const p = (await api.projects.list())[0];
    await api.auth.register({
      nome: "Outra pessoa",
      email: "outro@example.org",
      senha: "ficticia",
    });
    await api.auth.login({ email: "outro@example.org", senha: "ficticia" });
    expect(await api.projects.mine()).toEqual([]);
    const org = await api.organizations.create({
      nome: "Outra",
      descricao: "Descrição",
      emailContato: "outro@example.org",
    });
    await expect(
      api.projects.update(p.id, { ...p, organizacaoId: org.id }),
    ).rejects.toMatchObject({ status: 403 });
    await expect(api.projects.remove(p.id)).rejects.toMatchObject({
      status: 403,
    });
    await expect(api.projects.publish(p.id, "Indevida")).rejects.toMatchObject({
      status: 403,
    });
    await expect(
      api.organizations.remove(p.organizacaoId!),
    ).rejects.toMatchObject({ status: 403 });
    await expect(api.projects.get("ausente")).rejects.toMatchObject({
      status: 404,
    });
  });

  it("trata cadastro repetido, dados corrompidos e restauração explícita", async () => {
    const api = createDemoServices();
    await expect(
      api.auth.login({ email: "ausente@example.org", senha: "ficticia" }),
    ).rejects.toMatchObject({ status: 401 });
    await expect(
      api.auth.register({
        nome: "Ana",
        email: "ANA@example.org",
        senha: "ficticia",
      }),
    ).rejects.toMatchObject({ status: 409 });
    localStorage.setItem("projetos-sociais.demo.v1", "corrompido");
    await expect(api.projects.list()).rejects.toThrow("Restaure os dados");
    await api.resetDemo?.();
    expect(await api.projects.list()).toHaveLength(3);
  });
});

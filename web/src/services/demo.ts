import { ApiError } from "@/lib/errors";
import type { Organizacao, Projeto } from "@/types/domain";
import type { Services } from "./contracts";
import { demoFixtures, type DemoData } from "./fixtures";

const KEY = "projetos-sociais.demo.v1";
export function createDemoServices(
  storage: () => Storage = () => window.localStorage,
): Services {
  function read(): DemoData {
    try {
      const raw = storage().getItem(KEY);
      if (!raw) {
        const initial = demoFixtures();
        save(initial);
        return initial;
      }
      const data = JSON.parse(raw) as DemoData;
      if (
        data.version !== 1 ||
        !Array.isArray(data.users) ||
        !Array.isArray(data.projects) ||
        !Array.isArray(data.organizations)
      )
        throw new Error();
      return data;
    } catch {
      throw new ApiError(
        0,
        "Não foi possível ler a demonstração. Restaure os dados pelo aviso no topo.",
      );
    }
  }
  function save(data: DemoData) {
    try {
      storage().setItem(KEY, JSON.stringify(data));
    } catch {
      throw new ApiError(0, "O navegador não permitiu salvar a demonstração.");
    }
  }
  function current(data: DemoData) {
    const user = data.users.find((item) => item.id === data.sessionId);
    if (!user) throw new ApiError(401, "Entre para acessar sua gestão.");
    return user;
  }
  function owned(data: DemoData, id: string | null) {
    const user = current(data);
    const org = data.organizations.find((item) => item.id === id);
    if (!org || org.proprietarioId !== user.id)
      throw new ApiError(403, "A organização não pertence à sua conta.");
    return org;
  }
  function project(data: DemoData, id: string) {
    const value = data.projects.find((item) => item.id === id);
    if (!value) throw new ApiError(404, "Projeto não encontrado.");
    return value;
  }
  function publicOrg(org: Organizacao): Organizacao {
    return {
      id: org.id,
      nome: org.nome,
      descricao: org.descricao,
      emailContato: org.emailContato,
    };
  }
  function hydrated(data: DemoData, value: Projeto): Projeto {
    const org = data.organizations.find(
      (item) => item.id === value.organizacaoId,
    );
    return { ...value, organizacao: org ? publicOrg(org) : null };
  }
  const id = () => crypto.randomUUID().replaceAll("-", "").slice(0, 24);
  return {
    mode: "demo",
    auth: {
      async me() {
        const data = read();
        return data.users.find((user) => user.id === data.sessionId) || null;
      },
      async register(input) {
        const data = read();
        const email = input.email.trim().toLowerCase();
        if (data.users.some((user) => user.email === email))
          throw new ApiError(409, "Já existe uma conta com este e-mail.");
        // Credenciais não são armazenadas. Este adapter não implementa autenticação real.
        const user = { id: id(), nome: input.nome.trim(), email };
        data.users.push(user);
        save(data);
        return user;
      },
      async login(input) {
        const data = read();
        const user = data.users.find(
          (item) => item.email === input.email.trim().toLowerCase(),
        );
        if (!user)
          throw new ApiError(
            401,
            "Conta demonstrativa não encontrada. Crie uma conta fictícia.",
          );
        data.sessionId = user.id;
        save(data);
        return user;
      },
      async logout() {
        const data = read();
        data.sessionId = null;
        save(data);
      },
    },
    projects: {
      async list(filters = {}) {
        const data = read();
        return data.projects
          .filter(
            (p) =>
              (!filters.nome ||
                p.nome
                  .toLocaleLowerCase("pt-BR")
                  .includes(filters.nome.trim().toLocaleLowerCase("pt-BR"))) &&
              (!filters.status || p.status === filters.status),
          )
          .map((p) => hydrated(data, p));
      },
      async get(projectId) {
        const data = read();
        return hydrated(data, project(data, projectId));
      },
      async mine() {
        const data = read();
        const user = current(data);
        const ids = new Set(
          data.organizations
            .filter((o) => o.proprietarioId === user.id)
            .map((o) => o.id),
        );
        return data.projects
          .filter((p) => ids.has(p.organizacaoId || ""))
          .map((p) => hydrated(data, p));
      },
      async create(input) {
        const data = read();
        const org = owned(data, input.organizacaoId);
        const now = new Date().toISOString();
        const value: Projeto = {
          ...input,
          id: id(),
          organizacao: publicOrg(org),
          criadoEm: now,
          atualizadoEm: now,
          atualizacoes: [],
        };
        data.projects.push(value);
        save(data);
        return value;
      },
      async update(projectId, input) {
        const data = read();
        const value = project(data, projectId);
        owned(data, value.organizacaoId);
        owned(data, input.organizacaoId);
        Object.assign(value, {
          nome: input.nome,
          descricao: input.descricao,
          emailContato: input.emailContato,
          organizacaoId: input.organizacaoId,
          dataInicio: input.dataInicio,
          dataFim: input.dataFim,
          status: input.status,
          atualizadoEm: new Date().toISOString(),
        });
        save(data);
        return hydrated(data, value);
      },
      async remove(projectId) {
        const data = read();
        owned(data, project(data, projectId).organizacaoId);
        data.projects = data.projects.filter((p) => p.id !== projectId);
        save(data);
      },
      async publish(projectId, descricao) {
        const data = read();
        const value = project(data, projectId);
        owned(data, value.organizacaoId);
        const now = new Date().toISOString();
        value.atualizacoes.push({ data: now, descricao: descricao.trim() });
        value.atualizadoEm = now;
        save(data);
        return hydrated(data, value);
      },
    },
    organizations: {
      async mine() {
        const data = read();
        const user = current(data);
        return data.organizations
          .filter((o) => o.proprietarioId === user.id)
          .map(publicOrg);
      },
      async create(input) {
        const data = read();
        const value = { ...input, id: id(), proprietarioId: current(data).id };
        data.organizations.push(value);
        save(data);
        return publicOrg(value);
      },
      async update(orgId, input) {
        const data = read();
        const value = owned(data, orgId);
        Object.assign(value, input);
        save(data);
        return publicOrg(value);
      },
      async remove(orgId) {
        const data = read();
        owned(data, orgId);
        if (data.projects.some((p) => p.organizacaoId === orgId))
          throw new ApiError(
            409,
            "Esta organização possui projetos vinculados.",
          );
        data.organizations = data.organizations.filter((o) => o.id !== orgId);
        save(data);
      },
    },
    async resetDemo() {
      save(demoFixtures());
    },
  };
}

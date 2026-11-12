import { ApiError } from "@/lib/errors";
import type { Services } from "./contracts";
import type { ErrosCampos, Filtros } from "@/types/domain";

type Csrf = { headerName: string; token: string };

export function createHttpServices(
  baseUrl: string,
  transport: typeof fetch = (...args) => fetch(...args),
): Services {
  const base = baseUrl.replace(/\/$/, "");
  let csrf: Promise<Csrf> | undefined;

  async function request<T>(
    path: string,
    method = "GET",
    body?: unknown,
    signal?: AbortSignal,
  ): Promise<T> {
    const headers: Record<string, string> = { Accept: "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (method !== "GET") {
      csrf ??= request<Csrf>("/auth/csrf").catch((error) => {
        csrf = undefined;
        throw error;
      });
      const token = await csrf;
      headers[token.headerName] = token.token;
    }
    let response: Response;
    try {
      response = await transport(`${base}${path}`, {
        method,
        headers,
        credentials: "include",
        cache: "no-store",
        signal,
        ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
      });
    } catch (error) {
      if (
        error &&
        typeof error === "object" &&
        "name" in error &&
        error.name === "AbortError"
      )
        throw error;
      throw new ApiError(
        0,
        "Não foi possível conectar. Verifique sua conexão e tente novamente.",
      );
    }
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) csrf = undefined;
      const problem = (await response.json().catch(() => ({}))) as {
        detail?: string;
        message?: string;
        erros?: ErrosCampos;
      };
      const fallback =
        response.status === 401
          ? "Sua sessão terminou. Entre novamente."
          : response.status === 403
            ? "Você não tem permissão ou sua sessão precisa ser renovada."
            : "Não foi possível concluir. Tente novamente.";
      throw new ApiError(
        response.status,
        problem.detail || problem.message || fallback,
        problem.erros,
      );
    }
    return response.status === 204
      ? (undefined as T)
      : (response.json() as Promise<T>);
  }

  function query(filters: Filtros = {}) {
    const params = new URLSearchParams();
    if (filters.nome?.trim()) params.set("nome", filters.nome.trim());
    if (filters.status) params.set("status", filters.status);
    return params.size ? `?${params}` : "";
  }

  return {
    mode: "api",
    auth: {
      async me() {
        try {
          return await request("/auth/me");
        } catch (error) {
          if (error instanceof ApiError && error.status === 401) return null;
          throw error;
        }
      },
      register: (input) => request("/auth/cadastro", "POST", input),
      async login(input) {
        const user = await request<
          Awaited<ReturnType<Services["auth"]["login"]>>
        >("/auth/login", "POST", input);
        // Login rotaciona a sessão e invalida o token. A próxima escrita busca um novo.
        csrf = undefined;
        return user;
      },
      async logout() {
        await request<void>("/auth/logout", "POST");
        csrf = undefined;
      },
    },
    projects: {
      list: (filters, signal) =>
        request(`/projetos${query(filters)}`, "GET", undefined, signal),
      get: (id, signal) =>
        request(
          `/projetos/${encodeURIComponent(id)}`,
          "GET",
          undefined,
          signal,
        ),
      mine: () => request("/me/projetos"),
      create: (input) => request("/projetos", "POST", input),
      update: (id, input) =>
        request(`/projetos/${encodeURIComponent(id)}`, "PUT", input),
      remove: (id) => request(`/projetos/${encodeURIComponent(id)}`, "DELETE"),
      publish: (id, descricao) =>
        request(`/projetos/${encodeURIComponent(id)}/atualizacoes`, "POST", {
          descricao,
        }),
    },
    organizations: {
      mine: () => request("/me/organizacoes"),
      create: (input) => request("/organizacoes", "POST", input),
      update: (id, input) =>
        request(`/organizacoes/${encodeURIComponent(id)}`, "PUT", input),
      remove: (id) =>
        request(`/organizacoes/${encodeURIComponent(id)}`, "DELETE"),
    },
  };
}

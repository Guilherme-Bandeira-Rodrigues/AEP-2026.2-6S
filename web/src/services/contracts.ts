import type {
  CadastroInput,
  Filtros,
  LoginInput,
  Organizacao,
  OrganizacaoInput,
  Projeto,
  ProjetoInput,
  Usuario,
} from "@/types/domain";

export interface Services {
  mode: "api" | "demo";
  auth: {
    me(): Promise<Usuario | null>;
    register(input: CadastroInput): Promise<Usuario>;
    login(input: LoginInput): Promise<Usuario>;
    logout(): Promise<void>;
  };
  projects: {
    list(filters?: Filtros, signal?: AbortSignal): Promise<Projeto[]>;
    get(id: string, signal?: AbortSignal): Promise<Projeto>;
    mine(): Promise<Projeto[]>;
    create(input: ProjetoInput): Promise<Projeto>;
    update(id: string, input: ProjetoInput): Promise<Projeto>;
    remove(id: string): Promise<void>;
    publish(id: string, descricao: string): Promise<Projeto>;
  };
  organizations: {
    mine(): Promise<Organizacao[]>;
    create(input: OrganizacaoInput): Promise<Organizacao>;
    update(id: string, input: OrganizacaoInput): Promise<Organizacao>;
    remove(id: string): Promise<void>;
  };
  resetDemo?: () => Promise<void>;
}

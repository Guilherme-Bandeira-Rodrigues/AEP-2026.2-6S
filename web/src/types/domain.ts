export type StatusProjeto =
  "PLANEJADO" | "EM_ANDAMENTO" | "CONCLUIDO" | "CANCELADO";
export const STATUS: Record<StatusProjeto, string> = {
  PLANEJADO: "Planejado",
  EM_ANDAMENTO: "Em andamento",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
};
export interface Usuario {
  id: string;
  nome: string;
  email: string;
}
export interface Organizacao {
  id: string;
  nome: string;
  descricao: string;
  emailContato: string;
}
export interface AtualizacaoProjeto {
  data: string;
  descricao: string;
}
export interface Projeto {
  id: string;
  nome: string;
  descricao: string;
  organizacaoId: string | null;
  organizacao: Organizacao | null;
  emailContato: string;
  dataInicio: string;
  dataFim: string | null;
  status: StatusProjeto;
  criadoEm: string;
  atualizadoEm: string;
  atualizacoes: AtualizacaoProjeto[];
}
export interface ProjetoInput {
  nome: string;
  descricao: string;
  organizacaoId: string;
  emailContato: string;
  dataInicio: string;
  dataFim: string | null;
  status: StatusProjeto;
}
export type OrganizacaoInput = Omit<Organizacao, "id">;
export interface CadastroInput {
  nome: string;
  email: string;
  senha: string;
}
export interface LoginInput {
  email: string;
  senha: string;
}
export interface Filtros {
  nome?: string;
  status?: StatusProjeto | "";
}
export type ErrosCampos = Record<string, string[]>;

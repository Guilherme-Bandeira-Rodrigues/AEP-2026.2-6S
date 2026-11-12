import type { ErrosCampos } from "@/types/domain";

export function validate(
  kind: "login" | "cadastro" | "organizacao" | "projeto" | "atualizacao",
  values: Record<string, string>,
): ErrosCampos {
  const errors: ErrosCampos = {};
  function length(key: string, label: string, min: number, max: number) {
    const size = (values[key] || "").trim().length;
    if (size < min || size > max)
      errors[key] = [`${label}: informe entre ${min} e ${max} caracteres.`];
  }
  function email(key: string) {
    if (
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values[key]?.trim() || "") ||
      values[key].length > 254
    )
      errors[key] = ["Informe um e-mail válido."];
  }
  if (kind === "login" || kind === "cadastro") {
    email("email");
    const password = values.senha || "";
    if (
      !password.trim() ||
      new TextEncoder().encode(password).length > 72 ||
      (kind === "cadastro" && password.length < 8)
    )
      errors.senha = [
        "Use uma senha de 8 caracteres ou mais, com até 72 bytes.",
      ];
    if (kind === "cadastro") {
      length("nome", "Nome", 3, 100);
      if (values.confirmacao !== password)
        errors.confirmacao = ["As senhas precisam ser iguais."];
    }
  } else if (kind === "atualizacao") {
    length("descricao", "Atualização", 1, 1000);
  } else {
    length(
      "nome",
      "Nome",
      kind === "projeto" ? 3 : 1,
      kind === "projeto" ? 120 : 150,
    );
    length("descricao", "Descrição", kind === "projeto" ? 10 : 1, 1000);
    email("emailContato");
    if (kind === "projeto") {
      if (!values.organizacaoId)
        errors.organizacaoId = ["Selecione a organização responsável."];
      if (!values.dataInicio) errors.dataInicio = ["Informe a data de início."];
      if (
        values.dataFim &&
        values.dataInicio &&
        values.dataFim < values.dataInicio
      )
        errors.dataFim = ["A data final não pode ser anterior ao início."];
      if (
        !["PLANEJADO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO"].includes(
          values.status,
        )
      )
        errors.status = ["Selecione um status válido."];
    }
  }
  return errors;
}

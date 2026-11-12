import { expect, it } from "vitest";
import { validate } from "./validation";
import { dateLabel } from "./format";
import { errorMessage, fieldErrors } from "./errors";

it("valida cadastro, senha UTF-8, confirmação e campos obrigatórios", () => {
  expect(
    Object.keys(
      validate("cadastro", {
        nome: "A",
        email: "invalido",
        senha: "curta",
        confirmacao: "outra",
      }),
    ),
  ).toEqual(["email", "senha", "nome", "confirmacao"]);
  expect(
    validate("cadastro", {
      nome: "Ana Silva",
      email: "ana@example.org",
      senha: "á".repeat(37),
      confirmacao: "á".repeat(37),
    }),
  ).toHaveProperty("senha");
  expect(
    validate("login", { email: "ana@example.org", senha: "Senha123!" }),
  ).toEqual({});
  expect(
    validate("organizacao", {
      nome: "Organização",
      descricao: "Descrição",
      emailContato: "contato@example.org",
    }),
  ).toEqual({});
  expect(validate("atualizacao", { descricao: " " })).toHaveProperty(
    "descricao",
  );
});
it("impede projeto sem organização, status inválido ou datas invertidas", () => {
  const data = {
    nome: "Projeto",
    descricao: "Descrição do projeto",
    emailContato: "p@example.org",
    organizacaoId: "",
    dataInicio: "2026-11-02",
    dataFim: "2026-11-01",
    status: "OUTRO",
  };
  expect(validate("projeto", data)).toHaveProperty("dataFim");
  expect(validate("projeto", { ...data, dataInicio: "" })).toHaveProperty(
    "dataInicio",
  );
  expect(
    validate("projeto", {
      ...data,
      organizacaoId: "org",
      dataFim: "",
      status: "PLANEJADO",
    }),
  ).toEqual({});
});
it("formata datas de calendário sem mudar o dia e suporta valores ausentes", () => {
  expect(dateLabel("2026-11-01")).toContain("01");
  expect(dateLabel(null)).toBe("Não informada");
  expect(dateLabel("inválido")).toBe("Não informada");
  expect(errorMessage(null)).toContain("Tente novamente");
  expect(fieldErrors(new Error())).toEqual({});
});

import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { AuthProvider } from "@/contexts/auth-context";
import { createDemoServices } from "@/services/demo";
import { ApiError } from "@/lib/errors";
import { ExplorePage, ProjectDetail } from "./projects";
import { AuthForm } from "./auth-form";
import { ManagementPage } from "./management";
import { Shell } from "./shell";

const nav = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  search: new URLSearchParams(),
  pathname: "/",
}));
vi.mock("next/navigation", () => ({
  useRouter: () => nav,
  useSearchParams: () => nav.search,
  usePathname: () => nav.pathname,
}));
beforeEach(() => {
  nav.push.mockClear();
  nav.replace.mockClear();
  nav.search = new URLSearchParams();
});
const projectId = "100000000000000000000001";
function mount(children: ReactNode, api = createDemoServices()) {
  return render(<AuthProvider service={api}>{children}</AuthProvider>);
}
async function fill(label: string, value: string) {
  const field = screen.getByLabelText(label, { exact: true });
  await userEvent.clear(field);
  if (value) await userEvent.type(field, value);
}
async function choose(label: string, option: string) {
  await userEvent.click(
    screen.getByRole("button", { name: new RegExp(label) }),
  );
  await userEvent.click(await screen.findByRole("option", { name: option }));
}

describe("consulta pública", () => {
  it("pesquisa, combina status e limpa filtros", async () => {
    mount(<ExplorePage />);
    expect(
      await screen.findByText("3 iniciativas encontradas"),
    ).toBeInTheDocument();
    await fill("Pesquisar pelo nome", "Cultura");
    await choose("Situação do projeto", "Planejado");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));
    expect(
      await screen.findByText("1 iniciativa encontrada"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Aprender em comunidade"),
    ).not.toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Limpar filtros" }),
    );
    expect(
      await screen.findByText("3 iniciativas encontradas"),
    ).toBeInTheDocument();
    await fill("Pesquisar pelo nome", "inexistente");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));
    expect(
      await screen.findByText("Nenhum projeto com esses filtros"),
    ).toBeInTheDocument();
  });

  it("recupera consulta após falha sem apresentar projetos fictícios", async () => {
    const api = createDemoServices();
    vi.spyOn(api.projects, "list").mockRejectedValueOnce(
      new Error("Sem conexão"),
    );
    mount(<ExplorePage />, api);
    expect(await screen.findByRole("alert")).toHaveTextContent("Sem conexão");
    expect(
      screen.queryByText("Aprender em comunidade"),
    ).not.toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );
    expect(
      await screen.findByText("3 iniciativas encontradas"),
    ).toBeInTheDocument();
  });

  it("mostra detalhes, contato e histórico em ordem decrescente", async () => {
    const api = createDemoServices();
    await api.auth.login({ email: "ana@example.org", senha: "ficticia" });
    await api.projects.publish(projectId, "Uma nova etapa começou.");
    mount(<ProjectDetail id={projectId} />, api);
    expect(
      await screen.findByRole("heading", { name: "Aprender em comunidade" }),
    ).toBeInTheDocument();
    const items = screen.getAllByRole("listitem");
    expect(items[0]).toHaveTextContent("Uma nova etapa começou.");
    expect(
      screen.getByRole("link", { name: "aprender@example.org" }),
    ).toHaveAttribute("href", "mailto:aprender@example.org");
  });

  it("trata projeto ausente e histórico vazio", async () => {
    const view = mount(<ProjectDetail id="inexistente" />);
    expect(
      await screen.findByText("Projeto não encontrado"),
    ).toBeInTheDocument();
    view.unmount();
    mount(<ProjectDetail id="100000000000000000000002" />);
    expect(
      await screen.findByText("Este projeto ainda não publicou atualizações."),
    ).toBeInTheDocument();
  });
});

describe("conta e sessão", () => {
  it("valida o cadastro e encaminha para entrar após sucesso", async () => {
    const api = createDemoServices();
    mount(<AuthForm mode="cadastro" />, api);
    await userEvent.click(
      screen.getByRole("button", { name: "Criar minha conta" }),
    );
    expect(
      await screen.findByText("Confira os campos destacados."),
    ).toBeInTheDocument();
    await fill("Seu nome", "Pessoa Teste");
    await fill("E-mail", "pessoa@example.org");
    await fill("Senha", "Senha123!");
    await fill("Confirme a senha", "Senha123!");
    await userEvent.click(
      screen.getByRole("button", { name: "Mostrar senha" }),
    );
    expect(screen.getByLabelText("Senha", { exact: true })).toHaveAttribute(
      "type",
      "text",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Ocultar senha" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Criar minha conta" }),
    );
    await waitFor(() =>
      expect(nav.push).toHaveBeenCalledWith(
        "/login?cadastro=sucesso&email=pessoa%40example.org",
      ),
    );
    expect(await api.auth.me()).toBeNull();
  });

  it("trata falha de login, entra e encerra a sessão pelo cabeçalho", async () => {
    const api = createDemoServices();
    const view = mount(<AuthForm mode="login" />, api);
    await fill("E-mail", "ausente@example.org");
    await fill("Senha", "Senha123!");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
    expect(
      await screen.findByText(/Conta demonstrativa não encontrada/),
    ).toBeInTheDocument();
    await fill("E-mail", "ana@example.org");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
    await waitFor(() => expect(nav.push).toHaveBeenCalledWith("/gestao"));
    view.unmount();
    mount(
      <Shell>
        <p>Conteúdo</p>
      </Shell>,
      api,
    );
    await userEvent.click(
      await screen.findByRole("button", { name: "Sair da conta" }),
    );
    expect(
      await screen.findByRole("link", { name: "Entrar" }),
    ).toBeInTheDocument();
    expect(await api.auth.me()).toBeNull();
  });

  it("redireciona visitante e permite recuperar uma falha ao verificar sessão", async () => {
    const api = createDemoServices();
    vi.spyOn(api.auth, "me").mockRejectedValueOnce(
      new Error("Falha de conexão"),
    );
    mount(<ManagementPage />, api);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Falha de conexão",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Verificar conexão novamente" }),
    );
    await waitFor(() => expect(nav.replace).toHaveBeenCalledWith("/login"));
  });
});

describe("gestão da conta", () => {
  it("cadastra organização, cria e edita projeto, publica atualização e exclui com confirmação", async () => {
    const api = createDemoServices();
    await api.auth.register({
      nome: "Pessoa Teste",
      email: "nova@example.org",
      senha: "Senha123!",
    });
    await api.auth.login({ email: "nova@example.org", senha: "Senha123!" });
    mount(<ManagementPage />, api);
    await userEvent.click(
      await screen.findByRole("button", { name: /Cadastrar uma organização/ }),
    );
    await fill("Nome da organização", "Coletivo Teste");
    await fill("Sobre a organização", "Iniciativas do bairro.");
    await fill("E-mail de contato", "contato@example.org");
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar organização" }),
    );
    expect(
      await screen.findByRole("heading", {
        name: "Dê o primeiro passo da sua iniciativa",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Organização responsável/ }),
    ).toHaveTextContent("Coletivo Teste");
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    await userEvent.click(
      screen.getByRole("tab", { name: /Minhas organizações/ }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Editar" }));
    await fill("Nome da organização", "Coletivo Novo");
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar organização" }),
    );
    expect(
      await screen.findByRole("heading", { name: "Coletivo Novo" }),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole("tab", { name: /Meus projetos/ }));
    await userEvent.click(screen.getByRole("button", { name: "Novo projeto" }));
    await fill("Nome do projeto", "Oficina de bairro");
    await fill(
      "Sobre a iniciativa",
      "Oficinas abertas para toda a comunidade.",
    );
    expect(
      screen.getByRole("button", { name: /Organização responsável/ }),
    ).toHaveTextContent("Coletivo Novo");
    await fill("E-mail do projeto", "oficina@example.org");
    const startDate = screen.getByRole("group", {
      name: "Data de início",
    });
    await userEvent.type(
      within(startDate).getByRole("spinbutton", { name: /dia/i }),
      "01",
    );
    await userEvent.type(
      within(startDate).getByRole("spinbutton", { name: /mês/i }),
      "11",
    );
    await userEvent.type(
      within(startDate).getByRole("spinbutton", { name: /ano/i }),
      "2026",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar projeto" }),
    );
    expect(
      await screen.findByRole("heading", { name: "Oficina de bairro" }),
    ).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Editar Oficina de bairro" }),
    );
    await choose("Situação do projeto", "Em andamento");
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar projeto" }),
    );
    expect(await screen.findByText("Em andamento")).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Publicar atualização" }),
    );
    await fill("Atualização do projeto", "Primeiro encontro realizado.");
    await userEvent.click(
      within(
        screen.getByLabelText("Atualização do projeto").closest("form")!,
      ).getByRole("button", { name: "Publicar atualização" }),
    );
    await waitFor(async () =>
      expect((await api.projects.mine())[0].atualizacoes).toHaveLength(1),
    );
    await userEvent.click(
      screen.getByRole("tab", { name: /Minhas organizações/ }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Excluir Coletivo Novo" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar exclusão" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "projetos vinculados",
    );
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    await userEvent.click(screen.getByRole("tab", { name: /Meus projetos/ }));
    await userEvent.click(
      screen.getByRole("button", { name: "Excluir Oficina de bairro" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar exclusão" }),
    );
    expect(
      await screen.findByText("Uma ideia pode ser o começo"),
    ).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("tab", { name: /Minhas organizações/ }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Excluir Coletivo Novo" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar exclusão" }),
    );
    expect(
      await screen.findByText("Pessoas reunidas por um propósito"),
    ).toBeInTheDocument();
  }, 20000);

  it("preserva formulário após erro e encerra acesso quando a sessão expira", async () => {
    const api = createDemoServices();
    await api.auth.login({ email: "ana@example.org", senha: "ficticia" });
    vi.spyOn(api.organizations, "create").mockRejectedValueOnce(
      new ApiError(400, "Confira", { nome: ["Nome inválido"] }),
    );
    mount(<ManagementPage />, api);
    await userEvent.click(
      await screen.findByRole("tab", { name: /Minhas organizações/ }),
    );
    await userEvent.click(
      await screen.findByRole("button", { name: "Nova organização" }),
    );
    await fill("Nome da organização", "Instituto Teste");
    await fill("Sobre a organização", "Descrição");
    await fill("E-mail de contato", "teste@example.org");
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar organização" }),
    );
    expect(await screen.findByText("Nome inválido")).toBeInTheDocument();
    expect(screen.getByLabelText("Nome da organização")).toHaveValue(
      "Instituto Teste",
    );
    vi.mocked(api.organizations.create).mockRejectedValueOnce(
      new ApiError(401, "Sessão expirada"),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Salvar organização" }),
    );
    await waitFor(() => expect(nav.replace).toHaveBeenCalledWith("/login"));
  });
});

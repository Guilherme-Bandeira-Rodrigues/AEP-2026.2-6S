import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DateField, SelectField } from "./form-controls";

describe("controles de formulário", () => {
  it("seleciona com teclado, fecha com Escape e mantém o valor no formulário", async () => {
    const submit = vi.fn();
    render(
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(Object.fromEntries(new FormData(e.currentTarget)));
        }}
      >
        <SelectField
          name="status"
          label="Situação"
          defaultValue="PLANEJADO"
          options={[
            { value: "PLANEJADO", label: "Planejado" },
            { value: "CONCLUIDO", label: "Concluído" },
          ]}
        />
        <button type="submit">Salvar</button>
      </form>,
    );
    const trigger = screen.getByRole("button", { name: /Situação/ });
    trigger.focus();
    await userEvent.keyboard("{Enter}{End}{Enter}");
    expect(trigger).toHaveTextContent("Concluído");
    await userEvent.click(trigger);
    await userEvent.keyboard("{Escape}");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    await waitFor(() => expect(trigger).toHaveFocus());
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(submit).toHaveBeenCalledWith({ status: "CONCLUIDO" });
  });

  it("exibe calendário em português, troca o mês e limpa uma data opcional", async () => {
    const submit = vi.fn();
    render(
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(Object.fromEntries(new FormData(e.currentTarget)));
        }}
      >
        <DateField
          name="dataFim"
          label="Data final"
          defaultValue="2026-11-01"
          optional
        />
        <button type="submit">Salvar</button>
      </form>,
    );
    await userEvent.click(
      screen.getByRole("button", { name: /Escolher data final/ }),
    );
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByRole("heading")).toHaveTextContent(
      "novembro de 2026",
    );
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Próximo mês" }),
    );
    expect(within(dialog).getByRole("heading")).toHaveTextContent(
      "dezembro de 2026",
    );
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Mês anterior" }),
    );
    await userEvent.click(
      within(dialog).getByRole("button", { name: /15 de novembro de 2026/ }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(submit).toHaveBeenLastCalledWith({ dataFim: "2026-11-15" });
    await userEvent.click(
      screen.getByRole("button", { name: /Escolher data final/ }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Deixar sem data final" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(submit).toHaveBeenLastCalledWith({ dataFim: "" });
  });
});

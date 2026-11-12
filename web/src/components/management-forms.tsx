"use client";

import type { Organizacao, Projeto, StatusProjeto } from "@/types/domain";
import { STATUS } from "@/types/domain";
import { useAuth } from "@/contexts/auth-context";
import { useForm } from "@/hooks/use-form";
import { Button, Feedback, Field } from "./ui";
import { DateField, SelectField } from "./form-controls";

interface Actions {
  onSaved(message: string): Promise<void>;
  onCancel(): void;
}
export function OrganizationForm({
  value,
  onSaved,
  onCancel,
}: Actions & { value?: Organizacao }) {
  const { api } = useAuth();
  const form = useForm("organizacao", async (v) => {
    const input = {
      nome: v.nome.trim(),
      descricao: v.descricao.trim(),
      emailContato: v.emailContato.trim(),
    };
    if (value) await api.organizations.update(value.id, input);
    else await api.organizations.create(input);
    await onSaved(
      value
        ? "Organização atualizada."
        : "Organização cadastrada. Agora você pode criar seu primeiro projeto.",
    );
  });
  return (
    <form onSubmit={form.submit} noValidate className="form-stack">
      <h3>
        {value
          ? "Editar organização"
          : "Uma organização, muitas possibilidades"}
      </h3>
      <Field name="nome" label="Nome da organização" error={form.fields.nome}>
        <input
          {...form.attrs("nome")}
          defaultValue={value?.nome}
          maxLength={150}
          autoFocus
        />
      </Field>
      <Field
        name="descricao"
        label="Sobre a organização"
        error={form.fields.descricao}
      >
        <textarea
          {...form.attrs("descricao")}
          defaultValue={value?.descricao}
          rows={3}
          maxLength={1000}
          placeholder="Conte o que a organização faz pela comunidade."
        />
      </Field>
      <Field
        name="emailContato"
        label="E-mail de contato"
        error={form.fields.emailContato}
      >
        <input
          {...form.attrs("emailContato")}
          defaultValue={value?.emailContato}
          type="email"
        />
      </Field>
      {form.error && <Feedback>{form.error}</Feedback>}
      <div className="form-actions">
        <Button type="submit" busy={form.busy}>
          Salvar organização
        </Button>
        <Button variant="secondary" onClick={onCancel} disabled={form.busy}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

export function ProjectForm({
  value,
  organizations,
  onSaved,
  onCancel,
}: Actions & { value?: Projeto; organizations: Organizacao[] }) {
  const { api } = useAuth();
  const form = useForm("projeto", async (v) => {
    const input = {
      nome: v.nome.trim(),
      descricao: v.descricao.trim(),
      emailContato: v.emailContato.trim(),
      organizacaoId: v.organizacaoId,
      dataInicio: v.dataInicio,
      dataFim: v.dataFim || null,
      status: v.status as StatusProjeto,
    };
    if (value) await api.projects.update(value.id, input);
    else await api.projects.create(input);
    await onSaved(
      value
        ? "Projeto atualizado."
        : "Projeto publicado. Sua iniciativa já pode ser encontrada pela comunidade.",
    );
  });
  return (
    <form onSubmit={form.submit} noValidate className="form-stack">
      <h3>
        {value ? "Editar projeto" : "Dê o primeiro passo da sua iniciativa"}
      </h3>
      <Field name="nome" label="Nome do projeto" error={form.fields.nome}>
        <input
          {...form.attrs("nome")}
          defaultValue={value?.nome}
          maxLength={120}
          autoFocus
        />
      </Field>
      <Field
        name="descricao"
        label="Sobre a iniciativa"
        error={form.fields.descricao}
      >
        <textarea
          {...form.attrs("descricao")}
          defaultValue={value?.descricao}
          rows={4}
          maxLength={1000}
          placeholder="Qual é o objetivo? Para quem é o projeto? Como ele acontece?"
        />
      </Field>
      <div className="form-columns">
        <SelectField
          name="organizacaoId"
          label="Organização responsável"
          error={form.fields.organizacaoId}
          defaultValue={
            value?.organizacaoId ||
            (organizations.length === 1 ? organizations[0].id : "")
          }
          placeholder="Selecione uma organização"
          options={organizations.map((o) => ({ value: o.id, label: o.nome }))}
        />
        <Field
          name="emailContato"
          label="E-mail do projeto"
          error={form.fields.emailContato}
        >
          <input
            {...form.attrs("emailContato")}
            defaultValue={value?.emailContato}
            type="email"
          />
        </Field>
      </div>
      <div className="form-columns">
        <DateField
          name="dataInicio"
          label="Data de início"
          error={form.fields.dataInicio}
          defaultValue={value?.dataInicio || ""}
        />
        <DateField
          name="dataFim"
          label="Data final (opcional)"
          error={form.fields.dataFim}
          defaultValue={value?.dataFim || ""}
          optional
        />
      </div>
      <SelectField
        name="status"
        label="Situação do projeto"
        error={form.fields.status}
        defaultValue={value?.status || "PLANEJADO"}
        options={Object.entries(STATUS).map(([key, label]) => ({
          value: key,
          label,
        }))}
      />
      {form.error && <Feedback>{form.error}</Feedback>}
      <div className="form-actions">
        <Button type="submit" busy={form.busy}>
          Salvar projeto
        </Button>
        <Button variant="secondary" onClick={onCancel} disabled={form.busy}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

export function UpdateForm({
  project,
  onSaved,
  onCancel,
}: Actions & { project: Projeto }) {
  const { api } = useAuth();
  const form = useForm("atualizacao", async (v) => {
    await api.projects.publish(project.id, v.descricao.trim());
    await onSaved(
      "Atualização publicada. A comunidade já pode acompanhar esse novo passo.",
    );
  });
  return (
    <form onSubmit={form.submit} noValidate className="form-stack">
      <h3>O que há de novo?</h3>
      <p className="muted">
        Compartilhe um novo passo de <strong>{project.nome}</strong>.
      </p>
      <Field
        name="descricao"
        label="Atualização do projeto"
        error={form.fields.descricao}
        hint="A publicação será pública. Conte atividades e resultados sem expor dados pessoais dos participantes."
      >
        <textarea
          {...form.attrs("descricao")}
          rows={5}
          maxLength={1000}
          autoFocus
          placeholder="Conte o que aconteceu, o que foi realizado ou qual será o próximo passo."
        />
      </Field>
      {form.error && <Feedback>{form.error}</Feedback>}
      <div className="form-actions">
        <Button type="submit" busy={form.busy}>
          Publicar atualização
        </Button>
        <Button variant="secondary" onClick={onCancel} disabled={form.busy}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

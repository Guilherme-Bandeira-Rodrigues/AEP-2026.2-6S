"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  ArrowUpRight,
  Building2,
  FolderHeart,
  Pencil,
  Plus,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { errorMessage } from "@/lib/errors";
import type { Organizacao, Projeto } from "@/types/domain";
import { Button, EmptyState, Feedback, Loading, StatusBadge } from "./ui";
import { OrganizationForm, ProjectForm, UpdateForm } from "./management-forms";

type Panel =
  | { type: "organization"; value?: Organizacao }
  | { type: "project"; value?: Projeto }
  | { type: "update"; value: Projeto };
type Deletion = { type: "organization" | "project"; id: string; name: string };

export function ManagementPage() {
  const { status, user, error, refresh } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (status === "ready" && !user) router.replace("/login");
  }, [status, user, router]);
  if (status === "error")
    return (
      <div className="container page-space">
        <Feedback>{error}</Feedback>
        <Button onClick={refresh} className="mt-4">
          Verificar conexão novamente
        </Button>
      </div>
    );
  if (status === "loading" || !user)
    return (
      <div className="container page-space">
        <Loading label="Verificando sua sessão…" />
      </div>
    );
  return <Manager key={user.id} name={user.nome} />;
}

function Manager({ name }: { name: string }) {
  const { api, handleError } = useAuth();
  const [tab, setTab] = useState<"projects" | "organizations">("projects");
  const [data, setData] = useState<{
    organizations: Organizacao[];
    projects: Projeto[];
  }>({ organizations: [], projects: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [panel, setPanel] = useState<Panel | null>(null);
  const [deletion, setDeletion] = useState<Deletion | null>(null);
  const [deleting, setDeleting] = useState(false);
  const editorRef = useRef<HTMLDivElement>(null);
  const createRef = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (panel)
      editorRef.current?.scrollIntoView?.({
        block: "start",
        behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)")
          .matches
          ? "instant"
          : "smooth",
      });
  }, [panel]);
  const fetchData = useCallback(async () => {
    const [organizations, projects] = await Promise.all([
      api.organizations.mine(),
      api.projects.mine(),
    ]);
    return { organizations, projects };
  }, [api]);
  async function load() {
    try {
      const updated = await fetchData();
      setData(updated);
      setError("");
      return updated;
    } catch (e) {
      setError(errorMessage(e));
      handleError(e);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    let active = true;
    fetchData()
      .then((result) => {
        if (active) {
          setData(result);
          setError("");
        }
      })
      .catch((e) => {
        if (active) {
          setError(errorMessage(e));
          handleError(e);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [fetchData, handleError]);
  async function saved(message: string) {
    const firstOrganization =
      panel?.type === "organization" &&
      !panel.value &&
      !data.organizations.length;
    setPanel(null);
    setNotice(message);
    const updated = await load();
    if (firstOrganization && updated?.organizations.length) {
      setTab("projects");
      setPanel({ type: "project" });
    } else createRef.current?.focus({ preventScroll: true });
  }
  function closePanel() {
    setPanel(null);
    requestAnimationFrame(() =>
      createRef.current?.focus({ preventScroll: true }),
    );
  }
  async function remove() {
    if (!deletion || deleting) return;
    setDeleting(true);
    setError("");
    try {
      if (deletion.type === "organization")
        await api.organizations.remove(deletion.id);
      else await api.projects.remove(deletion.id);
      setNotice(
        deletion.type === "organization"
          ? "Organização excluída."
          : "Projeto excluído.",
      );
      setDeletion(null);
      await load();
    } catch (e) {
      setError(errorMessage(e));
      handleError(e);
    } finally {
      setDeleting(false);
    }
  }
  function switchTab(value: typeof tab) {
    setTab(value);
    setPanel(null);
    setDeletion(null);
    setNotice("");
  }
  return (
    <div className="container management-page">
      <div className="management-heading">
        <div>
          <span className="eyebrow">MINHA GESTÃO</span>
          <h1>Seu cuidado vira movimento.</h1>
          <p>
            Olá, {name.split(" ")[0]}. Este é o espaço das iniciativas que você
            faz acontecer.
          </p>
        </div>
        <span className="management-mark" aria-hidden="true">
          <SproutMark />
        </span>
      </div>
      <div role="tablist" aria-label="Gerenciar iniciativas" className="tabs">
        {(["projects", "organizations"] as const).map((value) => (
          <button
            key={value}
            id={`tab-${value}`}
            role="tab"
            disabled={deleting}
            aria-selected={tab === value}
            aria-controls={`panel-${value}`}
            tabIndex={tab === value ? 0 : -1}
            onClick={() => switchTab(value)}
            onKeyDown={(e) => {
              if (["ArrowLeft", "ArrowRight", "Home", "End"].includes(e.key)) {
                e.preventDefault();
                const next =
                  e.key === "Home"
                    ? "projects"
                    : e.key === "End"
                      ? "organizations"
                      : value === "projects"
                        ? "organizations"
                        : "projects";
                switchTab(next);
                document.getElementById(`tab-${next}`)?.focus();
              }
            }}
          >
            {value === "projects" ? (
              <FolderHeart size={19} />
            ) : (
              <Building2 size={19} />
            )}
            {value === "projects" ? "Meus projetos" : "Minhas organizações"}
            <span>
              {value === "projects"
                ? data.projects.length
                : data.organizations.length}
            </span>
          </button>
        ))}
      </div>
      <section
        role="tabpanel"
        id={`panel-${tab}`}
        aria-labelledby={`tab-${tab}`}
      >
        {notice && (
          <div className="mb-5">
            <Feedback kind="success">{notice}</Feedback>
          </div>
        )}
        {error && (
          <div className="mb-5">
            <Feedback>{error}</Feedback>
            <button className="text-link mt-3" onClick={() => void load()}>
              Atualizar informações
            </button>
          </div>
        )}
        {loading ? (
          <Loading label="Carregando suas iniciativas…" />
        ) : (
          <>
            <div className="management-toolbar">
              <div>
                <h2>
                  {tab === "projects"
                    ? "Cada iniciativa tem uma história"
                    : "Quem está por trás da mudança"}
                </h2>
                <p>
                  {tab === "projects"
                    ? "Mantenha as informações em dia e compartilhe novos passos."
                    : "Organize os responsáveis pelos seus projetos."}
                </p>
              </div>
              <Button
                ref={createRef}
                disabled={
                  Boolean(panel) ||
                  deleting ||
                  (tab === "projects" && !data.organizations.length)
                }
                onClick={() => {
                  setNotice("");
                  setPanel({
                    type: tab === "projects" ? "project" : "organization",
                  });
                }}
              >
                <Plus size={17} />
                {tab === "projects" ? "Novo projeto" : "Nova organização"}
              </Button>
            </div>
            {panel && (
              <div
                className="editor-panel"
                ref={editorRef}
                key={`${panel.type}-${panel.value?.id || "new"}`}
              >
                {panel.type === "organization" ? (
                  <OrganizationForm
                    key={panel.value?.id || "new-org"}
                    value={panel.value}
                    onSaved={saved}
                    onCancel={closePanel}
                  />
                ) : panel.type === "project" ? (
                  <ProjectForm
                    key={panel.value?.id || "new-project"}
                    value={panel.value}
                    organizations={data.organizations}
                    onSaved={saved}
                    onCancel={closePanel}
                  />
                ) : (
                  <UpdateForm
                    key={panel.value.id}
                    project={panel.value}
                    onSaved={saved}
                    onCancel={closePanel}
                  />
                )}
              </div>
            )}
            {deletion && (
              <div
                className="delete-confirm"
                role="group"
                aria-label="Confirmar exclusão"
              >
                <div>
                  <strong>Excluir “{deletion.name}”?</strong>
                  <p>
                    {deletion.type === "project"
                      ? "O projeto e seu histórico serão removidos. Esta ação não pode ser desfeita."
                      : "A organização só pode ser excluída se não tiver projetos vinculados."}
                  </p>
                </div>
                <div className="form-actions">
                  <Button
                    variant="danger"
                    busy={deleting}
                    onClick={remove}
                    autoFocus
                  >
                    Confirmar exclusão
                  </Button>
                  <Button
                    variant="secondary"
                    disabled={deleting}
                    onClick={() => setDeletion(null)}
                  >
                    Cancelar
                  </Button>
                </div>
              </div>
            )}
            {tab === "projects" ? (
              data.projects.length ? (
                <div className="management-list">
                  {data.projects.map((p) => (
                    <article key={p.id} className="management-row">
                      <div className="row-icon">
                        <FolderHeart size={23} strokeWidth={1.5} />
                      </div>
                      <div className="row-main">
                        <span className="organization-label">
                          {p.organizacao?.nome || "Organização não vinculada"}
                        </span>
                        <h3>{p.nome}</h3>
                        <StatusBadge status={p.status} />
                      </div>
                      <div className="row-actions">
                        <button
                          onClick={() => {
                            setPanel({ type: "update", value: p });
                            setNotice("");
                          }}
                          disabled={Boolean(panel)}
                          className="text-link"
                        >
                          Publicar atualização
                        </button>
                        <Link
                          href={`/projetos/${p.id}`}
                          aria-label={`Ver ${p.nome}`}
                          className="icon-button"
                        >
                          <ArrowUpRight size={18} />
                        </Link>
                        <button
                          onClick={() =>
                            setPanel({ type: "project", value: p })
                          }
                          disabled={Boolean(panel)}
                          className="icon-button"
                          aria-label={`Editar ${p.nome}`}
                        >
                          <Pencil size={17} />
                        </button>
                        <button
                          onClick={() =>
                            setDeletion({
                              type: "project",
                              id: p.id,
                              name: p.nome,
                            })
                          }
                          disabled={Boolean(panel) || deleting}
                          className="icon-button"
                          aria-label={`Excluir ${p.nome}`}
                        >
                          <Trash2 size={17} />
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title={
                    data.organizations.length
                      ? "Uma ideia pode ser o começo"
                      : "Vamos começar pela sua organização?"
                  }
                  action={
                    !data.organizations.length && (
                      <Button
                        variant="secondary"
                        onClick={() => {
                          switchTab("organizations");
                          setPanel({ type: "organization" });
                        }}
                      >
                        Cadastrar uma organização <ArrowUpRight size={17} />
                      </Button>
                    )
                  }
                >
                  {data.organizations.length
                    ? "Crie seu primeiro projeto e mostre à comunidade o que você está construindo."
                    : "Antes de publicar um projeto, cadastre a organização responsável por ele."}
                </EmptyState>
              )
            ) : data.organizations.length ? (
              <div className="management-list">
                {data.organizations.map((o) => (
                  <article key={o.id} className="management-row">
                    <div className="row-icon">
                      <Building2 size={23} strokeWidth={1.5} />
                    </div>
                    <div className="row-main">
                      <h3>{o.nome}</h3>
                      <p>{o.emailContato}</p>
                    </div>
                    <div className="row-actions">
                      <button
                        onClick={() =>
                          setPanel({ type: "organization", value: o })
                        }
                        disabled={Boolean(panel)}
                        className="text-link"
                      >
                        <Pencil size={16} />
                        Editar
                      </button>
                      <button
                        onClick={() =>
                          setDeletion({
                            type: "organization",
                            id: o.id,
                            name: o.nome,
                          })
                        }
                        disabled={Boolean(panel) || deleting}
                        className="icon-button"
                        aria-label={`Excluir ${o.nome}`}
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState title="Pessoas reunidas por um propósito">
                Cadastre sua organização para começar a compartilhar
                iniciativas.
              </EmptyState>
            )}
          </>
        )}
      </section>
    </div>
  );
}
function SproutMark() {
  return (
    <svg viewBox="0 0 80 80" fill="none">
      <path d="M40 68V32" stroke="currentColor" strokeWidth="3" />
      <path d="M40 44C16 44 8 24 8 12c24 0 32 12 32 32" fill="#32AEB1" />
      <path d="M40 34c24 0 32-14 32-26-24 0-32 12-32 26" fill="#E6BC38" />
    </svg>
  );
}

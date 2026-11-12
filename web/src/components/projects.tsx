"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  ArrowDown,
  ArrowLeft,
  ArrowUpRight,
  Building2,
  CalendarDays,
  Mail,
  Search,
  Sprout,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { dateLabel } from "@/lib/format";
import { ApiError, errorMessage } from "@/lib/errors";
import {
  STATUS,
  type Filtros,
  type Projeto,
  type StatusProjeto,
} from "@/types/domain";
import {
  Button,
  EmptyState,
  Feedback,
  LinkArrow,
  Loading,
  StatusBadge,
} from "./ui";
import { Mosaic } from "./mosaic";
import { SelectField } from "./form-controls";

export function ProjectCard({ project }: { project: Projeto }) {
  return (
    <article className="project-card">
      <div className="project-card-heading">
        <span className="organization-label">
          <Building2 size={15} aria-hidden="true" />
          {project.organizacao?.nome || "Organização não vinculada"}
        </span>
        <span className="card-flower" aria-hidden="true" />
      </div>
      <h3>
        <Link href={`/projetos/${project.id}`}>{project.nome}</Link>
      </h3>
      <p className="project-summary">
        {project.descricao || "Descrição não informada."}
      </p>
      <div className="project-meta">
        <StatusBadge status={project.status} />
        <span>
          <CalendarDays size={14} aria-hidden="true" />
          {dateLabel(project.dataInicio)}
        </span>
      </div>
      <Link href={`/projetos/${project.id}`} className="card-link">
        Conhecer projeto <LinkArrow />
      </Link>
    </article>
  );
}

export function ExplorePage() {
  const { api } = useAuth();
  const [filters, setFilters] = useState<Filtros>({});
  const [statusFilter, setStatusFilter] = useState("");
  const [result, setResult] = useState<{
    data: Projeto[];
    loading: boolean;
    error: string;
  }>({ data: [], loading: true, error: "" });
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const abort = new AbortController();
    api.projects
      .list(filters, abort.signal)
      .then((data) => {
        if (!abort.signal.aborted)
          setResult({ data, loading: false, error: "" });
      })
      .catch((e) => {
        if (!abort.signal.aborted)
          setResult({ data: [], loading: false, error: errorMessage(e) });
      });
    return () => abort.abort();
  }, [api, filters, retry]);
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setResult((previous) => ({ ...previous, loading: true, error: "" }));
    setFilters({
      nome: String(data.get("nome") || ""),
      status: String(data.get("status") || "") as StatusProjeto | "",
    });
  }
  return (
    <>
      <section className="container hero">
        <div className="hero-copy">
          <span className="eyebrow">
            <Sprout size={17} aria-hidden="true" /> FEITO POR PESSOAS, PARA
            PESSOAS
          </span>
          <h1>
            Toda mudança
            <br />
            começa <span>perto.</span>
          </h1>
          <p>
            Conheça quem faz a diferença na comunidade.
            <br className="hidden sm:block" /> Encontre projetos, acompanhe
            histórias e veja as iniciativas acontecerem.
          </p>
          <a href="#projetos" className="button button-primary">
            Explore as iniciativas <ArrowDown size={17} aria-hidden="true" />
          </a>
          <div className="hero-footnote">
            <span className="mini-tiles" aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
            <span>Mais informação. Mais comunidade.</span>
          </div>
        </div>
        <div className="hero-art">
          <Mosaic />
          <div className="art-caption">
            <span>
              Um infinito campo
              <br />
              de possibilidades.
            </span>
            <ArrowUpRight size={28} strokeWidth={1.2} aria-hidden="true" />
          </div>
        </div>
      </section>
      <section className="projects-section" id="projetos">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="eyebrow">CONEXÕES QUE TRANSFORMAM</span>
              <h2>Projetos da comunidade</h2>
            </div>
            <p>
              Encontre uma iniciativa.
              <br />
              Conheça a história por trás dela.
            </p>
          </div>
          <form
            onSubmit={search}
            className="search-form"
            onReset={() => setStatusFilter("")}
          >
            <div className="search-input">
              <label htmlFor="busca">Pesquisar pelo nome</label>
              <div>
                <Search size={19} aria-hidden="true" />
                <input
                  id="busca"
                  name="nome"
                  placeholder="Qual projeto você procura?"
                  type="search"
                />
              </div>
            </div>
            <div className="search-status">
              <SelectField
                name="status"
                label="Situação do projeto"
                value={statusFilter}
                onChange={setStatusFilter}
                options={[
                  { value: "", label: "Todos os status" },
                  ...Object.entries(STATUS).map(([value, label]) => ({
                    value,
                    label,
                  })),
                ]}
              />
            </div>
            <Button type="submit" busy={result.loading}>
              Buscar <LinkArrow />
            </Button>
            {(filters.nome || filters.status) && (
              <button
                type="reset"
                className="text-link clear-filters"
                onClick={() => {
                  setResult((previous) => ({
                    ...previous,
                    loading: true,
                    error: "",
                  }));
                  setFilters({});
                }}
              >
                Limpar filtros
              </button>
            )}
          </form>
          {result.loading && !result.data.length ? (
            <Loading label="Buscando iniciativas…" />
          ) : result.error ? (
            <div className="py-8">
              <Feedback>{result.error}</Feedback>
              <Button
                variant="secondary"
                className="mt-4"
                onClick={() => {
                  setResult({ data: [], loading: true, error: "" });
                  setRetry((n) => n + 1);
                }}
              >
                Tentar novamente
              </Button>
            </div>
          ) : result.data.length ? (
            <>
              <p className="result-count" role="status">
                {result.loading ? (
                  "Atualizando iniciativas…"
                ) : (
                  <>
                    {result.data.length}{" "}
                    {result.data.length === 1
                      ? "iniciativa encontrada"
                      : "iniciativas encontradas"}
                  </>
                )}
              </p>
              <div className="project-grid" aria-busy={result.loading}>
                {result.data.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            </>
          ) : (
            <EmptyState
              title={
                filters.nome || filters.status
                  ? "Nenhum projeto com esses filtros"
                  : "O próximo começo pode ser o seu"
              }
            >
              {filters.nome || filters.status
                ? "Experimente outro nome ou limpe os filtros para explorar as iniciativas."
                : "Ainda não há projetos publicados. Cadastre uma organização e compartilhe sua iniciativa."}
            </EmptyState>
          )}
        </div>
      </section>
      <section className="container invitation">
        <div className="invitation-mark" aria-hidden="true">
          <Sprout size={38} strokeWidth={1.3} />
        </div>
        <div>
          <h2>Sua iniciativa também cabe aqui.</h2>
          <p>Dê visibilidade ao seu projeto e compartilhe cada novo passo.</p>
        </div>
        <Link href="/gestao" className="button button-secondary">
          Compartilhar uma iniciativa{" "}
          <ArrowUpRight size={18} aria-hidden="true" />
        </Link>
      </section>
    </>
  );
}

export function ProjectDetail({ id }: { id: string }) {
  const { api } = useAuth();
  const [state, setState] = useState<{
    project: Projeto | null;
    error: string;
    missing: boolean;
    loading: boolean;
  }>({ project: null, error: "", missing: false, loading: true });
  const load = useCallback(
    (signal?: AbortSignal) =>
      api.projects
        .get(id, signal)
        .then((project) => {
          if (!signal?.aborted)
            setState({ project, error: "", missing: false, loading: false });
        })
        .catch((e) => {
          if (!signal?.aborted)
            setState({
              project: null,
              error: errorMessage(e),
              missing: e instanceof ApiError && e.status === 404,
              loading: false,
            });
        }),
    [api, id],
  );
  useEffect(() => {
    const abort = new AbortController();
    void load(abort.signal);
    return () => abort.abort();
  }, [load]);
  if (state.loading)
    return (
      <div className="container page-space">
        <Loading label="Carregando projeto…" />
      </div>
    );
  if (!state.project)
    return (
      <div className="container page-space">
        <Link href="/" className="back-link">
          <ArrowLeft size={16} />
          Explorar projetos
        </Link>
        <EmptyState
          title={
            state.missing
              ? "Projeto não encontrado"
              : "Não foi possível abrir este projeto"
          }
          action={
            !state.missing && (
              <Button
                onClick={() => {
                  setState({ ...state, loading: true });
                  void load();
                }}
              >
                Tentar novamente
              </Button>
            )
          }
        >
          {state.missing
            ? "O projeto pode ter sido removido. Conheça outras iniciativas da comunidade."
            : state.error}
        </EmptyState>
      </div>
    );
  const p = state.project;
  const updates = [...(p.atualizacoes || [])].sort((a, b) =>
    b.data.localeCompare(a.data),
  );
  return (
    <div className="container detail-page">
      <Link href="/" className="back-link">
        <ArrowLeft size={16} aria-hidden="true" />
        Todos os projetos
      </Link>
      <div className="detail-heading">
        <div>
          <StatusBadge status={p.status} />
          <h1>{p.nome}</h1>
          <p className="organization-label">
            <Building2 size={17} />
            {p.organizacao?.nome || "Organização não vinculada"}
          </p>
        </div>
        <span className="detail-flower" aria-hidden="true" />
      </div>
      <div className="detail-grid">
        <div>
          <section className="detail-section">
            <h2>Sobre a iniciativa</h2>
            <p className="preserve-text">
              {p.descricao || "Descrição não informada."}
            </p>
          </section>
          <section className="detail-section">
            <div className="timeline-title">
              <h2>Cada passo dessa história</h2>
              <span>Atualizações do projeto</span>
            </div>
            {updates.length ? (
              <ol className="timeline">
                {updates.map((item, index) => (
                  <li key={`${item.data}-${index}`}>
                    <time dateTime={item.data}>{dateLabel(item.data)}</time>
                    <p className="preserve-text">{item.descricao}</p>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="quiet-box">
                Este projeto ainda não publicou atualizações.
              </p>
            )}
          </section>
        </div>
        <aside className="project-aside">
          <div className="info-block">
            <span className="eyebrow">QUEM FAZ ACONTECER</span>
            <h2>{p.organizacao?.nome || "Organização não vinculada"}</h2>
            <p>
              {p.organizacao?.descricao ||
                "As informações da organização ainda não estão disponíveis."}
            </p>
          </div>
          <dl className="dates">
            <div>
              <dt>
                <CalendarDays size={16} />
                Início
              </dt>
              <dd>{dateLabel(p.dataInicio)}</dd>
            </div>
            <div>
              <dt>Encerramento</dt>
              <dd>{p.dataFim ? dateLabel(p.dataFim) : "Sem data definida"}</dd>
            </div>
          </dl>
          {p.emailContato && (
            <div className="contact-block">
              <h3>Vamos conversar?</h3>
              <p>Entre em contato com o projeto.</p>
              <a href={`mailto:${p.emailContato}`}>
                <Mail size={17} />
                {p.emailContato}
              </a>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}

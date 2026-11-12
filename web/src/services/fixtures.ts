import type { Organizacao, Projeto, Usuario } from "@/types/domain";

export interface DemoData {
  version: 1;
  users: Usuario[];
  organizations: (Organizacao & { proprietarioId: string })[];
  projects: Projeto[];
  sessionId: string | null;
}
export function demoFixtures(): DemoData {
  const organizations = [
    {
      id: "000000000000000000000001",
      nome: "Instituto Raízes",
      descricao: "Encontros, aprendizado e cuidado com a nossa comunidade.",
      emailContato: "raizes@example.org",
      proprietarioId: "demo",
    },
    {
      id: "000000000000000000000002",
      nome: "Coletivo Horizonte",
      descricao:
        "Iniciativas culturais feitas por pessoas do bairro, para o bairro.",
      emailContato: "horizonte@example.org",
      proprietarioId: "demo",
    },
    {
      id: "000000000000000000000003",
      nome: "Associação Sementes",
      descricao: "Espaços de convivência e educação ambiental.",
      emailContato: "sementes@example.org",
      proprietarioId: "outra-conta",
    },
  ];
  const projects: Projeto[] = [
    {
      id: "100000000000000000000001",
      nome: "Aprender em comunidade",
      descricao:
        "Um espaço de troca e aprendizado para crianças e jovens. O projeto reúne a comunidade em oficinas de leitura e atividades educativas, com encontros semanais abertos às famílias.",
      organizacaoId: organizations[0].id,
      organizacao: organizations[0],
      emailContato: "aprender@example.org",
      dataInicio: "2026-09-10",
      dataFim: null,
      status: "EM_ANDAMENTO",
      criadoEm: "2026-09-01T12:00:00Z",
      atualizadoEm: "2026-11-03T14:00:00Z",
      atualizacoes: [
        {
          data: "2026-11-03T14:00:00Z",
          descricao:
            "A primeira oficina de leitura aconteceu no espaço comunitário. O próximo encontro será dedicado à troca de livros entre as famílias.",
        },
      ],
    },
    {
      id: "100000000000000000000002",
      nome: "Cultura de portas abertas",
      descricao:
        "Oficinas de arte e encontros culturais para aproximar moradores e valorizar a produção do bairro. Estamos preparando a programação junto à comunidade.",
      organizacaoId: organizations[1].id,
      organizacao: organizations[1],
      emailContato: "cultura@example.org",
      dataInicio: "2026-12-01",
      dataFim: "2027-03-30",
      status: "PLANEJADO",
      criadoEm: "2026-10-20T12:00:00Z",
      atualizadoEm: "2026-10-20T12:00:00Z",
      atualizacoes: [],
    },
    {
      id: "100000000000000000000003",
      nome: "Uma horta, muitos encontros",
      descricao:
        "A criação de uma horta coletiva transformou um espaço de convivência. A iniciativa reuniu moradores para aprender sobre cultivo e compartilhar os cuidados com o lugar.",
      organizacaoId: organizations[2].id,
      organizacao: organizations[2],
      emailContato: "horta@example.org",
      dataInicio: "2026-06-01",
      dataFim: "2026-10-10",
      status: "CONCLUIDO",
      criadoEm: "2026-05-20T12:00:00Z",
      atualizadoEm: "2026-10-10T12:00:00Z",
      atualizacoes: [
        {
          data: "2026-10-10T12:00:00Z",
          descricao:
            "Encerramos as oficinas de cultivo. Os cuidados com a horta seguem organizados pelos moradores.",
        },
      ],
    },
  ];
  return {
    version: 1,
    users: [
      { id: "demo", nome: "Ana da Comunidade", email: "ana@example.org" },
    ],
    organizations,
    projects,
    sessionId: null,
  };
}

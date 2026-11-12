# Projetos Sociais — Frontend

Interface da PoC em **Next.js (App Router), React, TypeScript e Tailwind CSS**. A consulta pública mostra iniciativas, organizações responsáveis e histórico de atualizações. Após criar uma conta e entrar, o responsável gerencia suas organizações e projetos. A proposta apoia o ODS 16 pelo acesso à informação e transparência.

## Executar com a API

Para executar tudo com Docker, use `docker compose up --build -d --wait` na raiz do repositório. O [README principal](../README.md) explica portas, persistência e configuração. Nesse modo, não é necessário instalar Node.js ou Java localmente.

Requisitos: Node.js 22.12 ou superior, npm e o backend em `../api` com MongoDB. As dependências estão fixadas no `package-lock.json`.

Na raiz do repositório, abra dois terminais:

```powershell
# Terminal 1: API (Java 21 e Docker)
cd api
docker compose up -d mongodb
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2: frontend
cd web
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Acesse **http://localhost:3000**. No Linux/macOS, use `./mvnw` e `cp .env.example .env.local`.

Configuração em `.env.local`:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_DATA_MODE=api
```

O modo padrão é `api`. Cadastre uma conta, faça login, crie uma organização e depois um projeto. Uma base vazia apresenta orientações de primeiro uso. Dados reais não são substituídos por exemplos quando a API está indisponível.

Use `localhost` nos dois serviços. O backend permite `http://localhost:3000` por padrão; ao mudar a origem, configure também `FRONTEND_ORIGINS` na API. Variáveis `NEXT_PUBLIC_*` são públicas e incorporadas ao build: nunca coloque segredos nelas. Reinicie o servidor após alterá-las.

## Demonstração sem backend

Para visualizar as telas com dados fictícios, altere `NEXT_PUBLIC_DATA_MODE=demo` e reinicie o frontend. Um aviso permanente identifica a simulação. Use `ana@example.org` e uma senha fictícia de pelo menos 8 caracteres, ou cadastre uma conta fictícia.

O adaptador demonstrativo persiste dados em `localStorage`, mas **não guarda nem verifica senhas e não oferece autenticação real**. O botão “Restaurar exemplos” reinicia os dados após confirmação. Este modo serve para apresentação visual e desenvolvimento; a evidência de persistência NoSQL e segurança deve ser apresentada no modo `api`.

## Telas e fluxo

| Rota             | Funcionalidade                                                                            |
| ---------------- | ----------------------------------------------------------------------------------------- |
| `/`              | Busca por nome, filtro de situação e cartões de projetos                                  |
| `/projetos/[id]` | Descrição, organização, contato, datas e histórico do mais recente para o mais antigo     |
| `/cadastro`      | Cadastro com confirmação de senha e validação dos campos                                  |
| `/login`         | Login e direcionamento à gestão                                                           |
| `/gestao`        | Organizações e projetos da conta; cadastro, edição, exclusão e publicação de atualizações |

Fluxo sugerido para demonstração: criar conta → entrar → cadastrar organização → criar projeto → publicar atualização → abrir página pública → sair → consultar o projeto sem login. A exclusão pede confirmação; a API impede excluir organizações que ainda tenham projetos.

Não há inscrição de voluntários, doações, recuperação de senha ou gestão administrativa nesta PoC. O contato com o responsável é feito pelo e-mail público do projeto.

## Organização do código

```text
src/
  app/          Rotas, layout, metadados e estilos globais
  components/   Telas e componentes reutilizáveis
  contexts/     Sessão autenticada e injeção dos serviços
  hooks/        Estado, validação e submissão dos formulários
  lib/          Validação, datas e erros tipados
  services/     Contrato, adaptadores HTTP/demo e exemplos fictícios
  types/        Tipos do domínio alinhados aos DTOs da API
  test/         Configuração dos testes
```

As telas consomem o contrato `Services`; chamadas HTTP e armazenamento demonstrativo ficam isolados. O contexto de autenticação recebe o serviço, o que permite testar os fluxos sem servidor. Formulários validam antes de enviar, preservam os valores após falhas, bloqueiam submissões duplicadas e exibem erros de campo retornados pela API. A validação definitiva e a autorização pertencem ao backend.

## Autenticação e integração

- Requisições HTTP usam `credentials: include` e `cache: no-store`.
- A API mantém a sessão em cookie HttpOnly. O frontend não armazena senha, JWT ou cookie de sessão em `localStorage`.
- Antes das gravações, o cliente obtém `/auth/csrf` e envia o token no header informado. Após login, logout, `401` ou `403`, invalida o token em memória. Operações de escrita não são repetidas automaticamente.
- Cadastro não autentica automaticamente: a pessoa é encaminhada ao login.
- `/auth/me` restaura a sessão; um `401` durante a gestão encerra o estado autenticado e direciona ao login.
- Os endpoints `/me/organizacoes` e `/me/projetos` alimentam a área de gestão. Consultas públicas continuam disponíveis sem conta.
- Respostas `application/problem+json` exibem `detail` e o mapa `erros`. Os conflitos `409`, registros ausentes e indisponibilidade recebem mensagens próprias.
- O histórico usa `{ data, descricao }`. A publicação retorna o projeto completo; o cliente não presume um ID para as atualizações.

Contrato completo: [autenticação e frontend](../api/docs/autenticacao-frontend.md). Em produção, mantenha frontend/API no mesmo site, HTTPS, CORS restrito e `SESSION_COOKIE_SECURE=true` na API. Não habilite o modo demonstrativo como autenticação de produção.

## Design e acessibilidade

Paleta inspirada na referência: azul petróleo, turquesa, amarelo, laranja e vermelho, sobre fundo claro. Tipografia local Source Sans 3 e Barlow Condensed, ilustrações geométricas em SVG, cantos discretos e animações CSS curtas. Fontes vêm do pacote instalado, sem requisições ao Google Fonts.

Layouts adaptados a celular e desktop, labels nos formulários, foco visível, navegação das abas por teclado, link para pular ao conteúdo, estados de carregamento/erro/vazio e respeito a `prefers-reduced-motion`.

Calendários e selects usam React Aria Components com estilos próprios. As datas são apresentadas em português e enviadas em ISO; a data final pode ser removida pelo calendário. Os controles aceitam navegação por teclado e restauram o foco ao fechar. Bordas e foco usam tons neutros, preservando a sinalização dos erros de validação.

Na gestão, o primeiro cadastro de organização abre diretamente a criação do projeto, com a organização já selecionada quando existe apenas uma. Os formulários entram em foco ao abrir e retornam ao botão de criação ao cancelar. A busca mantém os cards visíveis enquanto consulta novos resultados.

## Testes e cobertura

```powershell
npm run typecheck
npm run lint
npm run format:check
npm run test:coverage
npm run build
```

Vitest e Testing Library verificam comportamento de consulta, filtros, recuperação de falhas, cadastro, login, logout, CRUD, publicação, conflitos, sessão expirada e preservação dos formulários. Os testes do cliente HTTP verificam cookies, CSRF, rotação, erros e ausência de repetição automática de gravações. O adaptador demonstrativo também tem testes de persistência, propriedade dos registros e preservação do histórico.

O comando de cobertura falha abaixo de **70% de linhas**. Abrange os arquivos TypeScript/TSX de `src`, incluindo telas, serviços, validação e rotas; exclui apenas testes, configuração de testes, tipos do domínio e fixtures estáticas. Relatórios: `coverage/index.html` e `coverage/lcov.info`. Esta medição corresponde ao frontend; a API mantém sua própria suíte JaCoCo.

Os testes automatizados do frontend usam serviços injetados e transporte HTTP simulado. Para validar integração real, execute o fluxo sugerido acima com API e MongoDB ativos.

Na verificação inicial, `npm audit` apontou 7 alertas moderados transitivos na cadeia de desenvolvimento `typescript-eslint`/`minimatch`. `npm audit fix` não os resolveu com as versões compatíveis atuais. Reavalie-os ao atualizar as ferramentas; não foi aplicada substituição forçada de versão principal. O ESLint 9 foi mantido pela compatibilidade declarada pelos plugins do Next.

```powershell
# Build e execução de produção
npm run build
npm start

# Desenvolvimento
npm run test:watch
npm run format
```

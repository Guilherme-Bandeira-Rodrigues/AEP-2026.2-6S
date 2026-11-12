# API de Projetos Sociais

API REST em Java 21 com Spring Boot, Spring Security, Spring Data MongoDB e Swagger. A API permite consulta pública de iniciativas comunitárias e gestão de organizações, projetos e históricos pelo responsável autenticado. A proposta se relaciona ao ODS 16 pelo acesso à informação e transparência.

## Execução local

Para subir frontend, API e MongoDB juntos, execute `docker compose up --build -d --wait` na raiz do repositório. Veja as [instruções do projeto completo](../README.md). Os comandos abaixo são para executar a API diretamente no computador.

```powershell
docker compose up -d mongodb
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, use `./mvnw spring-boot:run`. A aplicação lê `MONGODB_URI` e, por padrão, usa as credenciais locais do Compose no banco `aep`. A interface do Swagger fica em [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## API

Consultas são públicas; gravações exigem sessão, token CSRF e propriedade do registro. Comece por `GET /api/auth/csrf`, cadastre a conta e faça login. Veja os contratos e exemplos em [Autenticação e integração do frontend](docs/autenticacao-frontend.md).

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Obtém token CSRF e sessão anônima |
| POST | `/api/auth/cadastro` | Cria conta com nome, e-mail e senha; retorna 201 |
| POST | `/api/auth/login` | Autentica, renova a sessão e retorna o usuário |
| GET | `/api/auth/me` | Consulta o usuário autenticado |
| POST | `/api/auth/logout` | Encerra a sessão; retorna 204 |
| GET | `/api/me/organizacoes` | Lista somente organizações da conta |
| GET | `/api/me/projetos` | Lista somente projetos da conta |

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | `/api/organizacoes` | Cria organização e retorna 201 |
| GET | `/api/organizacoes` | Lista organizações |
| GET | `/api/organizacoes/{id}` | Consulta organização |
| PUT | `/api/organizacoes/{id}` | Atualiza organização |
| DELETE | `/api/organizacoes/{id}` | Exclui organização com 204 ou retorna 409 se houver projetos vinculados |
| POST | `/api/projetos` | Cria projeto com organização existente e retorna 201 |
| GET | `/api/projetos` | Lista projetos com organização e histórico |
| GET | `/api/projetos/{id}` | Consulta projeto com organização e histórico |
| PUT | `/api/projetos/{id}` | Atualiza dados editáveis e preserva o histórico |
| POST | `/api/projetos/{id}/atualizacoes` | Publica atualização e retorna 201 com o projeto e o histórico completo |
| DELETE | `/api/projetos/{id}` | Exclui projeto com 204 |

Cadastre uma organização com `nome`, `descricao` e `emailContato`. Use o `id` retornado no campo `organizacaoId` ao cadastrar ou editar um projeto. O projeto tem seu próprio `emailContato`, além de `nome`, `descricao`, `dataInicio`, `status` e `dataFim` opcional.

Publique uma atualização com um corpo como `{"descricao":"A primeira turma iniciou as atividades."}`. A API registra a data e a hora em UTC e acrescenta a entrada ao final da lista. Para combinar filtros, use `GET /api/projetos?nome=educa&status=EM_ANDAMENTO`. A busca por nome usa um trecho literal sem distinção entre maiúsculas e minúsculas. Os status aceitos são `PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO` e `CANCELADO`.

A API responde com `400` para dados inválidos, `401` para autenticação ausente/inválida, `403` para CSRF inválido ou operação em recurso de outra conta, `404` para recursos inexistentes e `409` para e-mail duplicado ou organização em uso. As respostas de erro usam `application/problem+json`. Erros de validação de campos incluem o mapa `erros`.

`FRONTEND_ORIGINS` configura as origens CORS (padrão: `http://localhost:3000`, valores separados por vírgula). O frontend deve enviar credenciais e o header CSRF em gravações. Em HTTPS, configure `SESSION_COOKIE_SECURE=true`. As sessões expiram após 30 minutos sem atividade.

## Persistência e migração

A coleção `organizacoes` usa um `ObjectId` em `_id`. Em `projetos_sociais`, `organizacaoId` armazena a representação textual desse identificador. O campo `atualizacoes` contém subdocumentos com `data` (BSON Date) e `descricao`. A publicação acrescenta uma entrada atomicamente, enquanto a edição altera apenas os campos do cadastro. Na implantação com uma instância da API, a validação e a gravação do vínculo são sincronizadas com a exclusão da organização.

Consulte o [procedimento de migração](docs/migracao-organizacoes.md) se o banco já contiver projetos com `organizacaoResponsavel`.

A coleção `usuarios` armazena o hash BCrypt da senha e possui índice único de e-mail normalizado. Organizações têm `proprietarioId` definido pela sessão, nunca pelo cliente. Projetos herdam a autorização da organização vinculada. Os dados antigos sem proprietário precisam de associação administrativa explícita antes de serem editados. Timestamps de criação, edição e publicação são retornados em UTC; os BSON Dates existentes são preservados.

## Testes

Execute `./mvnw verify` ou `.\mvnw.cmd verify`. O JaCoCo gera o relatório em `target/site/jacoco/index.html`.

Use `clean verify` para medir uma compilação limpa. O build exige pelo menos **80% de linhas e 80% de branches**, acima do mínimo de 70% da entrega. A suíte padrão cobre validação, autenticação, sessão, CSRF, CORS e autorização. Sem `AEP_MONGODB_TEST_URI`, os testes de integração são explicitamente ignorados.

Para incluir os testes de integração, configure `AEP_MONGODB_TEST_URI` com a URI de um MongoDB de desenvolvimento. Por exemplo, no PowerShell com o Compose local em execução:

```powershell
$env:AEP_MONGODB_TEST_URI = 'mongodb://admin:admin@localhost:27017/?authSource=admin'
.\mvnw.cmd verify
```

A suíte de integração cria e remove um banco temporário de nome aleatório e exige que o usuário do MongoDB tenha permissão para essas operações.

A integração testa cadastro/login com MongoDB, índice único sob concorrência, isolamento entre duas contas, bloqueio de apropriação de projetos, CSRF real, rotação de sessão, logout e preservação de histórico.

## Evolução

A primeira versão (`0025812`) oferecia CRUD de projetos com o nome da organização no próprio documento. A segunda acrescenta organizações relacionadas, histórico de atualizações, filtros, documentação de migração e gestão autenticada por proprietário. A implementação mantém controller → service → repository/MongoTemplate, com DTOs de entrada e saída separados das entidades persistidas.

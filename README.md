# API de Projetos Sociais

API REST em Java 21 com Spring Boot, Spring Data MongoDB e Swagger. A API cadastra organizações responsáveis e projetos sociais e registra o histórico de cada projeto.

## Execução local

```powershell
docker compose up -d mongodb
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, use `./mvnw spring-boot:run`. A aplicação lê `MONGODB_URI` e, por padrão, usa as credenciais locais do Compose no banco `aep`. A interface do Swagger fica em [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## API

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

A API responde com `400` para dados inválidos, `404` para recursos ou organizações vinculadas inexistentes e `409` para exclusão de organização em uso. As respostas de erro usam `application/problem+json`. Erros de validação de campos incluem o mapa `erros`.

## Persistência e migração

A coleção `organizacoes` usa um `ObjectId` em `_id`. Em `projetos_sociais`, `organizacaoId` armazena a representação textual desse identificador. O campo `atualizacoes` contém subdocumentos com `data` (BSON Date) e `descricao`. A publicação acrescenta uma entrada atomicamente, enquanto a edição altera apenas os campos do cadastro. Na implantação com uma instância da API, a validação e a gravação do vínculo são sincronizadas com a exclusão da organização.

Consulte o [procedimento de migração](docs/migracao-organizacoes.md) se o banco já contiver projetos com `organizacaoResponsavel`.

## Testes

Execute `./mvnw verify` ou `.\mvnw.cmd verify`. O JaCoCo gera o relatório em `target/site/jacoco/index.html`.

Para incluir os testes de integração, configure `AEP_MONGODB_TEST_URI` com a URI de um MongoDB de desenvolvimento. Por exemplo, no PowerShell com o Compose local em execução:

```powershell
$env:AEP_MONGODB_TEST_URI = 'mongodb://admin:admin@localhost:27017/?authSource=admin'
.\mvnw.cmd verify
```

A suíte de integração cria e remove um banco temporário de nome aleatório e exige que o usuário do MongoDB tenha permissão para essas operações.

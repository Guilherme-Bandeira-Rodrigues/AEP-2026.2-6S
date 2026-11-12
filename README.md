# Projetos Sociais

PoC de consulta pública e gestão de iniciativas sociais: frontend em Next.js/React/TypeScript/Tailwind, API Java 21 com Spring Boot/Spring Security e banco MongoDB. A proposta apoia o ODS 16 pelo acesso à informação e transparência.

## Subir o projeto completo

Instale e inicie o **Docker Desktop com containers Linux** e Docker Compose. Não é necessário instalar Java, Node.js ou MongoDB no computador.

Na raiz do repositório:

```powershell
docker compose up --build -d --wait --wait-timeout 180
```

O primeiro build baixa as imagens e dependências e pode levar alguns minutos. O Dockerfile da API executa `mvnw verify`, incluindo testes unitários e a verificação JaCoCo; os testes que exigem MongoDB externo permanecem condicionados à configuração descrita na documentação da API. O frontend é compilado em modo de produção, integrado à API real.

| Serviço | Endereço |
| --- | --- |
| Frontend | http://localhost:3000 |
| API | http://localhost:8080/api/projetos |
| Swagger | http://localhost:8080/swagger-ui.html |
| MongoDB | `mongodb:27017`, somente na rede dos containers |

Use **localhost** no navegador, pois a origem CORS e a URL pública da API estão configuradas para esse endereço. Cadastre uma conta, faça login, crie uma organização e então um projeto. As atualizações publicadas aparecem na consulta pública.

Os serviços aguardam os respectivos healthchecks: MongoDB → API → frontend. API e frontend usam builds em estágios, com execução sem usuário root. O Next utiliza o artefato standalone apenas no Docker, mantendo `npm run dev` e `npm start` disponíveis no desenvolvimento local.

## Configuração e dados

Os valores padrão permitem execução local imediata. Para personalizar portas ou credenciais, copie `.env.example` para `.env` **na raiz**, antes de iniciar:

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose up --build -d --wait --wait-timeout 180
```

`WEB_PORT` e `API_PORT` atualizam também CORS e a URL da API incorporada ao frontend. Alterar a porta da API exige reconstruir o frontend com `--build`, pois variáveis `NEXT_PUBLIC_*` são incorporadas durante a compilação. O `.env.local` de `web` não é copiado para a imagem.

As credenciais padrão são exclusivas desta configuração local. Use caracteres alfanuméricos, `_` e `-` nas credenciais configuradas aqui, pois são interpoladas na URI. A inicialização das credenciais do MongoDB ocorre apenas na primeira criação do volume; editar `.env` não altera usuários de um banco já inicializado.

O volume `mongodb_data` preserva contas, organizações e projetos entre reinícios. `docker compose down` remove os containers e mantém os dados. Evite `down -v` se quiser preservá-los: essa opção remove também o volume do banco.

Esta configuração expõe frontend/API apenas em `127.0.0.1` e não publica a porta do banco. Ela é destinada à execução local e avaliação. Para hospedagem, configure domínio, HTTPS, origens CORS, cookie seguro e credenciais próprias.

O arquivo `api/docker-compose.yml` continua disponível para o desenvolvimento que usa apenas MongoDB/mongo-express. Ele é independente do Compose completo da raiz e usa outro volume; dados anteriores não são migrados automaticamente.

## Comandos úteis

```powershell
docker compose ps
docker compose logs -f api web
docker compose stop
docker compose start
docker compose down
```

Para reconstruir após mudanças no código, use novamente `docker compose up --build -d --wait`.

## Documentação e testes

- [API: execução, endpoints, modelagem e testes](api/README.md)
- [Frontend: estrutura, funcionalidades e testes de cobertura](web/README.md)
- [Contrato de autenticação e integração](api/docs/autenticacao-frontend.md)

O build Docker da API inclui sua suíte padrão. Para gerar o relatório do frontend localmente, execute `npm ci` e `npm run test:coverage` dentro de `web`. Os relatórios são independentes; consulte cada README para os comandos reproduzíveis e testes de integração com MongoDB.

Referências de implementação: [Next.js standalone](https://nextjs.org/docs/app/api-reference/config/next-config-js/output) e [ordem de inicialização no Compose](https://docs.docker.com/compose/how-tos/startup-order/).

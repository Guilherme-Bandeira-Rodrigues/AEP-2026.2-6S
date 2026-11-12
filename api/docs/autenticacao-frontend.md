# Autenticação e integração do frontend

O backend usa Spring Security, sessão HTTP e CSRF. Não há JWT nem senha/token de autenticação no `localStorage`. A sessão fica em memória na instância da API; reiniciar a API exige novo login. Esta PoC pressupõe uma instância, sem sessão distribuída.

## Endpoints

| Método | Rota | Corpo / retorno |
| --- | --- | --- |
| GET | `/api/auth/csrf` | `200 {"headerName":"X-CSRF-TOKEN","token":"..."}`; estabelece a sessão anônima |
| POST | `/api/auth/cadastro` | `{nome,email,senha}` → `201 {id,nome,email}`; não autentica automaticamente |
| POST | `/api/auth/login` | `{email,senha}` → `200 {id,nome,email}` e sessão autenticada |
| GET | `/api/auth/me` | `200 {id,nome,email}` ou `401` |
| POST | `/api/auth/logout` | `204`, invalida a sessão e remove o cookie |
| GET | `/api/me/organizacoes` | Array das organizações da conta autenticada |
| GET | `/api/me/projetos` | Array dos projetos das organizações da conta autenticada |

Cadastro: nome de 3 a 100 caracteres após remover espaços externos, e-mail válido de até 254 caracteres, senha de 8 a 72 caracteres e no máximo 72 bytes UTF-8 (limite do BCrypt). E-mails são normalizados para minúsculas e sem espaços externos antes de validar/persistir; senhas nunca são aparadas. A confirmação de senha pertence ao formulário, não ao contrato da API. Cadastro com e-mail existente retorna `409`.

`usuarios` contém `_id`, `nome`, `email` e `senhaHash`. O hash usa o formato delegável do Spring Security, atualmente BCrypt. O índice único `usuario_email_unico` é assegurado antes de cada cadastro, protegendo inserções concorrentes. Senha e hash nunca são retornados pelos DTOs públicos.

## Sequência de chamadas

1. Faça `GET /api/auth/csrf` com `credentials: "include"`. Mantenha o token retornado em memória.
2. Envie esse token no header indicado por `headerName` em **todo POST, PUT ou DELETE**, inclusive cadastro, login e logout. Use `credentials: "include"` em todas as chamadas.
3. Após cadastro, apresente o login. Após login, chame **novamente** `GET /api/auth/csrf`: o identificador da sessão é renovado e o token anterior é invalidado.
4. Ao recarregar a página, consulte `/api/auth/me` para reconstruir o estado de autenticação e `/api/auth/csrf` para obter o token.
5. Após logout, descarte o estado local. Antes de novo cadastro/login, obtenha outro token CSRF.

Exemplo com chamadas diretas à API:

```ts
const base = "http://localhost:8080/api";
let csrf: { headerName: string; token: string };

async function renovarCsrf() {
  const resposta = await fetch(`${base}/auth/csrf`, { credentials: "include" });
  if (!resposta.ok) throw new Error("Não foi possível iniciar a sessão.");
  csrf = await resposta.json();
}

async function entrar(email: string, senha: string) {
  await renovarCsrf();
  const resposta = await fetch(`${base}/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ email, senha }),
  });
  if (!resposta.ok) throw await resposta.json();
  const usuario = await resposta.json();
  await renovarCsrf();
  return usuario;
}
```

O cookie `JSESSIONID` é HttpOnly, SameSite=Lax e expira com a sessão do navegador; a sessão no servidor expira após 30 minutos sem atividade. Configure `SESSION_COOKIE_SECURE=true` na implantação com HTTPS. CORS permite por padrão apenas `http://localhost:3000`; configure origens exatas separadas por vírgula em `FRONTEND_ORIGINS`. Não use `*` com credenciais. Use `localhost` de ambos os lados no desenvolvimento, sem misturar com `127.0.0.1`.

Para produção, prefira frontend e API na mesma origem por reverse proxy, ou subdomínios do mesmo site em HTTPS. Apenas configurar CORS não torna cookies SameSite=Lax compatíveis com sites distintos. Em proxy Next, preserve os cookies de requisição e os headers Set-Cookie da API; não implemente outra sessão independente no frontend.

## Permissões

- Listagem e detalhes de projetos/organizações são públicos.
- Criação de organização usa o usuário da sessão como `proprietarioId`. O corpo não aceita esse campo.
- Edição/exclusão de organização exige propriedade. Uma organização com projetos não pode ser excluída (`409`).
- Criação de projeto exige organização da conta.
- Edição de projeto exige propriedade tanto da organização atual quanto da nova organização, impedindo apropriação de projetos por troca de vínculo.
- Exclusão e publicação de atualização exigem propriedade da organização vinculada.
- Não há transferência de propriedade, contas administrativas ou cadastro de equipes nesta versão.
- Registros antigos sem proprietário continuam públicos, mas não podem ser alterados por contas comuns. Veja [migração](migracao-organizacoes.md).

## Erros

As respostas de erro de autenticação, autorização, CSRF e validação usam `application/problem+json`.

- `400`: corpo/campo inválido. Quando a validação é por campo, há `erros: Record<string, string[]>`.
- `401`: sessão ausente/inválida ou credenciais incorretas.
- `403`: token CSRF ausente/inválido ou recurso de outra conta. Uma escrita anônima sem token pode retornar `403` antes da verificação de autenticação.
- `404`: recurso inexistente.
- `409`: e-mail duplicado ou organização vinculada a projetos.

Após `401`, solicite login novamente. Em `403`, verifique a sessão e obtenha novo token se necessário, sem reenviar escritas automaticamente. O logout é idempotente: com token válido, retorna `204` mesmo sem usuário autenticado.

## Contratos de projetos

`POST /api/projetos/{id}/atualizacoes` recebe somente `{descricao}` e retorna `201` com **o projeto completo**. Cada atualização tem `{data, descricao}`, sem ID. Descrição obrigatória, até 1.000 caracteres. A API mantém ordem de inclusão; a interface pode ordenar uma cópia por data decrescente.

`criadoEm`, `atualizadoEm` e `atualizacoes[].data` são timestamps ISO em UTC. `dataInicio`/`dataFim` são datas `YYYY-MM-DD`, sem fuso; `dataFim` pode ser nula. Projetos legados podem ter `organizacaoId`/`organizacao` nulos e histórico vazio. Não presuma que todo registro legado está completo.

## Swagger

Leia `/api/auth/csrf`, copie o token no esquema `csrf` do botão Authorize e execute cadastro/login. O navegador recebe o cookie de sessão automaticamente. Após login, obtenha e configure um novo token. O esquema `sessao` documenta o cookie, que é HttpOnly e não deve ser preenchido manualmente pelo JavaScript. O logout é documentado apesar de ser atendido diretamente pelo filtro de segurança.

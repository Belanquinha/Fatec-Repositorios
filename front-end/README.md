# Fatec Projetos Integradores

Este Projeto foi feito usando Angular versão 21.2.0.

## Desenvolvimento Local

Para começar a desenvolver Localmente, rode:

```bash
ng serve
```

E quando tiver rodando, entre na URL: `http://localhost:4200/`. 



## Configuração

### Pré-requisito: app registration single-tenant

O app **só funciona** com uma conta do tenant da CPS. O app registration no Entra ID precisa estar configurado como **Single tenant**, apontando para o tenant `eabe64c5-68f5-4a76-8301-9577a679e449` (AD-3), e ter a **Redirect URI** registrada exatamente como o app é servido, com barra final:

| Ambiente | Redirect URI a registrar |
| --- | --- |
| `ng serve` | `http://localhost:4200/` |
| Docker / produção | o domínio real, com barra final (ex.: `https://repositorio.fatec.sp.gov.br/`) |

O front-end deriva essa URI de `document.baseURI` (o `<base href>` do `index.html`,
o mesmo valor que o roteador do Angular usa). Não há nada a configurar: o que é
enviado ao Entra ID é sempre a página real em que a pessoa está.

Duas divergências quebram o login e nenhuma delas aparece como erro de build:

| Sintoma | Causa |
| --- | --- |
| `AADSTS50011` | redirect URI registrada diferente da que o app é servido |
| conta de fora da CPS não entra | authority `common` em vez do GUID do tenant |

### Nenhuma configuração é necessária

O front-end **não lê variáveis de ambiente**. Os valores vivem como constantes em:

| Arquivo | Usado por | `apiUrl` |
| --- | --- | --- |
| `src/environments/environment.ts` | `ng build` e a imagem Docker | `/api` (relativa; o nginx decide o destino) |
| `src/environments/environment.development.ts` | `ng serve` | `http://localhost:4040` (direto ao Spring Boot, via CORS) |

`clientId` e `authority` definem um app registration único, single-tenant da
CPS, então são constantes da aplicação e não do ambiente. O `clientId` é um
identificador público — todo SPA web o expõe no bundle — e nunca deve ser
substituído por um "client secret".

Isso não é conveniência: passar a URL da API por variável de ambiente permitia que
um bundle de produção nascesse apontando para `localhost`, que funciona na máquina
de quem desenvolveu e quebra em qualquer outro host, sem erro algum. Todo mundo
agora compila exatamente o mesmo bundle.

## Configuração do back-end via Docker

O back-end **sim** lê do ambiente, e é configurado no `environment:` do compose —
em runtime, não em build, então uma imagem serve qualquer ambiente.

Dois segredos são **obrigatórios** e não têm default. O compose usa `${VAR:?...}`,
que aborta na hora com uma mensagem clara se a variável faltar:

| Variável | Onde é usada |
| --- | --- |
| `POSTGRES_PASSWORD` | senha do PostgreSQL (mesma para `postgres` e `backend`) |
| `JWT_SECRET` | assinatura dos tokens JWT (mínimo de 32 caracteres) |

Antes ambos tinham default, e o do JWT era uma chave **versionada no repositório**.
Um deploy que esquecesse de setar a variável passaria a assinar token com chave
pública — qualquer pessoa poderia forjar uma sessão de admin. Agora a API não sobe
sem a variável.

Para subir o stack, a partir da **raiz do repositório** (é lá que o compose lê):

```bash
cp .env.example .env
# edite .env e gere segredos de verdade:
openssl rand -base64 24   # POSTGRES_PASSWORD
openssl rand -base64 48   # JWT_SECRET
docker compose up
```

Existe **um único** `.env.example`, na raiz. As variáveis opcionais (`MSAL_TENANT_ID`,
`CORS_ALLOWED_ORIGINS`, `DB_URL`…) estão documentadas lá dentro, comentadas, para não
existir um segundo arquivo com parte da verdade. O `.env` está no `.gitignore`.

O resto tem default e é opcional:

| Variável | Onde é usada | Padrão |
| --- | --- | --- |
| `MSAL_TENANT_ID` | `app.security.msal.tenant-id` | `eabe64c5-68f5-4a76-8301-9577a679e449` |
| `DB_URL` | banco, quando o back-end roda fora do compose | `jdbc:postgresql://localhost:5432/fatecrepository` |
| `CORS_ALLOWED_ORIGINS` | origens liberadas, separadas por vírgula | `http://localhost:4200` e afins |

O back-end recusa com **401** qualquer token cujo `tid` não seja o `MSAL_TENANT_ID`,
antes de chamar o Microsoft Graph. Trocar o tenant exige mudar o app registration
junto — os dois lados precisam concordar.

> Em produção o CORS quase não é acionado: o nginx serve o front e faz proxy de
> `/api` para o back-end, então as duas ficam na mesma origem. Ele importa mesmo é
> no `ng serve` (`:4200` → `:4040`). Por isso a lista é configurável — trocar a
> porta do front-development não deve exigir recompilar o Java.

### Rodar sem Docker

Nada impede. O front-end não tem `.env`, e o back-end só precisa de `POSTGRES_PASSWORD`
e `JWT_SECRET` na ambiente:

```bash
# front
cd front-end && npm install && ng serve        # http://localhost:4200

# back
cd back-end
POSTGRES_PASSWORD=... JWT_SECRET=... ./mvnw spring-boot:run   # http://localhost:4040
```

Atenção à porta do banco: o `application.yml` assume `5432`, mas o compose expõe o
Postgres do host na **5433**. Apontando para o banco do compose, use
`DB_URL=jdbc:postgresql://localhost:5433/fatecrepository`.

### Papéis de teste

A tela de login oferece **um** caminho: o botão do Microsoft. Não existe atalho por papel, e não
existe flag de ambiente que ligue um.

Houve um atalho "Entrar sem Microsoft" (aluno / professor / admin com um clique), removido junto com
o `POST /auth/dev-login` que o servia. O motivo está em `back-end/README.md`: o back-end emitia
sessão válida para qualquer e-mail, e a única proteção era um par de interruptores de ambiente —
inclusive com os dois **ligados** no `docker-compose.yml` versionado.

Para os testes de papel, use a suíte (`npm test` no front; `./mvnw test` no back-end), que monta
sessões de ALUNO, PROFESSOR e ADMIN sem nenhuma rota aberta.


### Sessão e validade do token

- O MSAL guarda o cache em `localStorage`, e não no `sessionStorage` padrão. Com `sessionStorage` o
  cache morre com a aba e cada nova aba vira um login completo — MFA a cada troca de aba. O custo é
  token legível por script na página.
- O token de sessão do back-end tem validade (`JWT_EXPIRATION_HOURS`, 24h por padrão) e o
  `localStorage` não expira nada. `sessao-navegador.ts` lê o `exp` antes de declarar sessão viva, e
  descarta as chaves quando o token venceu. Sem isso, a tela anunciaria "logado" e toda chamada
  autenticada voltaria 401.
- O interceptor descarta a sessão em 401, mas não em 403: 403 é "autenticado, mas não autorizado",
  que um professor tentando aprovar projeto de outra instituição vai encontrar, e expulsar a pessoa
  por isso seria errado.

## Comandos Uteis de desenvovimento

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

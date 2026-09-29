# Fatec Projetos Integradores

Este Projeto foi feito usando Angular versão 21.2.0.

## Desenvolvimento Local

Para começar a desenvolver Localmente, rode:

```bash
ng serve
```

E quando tiver rodando, entre na URL: `http://localhost:4200/`. 



## Configuração — variáveis de ambiente

O build usa o `@ngx-env/builder`, que lê o arquivo **`.env` na raiz do front-end** (mesma pasta do `package.json`) e injeta as variáveis `NG_APP_*` em `import.meta.env` no momento do build.

### Pré-requisito: app registration single-tenant

O app **só funciona** com uma conta do tenant da CPS. O app registration no Entra ID precisa:

- estar configurado como **Single tenant**, apontando para o tenant `eabe64c5-68f5-4a76-8301-9577a679e449` (AD-3);
- ter a **Redirect URI** `http://localhost:4200/` registrada **exatamente assim, com a barra final**.

Duas divergências quebram o login e nenhuma delas aparece como erro de build:

| Sintoma | Causa |
| --- | --- |
| `AADSTS50011` | redirect URI sem a barra final (ou com barra a mais) |
| conta de fora da CPS não entra | authority `common` em vez do GUID do tenant |

1. Copie o molde e preencha:

   ```bash
   cp .env.example .env
   ```

2. No `.env`, informe o Application (client) ID do seu app registration no Microsoft Entra ID:

   ```dotenv
   NG_APP_MSAL_CLIENT_ID=seu-client-id-aqui
   ```

3. Instale as dependências e rode:

   ```bash
   npm install
   ng serve
   ```

> ⚠️ O valor vai no **`.env`**, **não** em `src/environments/environment.ts`. Os arquivos `environment*.ts` apenas leem `import.meta.env['NG_APP_*']` e, quando a variável vem **vazia ou em branco**, caem no padrão embutido: o tenant `eabe64c5-68f5-4a76-8301-9577a679e449` e o redirect `http://localhost:4200/`. Esse padrão é o valor canônico do projeto — ele existe para que nenhum build saia multi-tenant, não para ser editado. `clientId`/`redirectUri`/`apiUrl` **não são segredos** (SPA auth code + PKCE); nunca coloque um "client secret" no front-end.

## Configuração via Docker / variáveis do ambiente

O `docker-compose.yml` deriva a configuração dos mesmos valores, então existe **uma única fonte de verdade**. Qualquer uma delas pode ser sobrescrita no ambiente:

| Variável | Onde é usada | Padrão |
| --- | --- | --- |
| `MSAL_TENANT_ID` | authority do front-end **e** `app.security.msal.tenant-id` do back-end | `eabe64c5-68f5-4a76-8301-9577a679e449` |
| `MSAL_REDIRECT_URI` | redirect URI do front-end | `http://localhost:4200/` |

O back-end recusa com **401** qualquer token cujo `tid` não seja o `MSAL_TENANT_ID`, antes de chamar o Microsoft Graph. Trocar o tenant exige mudar o app registration junto — os dois lados leem a mesma variável.



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

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

> ⚠️ O valor vai no **`.env`**, **não** em `src/environments/environment.ts`. Os arquivos `environment*.ts` apenas leem `import.meta.env['NG_APP_*']` com fallback de desenvolvimento. `clientId`/`redirectUri`/`apiUrl` **não são segredos** (SPA auth code + PKCE); nunca coloque um "client secret" no front-end. 



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

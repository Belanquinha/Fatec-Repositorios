# Fatec-Repositorios (Back-end)

API Spring Boot da plataforma Fatec-Repositorios. Autenticação via MSAL (tenant CPS), papel derivado do domínio do e-mail no back-end.

## Banco de dados

```sql
CREATE DATABASE fatecrepository
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Portuguese_Brazil.1252'
    LC_CTYPE = 'Portuguese_Brazil.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;
```

O seed do admin está em `init/01-create-admin.sql` e `data.sql`: o admin é identificado pelo **e-mail institucional** (via MSAL, sem senha). **Substitua o e-mail pelo de um e-mail real `@cps.sp.gov.br` do dono do projeto.**

## Regras de domínio (papel)

A classificação mora em `UserProvisioningService` — uma implementação só, usada tanto pelo login
Microsoft quanto pelo login de desenvolvimento. Duas cópias divergentes fariam o mesmo endereço ser
`ALUNO` num caminho e `PROFESSOR` no outro, e isso só apareceria em produção.

- `@aluno.cps.sp.gov.br` → `ALUNO` (checado primeiro)
- `@cps.sp.gov.br` → `PROFESSOR`
- demais membros do tenant → `ALUNO` (fallback)
- admin: papel `ADMIN` persiste no banco quando o e-mail é semeado em `data.sql`

## Login de desenvolvimento (somente local)

Para testar aluno, professor e admin sem conta Microsoft e sem MFA, existe `POST /auth/dev-login`: ele
recebe um e-mail e devolve o mesmo JWT do login real, sem passar por verificação de assinatura nem
pelo Graph.

**A porta que isso abre:** qualquer um que alcance o endpoint recebe uma sessão válida para o e-mail
que informar, inclusive a conta de admin. São dois interruptores, ambos desligados por padrão:

```bash
SPRING_PROFILES_ACTIVE=dev    # perfil Spring
DEV_AUTH_ENABLED=true         # app.security.dev-auth.enabled
```

Com os dois, `GET /auth/dev-login/contas` devolve os atalhos e `POST /auth/dev-login` emite o token.
Com um só, as rotas respondem 404 — o bean do controller nem é criado. Ligando os dois, a aplicação
imprime um banner de aviso na subida.

O atalho de admin só leva ao painel administrativo se o e-mail do atalho
(`admin@cps.sp.gov.br` em `DevAuthService`) bater com o e-mail do `init/01-create-admin.sql`. Se você
trocou o e-mail do admin no seed, troque também no atalho — divergindo, a conta é criada como
`PROFESSOR` pela regra de domínio e o botão não faz o que promete.

> O `docker-compose.yml` não repassa `SPRING_PROFILES_ACTIVE` nem `DEV_AUTH_ENABLED` ao container.
> Com elas apenas no `.env`, o compose ignora. Para usar no stack containerizado, descomente as duas
> linhas no `environment:` do serviço `backend` — e comente de volta ao terminar. **O front-end
> precisa ser o `ng serve`**, porque o `Dockerfile` dele faz build de produção, onde os atalhos não
> existem; ver `front-end/README.md`.

### O que este recurso não garante

O token emitido aqui é um JWT normal, com o papel real no banco. Mas o back-end ainda não impõe
nada a partir desse papel: a única checagem de papel por endpoint é `hasRole("ADMIN")` nas rotas de
escrita de `/instituicoes` (`SecurityConfig.java`). Aprovação de projeto por professor continua sem
imposição pela API — é uma pendência de epic 1, story 1-4. Ou seja: use os atalhos para testar **o que
a interface mostra** para cada papel, e não para testar que a API restringe um papel.

### Removendo

Apague `DevAuthController`, `DevAuthService`, `DevLoginRequest`, `DevAuthAccountResponse` e
`DevAuthStartupWarning`; remova o bloco `dev-auth:` do `application.yml` e as duas linhas do
`docker-compose.yml`. Nada mais no back-end depende disso — `UserProvisioningService` é usado
também pelo login Microsoft e deve ficar.
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

## Testar os três papéis (ALUNO / PROFESSOR / ADMIN)

Não existe rota de login sem Microsoft. `POST /auth/login-microsoft` é o único caminho de entrada:
ele valida o access token do Graph contra o JWKS do tenant da CPS, deriva o papel do domínio do
e-mail e emite o JWT da sessão.

Houve um `POST /auth/dev-login` que emitia sessão válida para qualquer e-mail digitado, inclusive a
conta de admin. Ele foi **removido de propósito**, e o motivo não é o endpoint em si: a proteção
dependia inteiramente de configuração de ambiente (`SPRING_PROFILES_ACTIVE=dev` mais
`DEV_AUTH_ENABLED=true`), e nenhuma linha de código impedia que alguém ligasse os dois e publicasse
o resultado. Um bypass de autenticação cuja segurança é um valor de `.env` não é uma proteção.

Para exercitar os três papéis, use a suíte E2E, que emite tokens reais pelo `JwtTokenProvider` do
próprio projeto, sem nenhuma rota aberta:

```bash
# o banco de teste precisa existir uma vez
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" fatec-postgres \
  psql -U postgres -c "CREATE DATABASE fatecrepository_test"

TEST_DB_PASSWORD="$POSTGRES_PASSWORD" ./mvnw test
```

`AutorizacaoPorPapelE2ETest` cobre a matriz papel × método, `AutenticacaoSessaoE2ETest` cobre a
sessão, e `PoliticaDeAcessoE2ETest` garante que uma rota nova, esquecida em `SecurityConfig`,
exija sessão em vez de nascer pública.

## Sessão local sem Microsoft (contas de teste semeadas)

O banco de desenvolvimento sempre contém duas contas de teste (semeadas em `data.sql` e
`init/01-create-admin.sql`, como o admin e o catálogo):

| Conta | E-mail | Papel |
| :--- | :--- | :--- |
| Aluno Teste | `aluno.teste@aluno.cps.sp.gov.br` | `ALUNO` |
| Professor Teste | `professor.teste@cps.sp.gov.br` | `PROFESSOR` |

O projeto de exemplo (`seeds/projetos.json`) pertence ao Aluno Teste, e o Professor Teste é o
orientador responsável por ele — assim o aluno já tem 1 projeto em "Meus Projetos".

Para navegar como uma dessas contas sem passar pelo MSAL (útil para testes), gere uma sessão local:

``` 
# stack no ar, na raiz do repositório
docker compose up -d --build
python3 scripts/gerar-sessao.py aluno      # ou: professor
```

O script minta um JWT com as mesmas claims do `JwtTokenProvider` (assinado com o `JWT_SECRET`
do `.env`, válido por `JWT_EXPIRATION_HOURS`) e imprime um snippet para colar no console do
navegador (DevTools). A autenticação do back-end resolve a identidade pelo e-mail do token e
carrega papel e id do banco, então a sessão equivale a um login real — sem rota nova, sem
variável de ambiente e sem flag de build. Só existem os alvos `aluno` e `professor`: o script
não emite token de admin nem de e-mail arbitrário.

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

- `@aluno.cps.sp.gov.br` → `ALUNO` (checado primeiro)
- `@cps.sp.gov.br` → `PROFESSOR`
- demais membros do tenant → `ALUNO` (fallback)
- admin: papel `ADMIN` persiste no banco quando o e-mail é semeado em `data.sql`
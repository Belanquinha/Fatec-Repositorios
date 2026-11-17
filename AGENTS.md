# AGENTS.md — Contexto do Projeto Fatec-Repositorios

> Regra de ouro: todo agente (humano ou IA) deve ler este arquivo antes de começar
> qualquer tarefa no repositório. Evita re-explique o projeto a cada nova sessão.

---

## O que é este projeto

**Fatec-Repositorios** — um sistema web para repositório de conteúdos/acadêmicos,
composto por:

- **Backend** (`back-end/`): API Spring Boot 4.1 (Java 21), JPA + PostgreSQL 17,
  Spring Security com JWT (Azure AD/MSAL), Lombok, Swagger/OpenAPI (Springdoc).
- **Frontend** (`front-end/`): aplicação Angular 22 + TypeScript 6 + Tailwind CSS,
  Editor.js (rich text), Azure MSAL para login. Servida pelo Nginx em Docker.
- **Docker** (`docker-compose.yml`): `postgres:17`, `backend:4040`, `frontend:4200`.

## Stack resumida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security + JWT |
| Banco | PostgreSQL 17 (via Docker, porta 5433 local) |
| Frontend | Angular 22, TypeScript ~6.0, Tailwind CSS 4, Editor.js |
| Auth | Azure AD (MSAL), tenant ID `eabe64c5-68f5-4a76-8301-9577a679e449` |
| Infra | Docker Compose, Nginx (frontend), Maven (backend) |

## Comandos essenciais

```bash
# Subir a stack completa
docker compose up -d --build

# Parar
docker compose down

# Gerar sessão de teste (Bearer token) para dev
python3 scripts/gerar-sessao.py aluno     # ou: professor

# Frontend local (sem Docker)
cd front-end && npm run start

# Backend local (IDE ou mvn)
cd back-end && ./mvnw spring-boot:run
```

## Estrutura do repositório

```
Fatec-Repositorios/
├── back-end/          # Spring Boot + PostgreSQL client + JWT
│   ├── src/main/java  # (pacotes: controller, service, repository, config, security)
│   ├── src/main/resources/  # application.yml, schema.sql, seeds/
│   └── init/          # SQL de seed (admin) + geração de instituições
├── front-end/         # Angular 22 + Editor.js + Azure MSAL
│   ├── src/app/       # Componentes, services, guards, interceptors
│   └── environments/  # Configuração de ambiente
├── docker-compose.yml # Serviços: postgres, backend, frontend
└── scripts/           # Ferramentas de teste e deprecated
```

## Padrões e convenções

- **Autenticação**: JWT assinado com `JWT_SECRET` (ver `.env`, nunca commitar).
  Roles: `ALUNO`, `PROFESSOR`, `ADMIN`.
- **Upload de arquivos**: Files armazenados em `/app/uploads` (volume Docker).
  EM CASO DE FALHA DO UPLOAD: retornar erro real ao usuário, **não** gravar
  `data:URL Base64` no banco (ver issue registrada em `README.md`).
- **Env vars**: `.env.example` é a fonte da verdade. Copiar para `.env` antes
  de rodar `docker compose up`.
- **Git**: submeter apenas o `.env.example` (nunca `.env`). Ignorar
  `.agents/`, `*.log`, `node_modules/`, `dist/`, `target/`, `.idea/`.

## Como começar uma nova tarefa

1. Ler este arquivo (já feito).
2. Verificar `AGENTS.md` e `SPEC.md` se a tarefa envolve mudanças de requisito.
3. Propor tirar dúvidas sobre o escopo com o desenvolvedor (ou registrar na Issue).
4. Implementar em pequenos commits, testando local com `docker compose up -d --build`.
5. Reverter mudanças com `docker compose down` quando necessário.

## Arquivos de referência (gerados por metodologias)

- `SPEC.md` — específica técnica (se gerada por `bmad-spec`).
- `_bmad-output/` — artefatos temporários de planejamento (não commitar).

## Alinhamento

Atualize este arquivo sempre que:
- A stack mudar (novo framework, nova base de dados).
- Novos comandos ou scripts se tornarem essenciais.
- A arquitetura de pastas for refatorada.
